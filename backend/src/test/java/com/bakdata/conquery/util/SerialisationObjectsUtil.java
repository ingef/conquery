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
import com.bakdata.conquery.io.storage.NamespacedStorageImpl;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.forms.util.ResolutionShortNames;
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
	public static Dataset createDataset(NamespacedStorageImpl storage) {
		return createDataset("test-dataset", storage);
	}

	@NotNull
	public static Dataset createDataset(String name, NamespacedStorageImpl storage) {
		Dataset dataset = new Dataset(name);
		dataset.setNamespacedStorageProvider(storage);
		storage.updateDataset(dataset);
		return dataset;
	}

	@NotNull
	public static ExportForm createExportForm(Dataset dataset, NamespacedStorageImpl storage) {
		final TreeConcept concept = createConcept(dataset, storage);
		final ExportForm exportForm = new ExportForm();
		final AbsoluteMode mode = new AbsoluteMode();
		mode.setDateRange(new Range<>(LocalDate.of(2200, 6, 1), LocalDate.of(2200, 6, 2)));
		mode.setForm(exportForm);

		final CQConcept cqConcept = new CQConcept();

		final CQTable table = new CQTable();
		table.setConcept(cqConcept);
		table.setConnector(concept.getConnectors().get(0).getId());

		// Use ArrayList instead of ImmutableList here because they use different hash code implementations
		cqConcept.setTables(new ArrayList<>(List.of(table)));
		cqConcept.setElements(new ArrayList<>(List.of(concept.getId())));

		exportForm.setTimeMode(mode);
		exportForm.setFeatures(new ArrayList<>(List.of(cqConcept)));
		exportForm.setValues(new TextNode("Some Node"));
		exportForm.setQueryGroupId(new ManagedExecutionId(dataset.getId(), UUID.randomUUID()));
		exportForm.setResolution(new ArrayList<>(List.of(ResolutionShortNames.COMPLETE)));

		storage.updateConcept(concept);

		return exportForm;
	}

	/**
	 * Does not add the produced concept to a store, only dependencies.
	 * Otherwise, it might clash during serdes because init was not executed
	 */
	@NotNull
	public static TreeConcept createConcept(Dataset dataset, NamespacedStorageImpl storage) {
		TreeConcept concept = new TreeConcept();

		concept.setDataset(dataset.getId());
		concept.setLabel("conceptLabel");
		concept.setName("conceptName");

		final SecondaryIdDescription secondaryIdDescription = new SecondaryIdDescription();
		secondaryIdDescription.setDataset(dataset.getId());
		secondaryIdDescription.setName("sid");

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
		table.setLabel("tableLabel");
		table.setName("tableName");
		table.setDataset(dataset.getId());

		column.setTable(table);

		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setLabel("connLabel");
		connector.setName("connName");

		concept.setConnectors(List.of(connector));

		storage.updateDataset(dataset);
		storage.addSecondaryId(secondaryIdDescription);
		storage.addTable(table);

		// Set/Create ids after setting id resolver
		connector.setColumn(column.getId());
		column.setSecondaryId(secondaryIdDescription.getId());

		ValidityDate valDate = ValidityDate.create(dateColumn);
		valDate.setConnector(connector);
		valDate.setLabel("valLabel");
		valDate.setName("valName");
		connector.setValidityDates(List.of(valDate));

		// Initialize Concept
		concept = new TreeConcept.Initializer().convert(concept);

		return concept;
	}

	@NotNull
	public static User createUser(MetaStorage metaStorage) {
		final User user = new User("test-user", "test-user", metaStorage);
		user.setMetaStorage(metaStorage);
		user.updateStorage();
		return user;
	}
}
