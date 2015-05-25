package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class TransactionFactoryTest {

	//region size / isSupported

	@Test
	public void allExpectedTransactionTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionFactory.size(), IsEqual.equalTo(5));
	}

	@Test
	public void isSupportedReturnsTrueForSupportedTypes() {
		// Arrange:
		final List<Integer> expectedRegisteredTypes = Arrays.asList(
				TransactionTypes.TRANSFER,
				TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				TransactionTypes.MULTISIG,
				TransactionTypes.MULTISIG_SIGNATURE);

		// Act:
		for (final Integer type : expectedRegisteredTypes) {
			// Act:
			final boolean isSupported = TransactionFactory.isSupported(type);

			// Assert:
			Assert.assertThat(isSupported, IsEqual.equalTo(true));
		}

		Assert.assertThat(expectedRegisteredTypes.size(), IsEqual.equalTo(TransactionFactory.size()));
	}

	@Test
	public void isSupportedReturnsFalseForUnsupportedTypes() {
		// Assert:
		Assert.assertThat(TransactionFactory.isSupported(9999), IsEqual.equalTo(false));
		Assert.assertThat(TransactionFactory.isSupported(TransactionTypes.TRANSFER | 0x1000), IsEqual.equalTo(false));
	}

	//endregion

	//region Unknown Transaction Type

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownTransaction() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		TransactionFactory.VERIFIABLE.deserialize(deserializer);
	}

	//endregion

	//region TransferTransaction

	@Test
	public void canDeserializeVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, TransferTransaction.class, TransactionTypes.TRANSFER);
	}

	private static Transaction createTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new TransferTransaction(TimeInstant.ZERO, sender, recipient, new Amount(100), null);
	}

	//endregion

	//region ImportanceTransferTransaction

	@Test
	public void canDeserializeVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	@Test
	public void canDeserializeNonVerifiableImportanceTransferTransaction() {
		// Arrange:
		final Transaction originalTransaction = createImportanceTransferTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, ImportanceTransferTransaction.class, TransactionTypes.IMPORTANCE_TRANSFER);
	}

	private static Transaction createImportanceTransferTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				sender,
				ImportanceTransferMode.Activate,
				recipient);
	}

	//endregion

	//region MultisigAggregateModificationTransaction

	@Test
	public void canDeserializeVerifiableMultisigAggregateModificationTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigAggregateModificationTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, MultisigAggregateModificationTransaction.class, TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);
	}

	@Test
	public void canDeserializeNonVerifiableMultisigAggregateModificationTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigAggregateModificationTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(
				originalTransaction,
				MultisigAggregateModificationTransaction.class,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);
	}

	private static Transaction createMultisigAggregateModificationTransaction() {
		final Account sender = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final List<MultisigModification> modifications = Collections.singletonList(new MultisigModification(MultisigModificationType.Add, cosignatory));
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				sender,
				modifications);
	}

	//endregion

	//region MultisigTransaction

	@Test
	public void canDeserializeVerifiableMultisigTransaction() {
		// Arrange:
		final Transaction otherTransaction = createTransferTransaction();
		final Transaction originalTransaction = createMultisigTransaction(otherTransaction);

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, MultisigTransaction.class, TransactionTypes.MULTISIG);
	}

	@Test
	public void canDeserializeNonVerifiableMultisigTransaction() {
		// Arrange:
		final Transaction otherTransaction = createTransferTransaction();
		final Transaction originalTransaction = createMultisigTransaction(otherTransaction);

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, MultisigTransaction.class, TransactionTypes.MULTISIG);
	}

	private static Transaction createMultisigTransaction(final Transaction transaction) {
		final Account sender = Utils.generateRandomAccount();
		return new MultisigTransaction(
				TimeInstant.ZERO,
				sender,
				transaction);
	}

	//endregion

	//region MultisigSignatureTransaction

	@Test
	public void canDeserializeVerifiableMultisigSignatureTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigSignatureTransaction();

		// Assert:
		assertCanDeserializeVerifiable(originalTransaction, MultisigSignatureTransaction.class, TransactionTypes.MULTISIG_SIGNATURE);
	}

	@Test
	public void canDeserializeNonVerifiableMultisigSignatureTransaction() {
		// Arrange:
		final Transaction originalTransaction = createMultisigSignatureTransaction();

		// Assert:
		assertCanDeserializeNonVerifiable(originalTransaction, MultisigSignatureTransaction.class, TransactionTypes.MULTISIG_SIGNATURE);
	}

	private static Transaction createMultisigSignatureTransaction() {
		return new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Hash.ZERO);
	}

	//endregion

	private static void assertCanDeserializeVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());
		final Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.notNullValue());
	}

	private static void assertCanDeserializeNonVerifiable(
			final Transaction originalTransaction,
			final Class expectedClass,
			final int expectedType) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		final Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

		// Assert:
		Assert.assertThat(transaction, IsInstanceOf.instanceOf(expectedClass));
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(expectedType));
		Assert.assertThat(transaction.getSignature(), IsNull.nullValue());
	}
}