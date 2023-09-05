package com.bakdata.conquery.models.datasets.concepts.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendTable;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import org.junit.jupiter.api.Test;

public class FilterSearchItemTest {

	@Test
	public void sortedValidityDates() {

		Dataset dataset = new Dataset();
		dataset.setName("testDataset");

		Table table = new Table();
		table.setDataset(dataset);
		table.setName("testTable");

		Column column = new Column();
		column.setName("testColumn");
		column.setTable(table);

		Column dateColumn1 = new Column();
		column.setName("dateColumn1");
		column.setType(MajorTypeId.DATE);
		column.setTable(table);

		Column dateColumn2 = new Column();
		column.setName("dateColumn2");
		column.setType(MajorTypeId.DATE);
		column.setTable(table);



		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("testConnector");

		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setName("testConcept");

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
		connector.setColumn(column);
		connector.setConcept(concept);
		connector.setValidityDates(validityDates);
		FrontendTable feTable = new FrontEndConceptBuilder(new ConqueryConfig()).createTable(connector);
		
		assertThat(feTable.getDateColumn().getOptions()).containsExactly(
				new FrontendValue(val0.getId().toString(), "val0"),
				new FrontendValue(val1.getId().toString(), "val1"),
				new FrontendValue(val2.getId().toString(), "val2")
		);
	}
}
