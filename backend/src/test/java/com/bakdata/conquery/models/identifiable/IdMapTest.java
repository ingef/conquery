package com.bakdata.conquery.models.identifiable;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Mappers;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;


public class IdMapTest {
	@Test
	public void generalTest() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		Dataset d1 = new Dataset();
		d1.setName("d1");
		Dataset d2 = new Dataset();
		d2.setName("d2");
		
		IdMap<DatasetId, Dataset> idMap = new IdMap<DatasetId, Dataset>();
		idMap.add(d1);
		idMap.add(d2);
		ContainingDataset containedDataset = new ContainingDataset(idMap);
		
		JsonNode json = Mappers.getMapper().valueToTree(containedDataset);
		
		/*assertThat(json.isArray()).isTrue();
		assertThat(json.get(0)).isEqualTo(Jackson.MAPPER.valueToTree(d1));*/
		
		ContainingDataset constructed = Mappers.getMapper().treeToValue(json, ContainingDataset.class);
		assertThat(constructed.idMap.entrySet()).isEqualTo(containedDataset.idMap.entrySet());
	}
	
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class ContainingDataset {
		private IdMap<DatasetId, Dataset> idMap = new IdMap<>();
	}
}
