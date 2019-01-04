package com.bakdata.conquery.models.concepts;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;

public class SerializationTests {

	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");
		Concepts concepts = new Concepts();
		concepts.setDataset(dataset);
		
		SerializationTestUtil.testSerialization(dataset, Dataset.class);
	}
}
