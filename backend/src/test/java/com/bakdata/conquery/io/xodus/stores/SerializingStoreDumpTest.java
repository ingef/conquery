package com.bakdata.conquery.io.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.validation.Validator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.io.xodus.stores.SerializingStore.IterationResult;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.google.common.io.Files;
import io.dropwizard.jersey.validation.Validators;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SerializingStoreDumpTest {

	private File tmpDir;
	private Environment env;
	
	@BeforeEach
	public void init() {
		tmpDir = Files.createTempDir();
		env = Environments.newInstance(tmpDir);
	}
	
	@AfterEach
	public void destroy() throws IOException {
		env.close();
		FileUtils.deleteDirectory(tmpDir);
	}
	
	private <KEY, VALUE> SerializingStore<KEY, VALUE> createSerializedStore(Environment environment, Validator validator, IStoreInfo storeId) {
		return new SerializingStore<>(new XodusStore(environment, storeId), validator, storeId);
	}
	
	/**
	 * Tests if entries with corrupted values are dumped. The dump itself is not testet. 
	 */
	@Test
	public void testCorruptValueDump() throws JSONException, IOException {
		// Set dump directory to this tests temp-dir
		ConqueryConfig.getInstance().getStorage().setUnreadbleDataDumpDirectory(Optional.of(tmpDir));
		
		// Open a store and insert a valid key-value pair (UserId & User)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			User user = new User("username","userlabel");
			store.add(new UserId("testU1"), user);
		}
		
		// Open that store again, with a different config to insert a corrupt entry (UserId & ManagedQuery)
		try (SerializingStore<UserId, ManagedExecution<?>> store = createSerializedStore(env, Validators.newValidator(), new CorruptableStoreInfo(StoreInfo.AUTH_USER.getXodusName(), UserId.class, ManagedExecution.class))){
			ManagedQuery mQuery = new ManagedQuery(new ConceptQuery(new CQStup()), new UserId("testU"), new DatasetId("testD"));
			store.add(new UserId("testU2"), mQuery);
		}
		
		// Reopen the store with the initial value and try to iterate over all entries (this triggers the dump or removal of invalid entries)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			IterationResult expectedResult = new IterationResult();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(1);
			
			// Iterate (do nothing with the entries themselves)
			IterationResult result = store.forEach((k,v,s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
		
		// Test if the correct number of dumpfiles was generated
		Condition<File> dumpFile = new Condition<>(f -> f.getName().endsWith(SerializingStore.DUMP_FILE_EXTENTION) , "dump file");
		assertThat(tmpDir.listFiles()).areExactly(1, dumpFile);
	}
	

	/**
	 * Tests if entries with corrupted keys are dumped. The dump itself is not testet. 
	 */
	@Test
	public void testCorruptKeyDump() throws JSONException, IOException {
		// Set dump directory to this tests temp-dir
		ConqueryConfig.getInstance().getStorage().setUnreadbleDataDumpDirectory(Optional.of(tmpDir));
		
		// Open a store and insert a valid key-value pair (UserId & User)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			User user = new User("username","userlabel");
			store.add(new UserId("testU1"), user);
		}
		
		// Open that store again, with a different config to insert a corrupt entry (String & ManagedQuery)
		try (SerializingStore<String, ManagedExecution<?>> store = createSerializedStore(env, Validators.newValidator(), new CorruptableStoreInfo(StoreInfo.AUTH_USER.getXodusName(), String.class, ManagedExecution.class))){
			ManagedQuery mQuery = new ManagedQuery(new ConceptQuery(new CQStup()), new UserId("testU"), new DatasetId("testD"));
			store.add("not a valid conquery Id", mQuery);
		}
		
		// Reopen the store with the initial value and try to iterate over all entries (this triggers the dump or removal of invalid entries)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			IterationResult expectedResult = new IterationResult();
			expectedResult.setTotalProcessed(2);
			expectedResult.setFailedKeys(1);
			expectedResult.setFailedValues(0);
			
			// Iterate (do nothing with the entries themselves)
			IterationResult result = store.forEach((k,v,s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
		
		// Test if the correct number of dumpfiles was generated
		Condition<File> dumpFile = new Condition<>(f -> f.getName().endsWith(SerializingStore.DUMP_FILE_EXTENTION) , "dump file");
		assertThat(tmpDir.listFiles()).areExactly(1, dumpFile);
	}
	

	/**
	 * Tests if entries with corrupted are removed from the store if configured so. The dump itself is not testet. 
	 */
	@Test
	public void testCorruptionRemoval() throws JSONException, IOException {
		// Set config to remove corrupt entries
		ConqueryConfig.getInstance().getStorage().setRemoveUnreadablesFromStore(true);
		
		// Open a store and insert a valid key-value pair (UserId & User)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			User user = new User("username","userlabel");
			store.add(new UserId("testU1"), user);
		}
		
		{ // Insert two corrupt entries. One with a corrupt key and the other one with a corrupt value			
			try (SerializingStore<String, ManagedExecution<?>> store = createSerializedStore(env, Validators.newValidator(), new CorruptableStoreInfo(StoreInfo.AUTH_USER.getXodusName(), String.class, ManagedExecution.class))){
				ManagedQuery mQuery = new ManagedQuery(new ConceptQuery(new CQStup()), new UserId("testU"), new DatasetId("testD"));
				store.add("not a valid conquery Id", mQuery);
			}
			
			try (SerializingStore<UserId, ManagedExecution<?>> store = createSerializedStore(env, Validators.newValidator(), new CorruptableStoreInfo(StoreInfo.AUTH_USER.getXodusName(), UserId.class, ManagedExecution.class))){
				ManagedQuery mQuery = new ManagedQuery(new ConceptQuery(new CQStup()), new UserId("testU"), new DatasetId("testD"));
				store.add(new UserId("testU2"), mQuery);
			}
		}
		
		// Reopen the store with correct configuration and try to iterate over all entries (this triggers the dump or removal of invalid entries)
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			IterationResult expectedResult = new IterationResult();
			expectedResult.setTotalProcessed(3);
			expectedResult.setFailedKeys(1);
			expectedResult.setFailedValues(1);
			
			// Iterate (do nothing with the entries themselves)
			IterationResult result = store.forEach((k,v,s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
		
		// Reopen again to check that the corrupted values have been removed previously
		try (SerializingStore<UserId, User> store = createSerializedStore(env, Validators.newValidator(), StoreInfo.AUTH_USER)){
			IterationResult expectedResult = new IterationResult();
			expectedResult.setTotalProcessed(1);
			expectedResult.setFailedKeys(0);
			expectedResult.setFailedValues(0);
			
			// Iterate (do nothing with the entries themselves)
			IterationResult result = store.forEach((k,v,s) -> {});
			assertThat(result).isEqualTo(expectedResult);
		}
	}
	
	@RequiredArgsConstructor
	@Getter
	private static class CorruptableStoreInfo implements IStoreInfo{
		private final String xodusName;
		private final Class<?> keyType;
		private final Class<?> valueType;
	}

	
	@CPSType(id="STUP", base=CQElement.class)
	private static class CQStup implements CQElement {

		@Override
		public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
			// nothing to do
			return null;
		}

		@Override
		public void collectResultInfos(ResultInfoCollector collector) {
			// nothing to do
		}
	}

}
