package com.bakdata.conquery.io.storage.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.IStoreInfo;
import com.bakdata.conquery.io.storage.StoreInfo;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.google.common.io.Files;
import io.dropwizard.jersey.validation.Validators;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class SerializingStoreDumpTest {

	private File tmpDir;
	private Environment env;
	private XodusStoreFactory config;

	// Test data
	private final ManagedQuery managedQuery = new ManagedQuery(null, null, new Dataset("dataset"));
	private final ConceptQuery cQuery = new ConceptQuery(
		new CQReusedQuery(managedQuery.getId()));
	private final User user = new User("username", "userlabel");

	@BeforeEach
	public void init() {
		tmpDir = Files.createTempDir();
		config = new XodusStoreFactory();
		env = Environments.newInstance(tmpDir, config.getXodus().createConfig());
	}

	@AfterEach
	public void destroy() throws IOException {
		env.close();
		FileUtils.deleteDirectory(tmpDir);
	}

	private <KEY, VALUE> SerializingStore<KEY, VALUE> createSerializedStore(XodusStoreFactory config, Environment environment, Validator validator, IStoreInfo storeId) {
		return new SerializingStore<>(config, new XodusStore(environment, storeId, new ArrayList<>(), (e) -> {}, (e) -> {}), validator, storeId, config.getObjectMapper());
	}

	/**
	 * Tests if entries with corrupted values are dumped.
	 */
	@Test
	public void testCorruptValueDump() throws JSONException, IOException {
		// Set dump directory to this tests temp-dir
		config.setUnreadableDataDumpDirectory(Optional.of(tmpDir));

		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			store.add(user.getId(), user);
		}

		{
			// Open that store again, with a different config to insert a corrupt entry
			// (UserId & ManagedQuery)
			SerializingStore<UserId, QueryDescription> store = createSerializedStore(
				config,
				env,
				Validators.newValidator(),
				new CorruptableStoreInfo(StoreInfo.AUTH_USER.getName(), UserId.class, QueryDescription.class));
			store.add(new UserId("testU2"), cQuery);
		}

		{
			// Reopen the store with the initial value and try to iterate over all entries
			// (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(1);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}

		// Test if the correct number of dumpfiles was generated
		Condition<File> dumpFileCond = new Condition<>(f -> f.getName().endsWith(SerializingStore.DUMP_FILE_EXTENTION), "dump file");
		assertThat(tmpDir.listFiles()).areExactly(1, dumpFileCond);

		// Test if the dump is correct
		File dumpFile = getDumpFile(dumpFileCond);

		assertThat((QueryDescription) Jackson.MAPPER.readerFor(QueryDescription.class).readValue(dumpFile)).isEqualTo(cQuery);
	}

	private File getDumpFile(Condition<File> dumpFileCond) {
		return tmpDir.listFiles((name) -> dumpFileCond.matches(name))[0];
	}

	/**
	 * Tests if entries with corrupted keys are dumped.
	 */
	@Test
	public void testCorruptKeyDump() throws JSONException, IOException {
		// Set dump directory to this tests temp-dir
		config.setUnreadableDataDumpDirectory(Optional.of(tmpDir));

		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			store.add(new UserId("testU1"), user);
		}

		{
			// Open that store again, with a different config to insert a corrupt entry
			// (String & ManagedQuery)
			SerializingStore<String, QueryDescription> store = createSerializedStore(
				config,
				env,
				Validators.newValidator(),
				new CorruptableStoreInfo(StoreInfo.AUTH_USER.getName(), String.class, QueryDescription.class));
			store.add("not a valid conquery Id", cQuery);
		}

		{
			// Reopen the store with the initial value and try to iterate over all entries
			// (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(1);
			expectedResult.setFailedValues(0);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}

		// Test if the correct number of dumpfiles was generated
		Condition<File> dumpFileCond = new Condition<>(f -> f.getName().endsWith(SerializingStore.DUMP_FILE_EXTENTION), "dump file");
		assertThat(tmpDir.listFiles()).areExactly(1, dumpFileCond);

		// Test if the dump is correct
		File dumpFile = getDumpFile(dumpFileCond);

		assertThat((QueryDescription) Jackson.MAPPER.readerFor(QueryDescription.class).readValue(dumpFile)).isEqualTo(cQuery);
	}

	/**
	 * Tests if entries with corrupted are removed from the store if configured so.
	 * The dump itself is not testet.
	 */
	@Test
	public void testCorruptionRemoval() throws JSONException, IOException {
		log.info("This test will throw some warnings from the SerializingStore.");
		// Set config to remove corrupt entries
		config.setRemoveUnreadableFromStore(true);

		{
			// Open a store and insert a valid key-value pair (UserId & User)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			store.add(new UserId("testU1"), user);
		}

		{ // Insert two corrupt entries. One with a corrupt key and the other one with a
			// corrupt value
			{
				SerializingStore<String, QueryDescription> store = createSerializedStore(
					config,
					env,
					Validators.newValidator(),
					new CorruptableStoreInfo(StoreInfo.AUTH_USER.getName(), String.class, QueryDescription.class));
				store.add("not a valid conquery Id", cQuery);
			}

			{
				SerializingStore<UserId, QueryDescription> store = createSerializedStore(
					config,
					env,
					Validators.newValidator(),
					new CorruptableStoreInfo(StoreInfo.AUTH_USER.getName(), UserId.class, QueryDescription.class));
				store.add(new UserId("testU2"), cQuery);
			}
		}

		{
			// Reopen the store with correct configuration and try to iterate over all
			// entries (this triggers the dump or removal of invalid entries)
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
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
			SerializingStore<UserId, User> store = createSerializedStore(config, env, Validators.newValidator(), StoreInfo.AUTH_USER);
			IterationStatistic expectedResult = new IterationStatistic();
			expectedResult.setTotalProcessed(1);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(0);

			// Iterate (do nothing with the entries themselves)
			IterationStatistic result = store.forEach((k, v, s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
	}

	@RequiredArgsConstructor
	@Getter
	private static class CorruptableStoreInfo implements IStoreInfo {

		private final String name;
		private final Class<?> keyType;
		private final Class<?> valueType;
	}
}
