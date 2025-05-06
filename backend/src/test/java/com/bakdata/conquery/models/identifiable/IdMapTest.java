package com.bakdata.conquery.models.identifiable;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;


public class IdMapTest {
	@Test
	public void generalTest() throws IOException {
		NamespaceStorage storage = new NonPersistentStoreFactory().createNamespaceStorage();

		Dataset d1 = new Dataset("d1");
		d1.setStorageProvider(storage);

		Dataset d2 = new Dataset("d2");
		d2.setStorageProvider(storage);

		IdMap<DatasetId, Dataset> idMap = new IdMap<DatasetId, Dataset>();
		idMap.add(d1);
		idMap.add(d2);
		ContainingDataset containedDataset = new ContainingDataset(idMap);

		ObjectMapper mapper = Jackson.MAPPER.copy();

		storage.injectInto(mapper);

		JsonNode json = mapper.valueToTree(containedDataset);
		
		/*assertThat(json.isArray()).isTrue();
		assertThat(json.get(0)).isEqualTo(Jackson.MAPPER.valueToTree(d1));*/
		
		ContainingDataset constructed = mapper.treeToValue(json, ContainingDataset.class);
		assertThat(constructed.idMap.entrySet()).isEqualTo(containedDataset.idMap.entrySet());
	}
	
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class ContainingDataset {
		private IdMap<DatasetId, Dataset> idMap = new IdMap<>();
	}
}
