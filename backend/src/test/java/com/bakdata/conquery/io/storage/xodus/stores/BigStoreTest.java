package com.bakdata.conquery.io.storage.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.SerdesTarget;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Ints;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.DataSize;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BigStoreTest {

	private File tmpDir;
	private Environment env;

	private static final CentralRegistry CENTRAL_REGISTRY = new CentralRegistry();
	private static SingletonNamespaceCollection NAMESPACE_COLLECTION = new SingletonNamespaceCollection(CENTRAL_REGISTRY);
	private static ObjectMapper MAPPER = NAMESPACE_COLLECTION.injectIntoNew(Jackson.BINARY_MAPPER);

	@BeforeAll
	public static void setupRegistry(){
		CENTRAL_REGISTRY.register(Dataset.PLACEHOLDER);
	}

	@BeforeEach
	public void init() throws IOException {
		tmpDir = Files.createTempDirectory(BigStoreTest.class.getSimpleName()).toFile();
		tmpDir.mkdirs();
		env = Environments.newInstance(tmpDir);

		MAPPER.setConfig(MAPPER.getDeserializationConfig().withAttribute(SerdesTarget.class, SerdesTarget.MANAGER));
	}

	@AfterEach
	public void destroy() throws IOException {
		env.close();
		tmpDir.delete();
	}

	@Test
	public void testFull() throws JSONException, IOException {

		BigStore<DictionaryId, Dictionary> store = new BigStore<>(new XodusStoreFactory(), Validators.newValidator(), env,
																  StoreMappings.DICTIONARIES.storeInfo(), (e) -> {}, (e) -> {}, MAPPER
		);


		store.setChunkByteSize(Ints.checkedCast(DataSize.megabytes(1).toBytes()));

		Dictionary nDict = new MapDictionary(Dataset.PLACEHOLDER, "dict");

		for (int v = 0; v < 1000000; v++) {
			nDict.add(Integer.toHexString(v).getBytes());
		}

		// check if manual serialization deserialization works
		byte[] bytes = Jackson.BINARY_MAPPER.writeValueAsBytes(nDict);


		Dictionary simpleCopy = MAPPER.readValue(bytes, Dictionary.class);


		for (int v = 0; v < 1000000; v++) {
			assertThat(simpleCopy.getId(Integer.toHexString(v).getBytes())).isEqualTo(v);
		}

		// check if store works
		store.add(nDict.getId(), nDict);

		// check if the bytes in the store are the same as bytes
		assertThat(
			new SequenceInputStream(Iterators.asEnumeration(
				store.getMetaStore().get(nDict.getId()).loadData(store.getDataStore()).map(ByteArrayInputStream::new).iterator())))
					.hasSameContentAs(new ByteArrayInputStream(bytes));

		EncodedDictionary copy = new EncodedDictionary(store.get(nDict.getId()), StringTypeEncoded.Encoding.UTF8);
		for (int v = 0; v < 1000000; v++) {
			assertThat(copy.getId(Integer.toHexString(v))).isEqualTo(v);
		}

	}

	@Test
	public void testEmpty() throws JSONException, IOException {
		BigStore<DictionaryId, Dictionary> store = new BigStore<>(new XodusStoreFactory(), Validators.newValidator(), env,
																  StoreMappings.DICTIONARIES.storeInfo(), (e) -> {}, (e) -> {}, MAPPER
		);
		store.setChunkByteSize(Ints.checkedCast(DataSize.megabytes(1).toBytes()));

		Dictionary nDict = new MapDictionary(Dataset.PLACEHOLDER,"dict");

		// check if manual serialization deserialization works
		byte[] bytes = MAPPER.writeValueAsBytes(nDict);
		Dictionary simpleCopy = MAPPER.readValue(bytes, Dictionary.class);
		assertThat(simpleCopy).isEmpty();

		// check if store works
		store.add(nDict.getId(), nDict);

		// check if the bytes in the store are the same as bytes
		assertThat(
			new SequenceInputStream(Iterators.asEnumeration(
				store.getMetaStore().get(nDict.getId()).loadData(store.getDataStore()).map(ByteArrayInputStream::new).iterator())))
					.hasSameContentAs(new ByteArrayInputStream(bytes));

		Dictionary copy = store.get(nDict.getId());
		assertThat(copy).isEmpty();
	}

}
