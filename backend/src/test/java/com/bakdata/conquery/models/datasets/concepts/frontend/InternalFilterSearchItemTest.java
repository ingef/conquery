package com.bakdata.conquery.models.datasets.concepts.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendTable;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import com.bakdata.conquery.util.extensions.NamespaceStorageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InternalFilterSearchItemTest {

	@RegisterExtension
	private static final NamespaceStorageExtension NAMESPACE_STORAGE_EXTENSION = new NamespaceStorageExtension();
	private static final NamespacedStorage NAMESPACED_STORAGE = NAMESPACE_STORAGE_EXTENSION.getStorage();
	public static final NamespacedStorageProvider STORAGE_PROVIDER = new TestNamespacedStorageProvider(NAMESPACED_STORAGE);


	@Test
	public void sortedValidityDates() throws Exception {

		Dataset dataset = new Dataset();
		dataset.setName("testDataset");
		dataset.setStorageProvider(STORAGE_PROVIDER);

		NAMESPACED_STORAGE.updateDataset(dataset);

		Table table = new Table();
		table.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		table.setName("testTable");

		Column column = new Column();
		column.setName("testColumn");
		column.setTable(table);

		Column dateColumn1 = new Column();
		dateColumn1.setName("dateColumn1");
		dateColumn1.setType(MajorTypeId.DATE);
		dateColumn1.setTable(table);

		Column dateColumn2 = new Column();
		dateColumn2.setName("dateColumn2");
		dateColumn2.setType(MajorTypeId.DATE);
		dateColumn2.setTable(table);

		table.init();

		NAMESPACED_STORAGE.addTable(table);


		TreeConcept concept = new TreeConcept();
		concept.setNamespacedStorageProvider(NAMESPACED_STORAGE);
		concept.setName("testConcept");

		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("testConnector");

		ValidityDate val0 = ValidityDate.create(dateColumn1);
		val0.setName("val0");
		val0.setConnector(connector);

		ValidityDate val1 = ValidityDate.create(dateColumn2);
		val1.setName("val1");
		val1.setConnector(connector);

		ValidityDate val2 = ValidityDate.create(dateColumn1, dateColumn2);
		val2.setName("val2");
		val2.setConnector(connector);

		List<ValidityDate> validityDates = List.of(val0, val1, val2);
		connector.setColumn(column.getId());
		connector.setConcept(concept);
		connector.setValidityDates(validityDates);

		concept.init();

		NAMESPACED_STORAGE.updateConcept(concept);


		FrontendTable feTable = new FrontEndConceptBuilder(new ConqueryConfig()).createTable(connector);

		assertThat(feTable.getDateColumn().getOptions()).containsExactly(
				new FrontendValue(val0.getId().toString(), "val0"),
				new FrontendValue(val1.getId().toString(), "val1"),
				new FrontendValue(val2.getId().toString(), "val2")
		);
	}
}
