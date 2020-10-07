package com.bakdata.conquery.io.xodus.stores;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.SequenceInputStream;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.DataSize;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BigStoreTest {

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

	@Test
	public void testFull() throws JSONException, IOException {
		BigStore<DictionaryId, Dictionary> store = new BigStore<>(new StorageConfig(), Validators.newValidator(), env,
			StoreInfo.DICTIONARIES);
		store.setChunkSize(Ints.checkedCast(DataSize.megabytes(1).toBytes()));

		Dictionary nDict = new MapDictionary();
		nDict.setName("dict");
		nDict.setDataset(new DatasetId("test"));
		DirectDictionary direct = new DirectDictionary(nDict);

		for (int v = 0; v < 1000000; v++) {
			direct.add(Integer.toHexString(v));
		}

		// check if manual serialization deserialization works
		byte[] bytes = Jackson.BINARY_MAPPER.writeValueAsBytes(nDict);
		Dictionary simpleCopy = Jackson.BINARY_MAPPER.readValue(bytes, Dictionary.class);
		for (int v = 0; v < 1000000; v++) {
			assertThat(direct.getId(Integer.toHexString(v))).isEqualTo(v);
		}

		// check if store works
		store.add(nDict.getId(), nDict);

		// check if the bytes in the store are the same as bytes
		assertThat(
			new SequenceInputStream(Iterators.asEnumeration(
				store.getMetaStore().get(nDict.getId()).loadData(store.getDataStore()).map(ByteArrayInputStream::new).iterator())))
					.hasSameContentAs(new ByteArrayInputStream(bytes));

		DirectDictionary copy = new DirectDictionary(store.get(nDict.getId()));
		for (int v = 0; v < 1000000; v++) {
			assertThat(copy.getId(Integer.toHexString(v))).isEqualTo(v);
		}

	}

	@Test
	public void testEmpty() throws JSONException, IOException {
		BigStore<DictionaryId, Dictionary> store = new BigStore<>(new StorageConfig(), Validators.newValidator(), env,
			StoreInfo.DICTIONARIES);
		store.setChunkSize(Ints.checkedCast(DataSize.megabytes(1).toBytes()));

		Dictionary nDict = new MapDictionary();
		nDict.setName("dict");
		nDict.setDataset(new DatasetId("test"));

		// check if manual serialization deserialization works
		byte[] bytes = Jackson.BINARY_MAPPER.writeValueAsBytes(nDict);
		Dictionary simpleCopy = Jackson.BINARY_MAPPER.readValue(bytes, Dictionary.class);
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
