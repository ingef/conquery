package com.bakdata.conquery.models.datasets.concepts.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FETable;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.models.datasets.concepts.FrontEndConceptBuilder;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import org.junit.jupiter.api.Test;

public class FEValueTest {

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
		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("testConnector");
		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setName("testConcept");
		ValidityDate val0 = new ValidityDate();
		val0.setName("val0");
		val0.setConnector(connector);
		ValidityDate val1 = new ValidityDate();
		val1.setName("val1");
		val1.setConnector(connector);
		ValidityDate val2 = new ValidityDate();
		val2.setName("val2");
		val2.setConnector(connector);
		List<ValidityDate> validityDates = List.of(val0, val1, val2);
		connector.setColumn(column);
		connector.setConcept(concept);
		connector.setValidityDates(validityDates);
		FETable feTable = FrontEndConceptBuilder.createTable(connector);
		
		assertThat(feTable.getDateColumn().getOptions()).containsExactly(
				new FEValue("val0", val0.getId().toString()),
				new FEValue("val1", val1.getId().toString()),
				new FEValue("val2", val2.getId().toString())
			);
	}
}
