package com.bakdata.conquery.models.events;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import org.junit.jupiter.api.Test;

class CBlockTest {
	@Test
	public void serialize() throws IOException, JSONException {
		final CentralRegistry registry = new CentralRegistry();

		final Dataset dataset = new Dataset();
		dataset.setName("dataset");

		final TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setName("concept");

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("connector");

		connector.setConcept(concept);
		concept.setConnectors(List.of(connector));

		final Table table = new Table();
		table.setName("table");
		table.setDataset(dataset);

		final Import imp = new Import(table);
		imp.setName("import");

		final Bucket bucket = new Bucket(0, 0, 10, new ColumnStore[0], Collections.emptySet(),new int[10], new int[10], imp);


		final CBlock cBlock = CBlock.createCBlock(connector, bucket, 10);

		registry.register(dataset)
				.register(table)
				.register(concept)
				.register(connector)
				.register(bucket)
				.register(imp);

		SerializationTestUtil.forType(CBlock.class)
							 .registry(registry)
							 .test(cBlock);
	}

}