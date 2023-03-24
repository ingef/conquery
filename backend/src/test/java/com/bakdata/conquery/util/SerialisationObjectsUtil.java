package com.bakdata.conquery.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.forms.util.ResolutionShortNames;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for nested objects needed in {@link com.bakdata.conquery.models.SerializationTests}
 */
@UtilityClass
public class SerialisationObjectsUtil {


	@NotNull
	public static Dataset createDataset(CentralRegistry registry) {
		final Dataset dataset = new Dataset("test-dataset");
		registry.register(dataset);
		return dataset;
	}

	@NotNull
	public static TreeConcept createConcept(CentralRegistry registry, Dataset dataset) {
		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setLabel("conceptLabel");
		concept.setName("conceptName");

		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);

		Column dateColumn = new Column();
		dateColumn.setLabel("colLabel2");
		dateColumn.setName("colName2");
		dateColumn.setType(MajorTypeId.DATE);
		dateColumn.setTable(table);


		table.setColumns(new Column[]{column, dateColumn});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");

		column.setTable(table);

		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setLabel("connLabel");
		connector.setName("connName");
		connector.setColumn(column);

		concept.setConnectors(List.of(connector));

		ValidityDate valDate = new ValidityDate();
		valDate.setColumn(dateColumn);
		valDate.setConnector(connector);
		valDate.setLabel("valLabel");
		valDate.setName("valName");
		connector.setValidityDates(List.of(valDate));

		registry.register(concept);
		registry.register(column);
		registry.register(dateColumn);
		registry.register(table);
		registry.register(connector);
		registry.register(valDate);
		return concept;
	}

	@NotNull
	public static ExportForm createExportForm(CentralRegistry registry, Dataset dataset) {
		final TreeConcept concept = createConcept(registry, dataset);
		final ExportForm exportForm = new ExportForm();
		final AbsoluteMode mode = new AbsoluteMode();
		mode.setDateRange(new Range<>(LocalDate.of(2200, 6, 1), LocalDate.of(2200, 6, 2)));
		mode.setForm(exportForm);

		final CQConcept cqConcept = new CQConcept();

		final CQTable table = new CQTable();
		table.setConcept(cqConcept);
		table.setConnector(concept.getConnectors().get(0));

		// Use ArrayList instead of ImmutalbeList here because they use different hash code implementations
		cqConcept.setTables(new ArrayList<>(List.of(table)));
		cqConcept.setElements(new ArrayList<>(List.of(concept)));

		exportForm.setTimeMode(mode);
		exportForm.setFeatures(new ArrayList<>(List.of(cqConcept)));
		exportForm.setValues(new TextNode("Some Node"));
		exportForm.setQueryGroupId(new ManagedExecutionId(dataset.getId(), UUID.randomUUID()));
		exportForm.setResolution(new ArrayList<>(List.of(ResolutionShortNames.COMPLETE)));
		return exportForm;
	}

	@NotNull
	public static User createUser(CentralRegistry registry, MetaStorage storage) {
		final User user = new User("test-user", "test-user", storage);
		registry.register(user);

		user.updateStorage();
		return user;
	}
}
