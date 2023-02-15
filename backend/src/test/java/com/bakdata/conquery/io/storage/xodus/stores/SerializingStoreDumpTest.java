package com.bakdata.conquery.io.storage.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import io.dropwizard.jersey.validation.Validators;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class SerializingStoreDumpTest {

	public static final StoreInfo<UserId, User> USER_STORE_ID = StoreMappings.AUTH_USER.storeInfo();
	private static final MetaStorage STORAGE = new NonPersistentStoreFactory().createMetaStorage();
	private File tmpDir;
	private Environment env;
	private XodusStoreFactory config;
	private ObjectMapper objectMapper;

	// Test data
	private final ManagedQuery managedQuery = new ManagedQuery(null, null, new Dataset("dataset"));
	private final ConceptQuery cQuery = new ConceptQuery(
		new CQReusedQuery(managedQuery.getId()));
	private final User user = new User("username", "userlabel", STORAGE);

	@BeforeEach
	public void init() {
		tmpDir = Files.createTempDir();
		config = new XodusStoreFactory();
		env = Environments.newInstance(tmpDir, config.getXodus().createConfig());
		objectMapper = Jackson.BINARY_MAPPER.copy();
	}

	@AfterEach
	public void destroy() throws IOException {
		env.close();
		FileUtils.deleteDirectory(tmpDir);
	}

	private <KEY, VALUE> SerializingStore<KEY, VALUE> createSerializedStore(XodusStoreFactory config, Environment environment, Validator validator, StoreInfo<KEY,VALUE> storeId) {
		return new SerializingStore<>(
				new XodusStore(environment, storeId.getName(), (e) -> {
				}, (e) -> {
				}),
				validator,
				objectMapper,
				storeId.getKeyType(),
				storeId.getValueType(),
				config.isValidateOnWrite(),
				config.isRemoveUnreadableFromStore(),
				config.getUnreadableDataDumpDirectory()
		);
	}

	/**
	 * Tests if entries with corrupted values are dumped.
	 */
	@Test
	public void testCorruptValueDump() throws IOException {
		// Set dump directory to this tests temp-dir
		config.setUnreadableDataDumpDirectory(tmpDir);
		final StoreInfo<UserId, User> info = USER_STORE_ID;
		final UserId invalidId = new UserId("testU2");


		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), info);
			store.add(user.getId(), user);
		}

		{
			// Open that store again, with a different config to insert a corrupt entry
			// (UserId & ManagedQuery)
			SerializingStore<UserId, QueryDescription> store = createSerializedStore(
				config,
				env,
				Validators.newValidator(),
				new StoreInfo<>(info.getName(), UserId.class, QueryDescription.class));
			store.add(invalidId, cQuery);
		}

		{
			// Reopen the store with the initial value and try to iterate over all entries
			// (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), info);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(1);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}

		final File[] dumpFiles = SerializingStore.makeDumpFileName(invalidId.toString(), tmpDir, info.getName()).getParentFile()
												 .listFiles(file -> file.getName().endsWith(SerializingStore.DUMP_FILE_EXTENSION));

		final File[] exceptionFiles = SerializingStore.makeDumpFileName(invalidId.toString(), tmpDir, info.getName()).getParentFile()
													  .listFiles(file -> file.getName().endsWith(SerializingStore.EXCEPTION_FILE_EXTENSION));

		assertThat(dumpFiles)
				.isNotNull()
				.hasSize(1);

		assertThat(exceptionFiles)
				.isNotNull()
				.hasSize(1);


		assertThat((QueryDescription) Jackson.MAPPER.readerFor(QueryDescription.class).readValue(dumpFiles[0])).isEqualTo(cQuery);
	}

	/**
	 * Tests if entries with corrupted keys are dumped.
	 */
	@Test
	public void testCorruptKeyDump() throws IOException {
		// Set dump directory to this tests temp-dir
		config.setUnreadableDataDumpDirectory(tmpDir);
		final String invalidId = "not a valid conquery Id";
		final StoreInfo<UserId, User> info = USER_STORE_ID;

		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), info);
			store.add(new UserId("testU1"), user);
		}

		{
			// Open that store again, with a different config to insert a corrupt entry
			// (String & ManagedQuery)
			SerializingStore<String, QueryDescription> store = createSerializedStore(
				config,
				env,
				Validators.newValidator(),
				new StoreInfo<>(info.getName(), String.class, QueryDescription.class));
			store.add(invalidId, cQuery);
		}

		{
			// Reopen the store with the initial value and try to iterate over all entries
			// (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), info);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(1);
			expectedResult.setFailedValues(0);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}

		// Test if the correct number of dumpfiles was generated
		// The file name is bogus since the id wasn't read properly
		final File[] dumpFiles = SerializingStore.makeDumpFileName(invalidId, tmpDir, info.getName()).getParentFile()
												 .listFiles(file -> file.getName().endsWith(SerializingStore.DUMP_FILE_EXTENSION));

		final File[] exceptionFiles = SerializingStore.makeDumpFileName(invalidId, tmpDir, info.getName()).getParentFile()
												 .listFiles(file -> file.getName().endsWith(SerializingStore.EXCEPTION_FILE_EXTENSION));

		assertThat(dumpFiles)
				.isNotNull()
				.hasSize(1);

		assertThat(exceptionFiles)
				.isNotNull()
				.hasSize(1);


		assertThat((QueryDescription) Jackson.MAPPER.readerFor(QueryDescription.class).readValue(dumpFiles[0])).isEqualTo(cQuery);
	}

	/**
	 * Tests if entries with corrupted are removed from the store if configured so.
	 * The dump itself is not testet.
	 */
	@Test
	public void testCorruptionRemoval() {
		log.info("This test will throw some warnings from the SerializingStore.");
		// Set config to remove corrupt entries
		config.setRemoveUnreadableFromStore(true);

		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), USER_STORE_ID);
			store.add(new UserId("testU1"), user);
		}

		{ // Insert two corrupt entries. One with a corrupt key and the other one with a
			// corrupt value
			{
				SerializingStore<String, QueryDescription> store = createSerializedStore(
					config,
					env,
					Validators.newValidator(),
					new StoreInfo<>(USER_STORE_ID.getName(), String.class, QueryDescription.class));
				store.add("not a valid conquery Id", cQuery);
			}

			{
				SerializingStore<UserId, QueryDescription> store = createSerializedStore(
					config,
					env,
					Validators.newValidator(),
					new StoreInfo<>(USER_STORE_ID.getName(), UserId.class, QueryDescription.class));
				store.add(new UserId("testU2"), cQuery);
			}
		}

		{
			// Reopen the store with correct configuration and try to iterate over all
			// entries (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), USER_STORE_ID);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(3);
			expectedResult.setFailedKeys(1);
			expectedResult.setFailedValues(1);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}

		{
			// Reopen again to check that the corrupted values have been removed previously
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), USER_STORE_ID);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(1);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(0);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
	}
}
