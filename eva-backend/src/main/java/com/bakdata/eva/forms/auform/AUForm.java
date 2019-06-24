package com.bakdata.eva.forms.auform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQMultiSelectFilter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQSelectFilter;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQNegation;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.absexport.AbsExportForm;
import com.bakdata.eva.forms.absexport.AbsExportGenerator;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.FixedColumn;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.models.forms.DateContextMode;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "AU_FORM", base = Form.class)
public class AUForm extends StatisticForm {

	public static final String DISABILITY_FILTER_ID = "krankheitsursache";
	private static final String ACCIDENT_AT_WORK_LABEL = "Verletzungen durch Arbeitsunfall";
	private static final String NO_ACCIDENT_AT_WORK_LABEL = "Verletzungen ohne Arbeitsunfall";
	public static final String MISCELLANEOUS_LABEL = "Sonstiges";
	public static final String DISABILITY_DATE_TYPE = "au-zeit";
	private static final Set<String> DISALLOWED_FIRST_LEVEL_CONCEPTS = Set
		.of(
			"g00-g99",
			"m00-m99",
			"j00-j99",
			"i00-i99",
			"f00-f99",
			"r00-r99",
			"l00-l99",
			"h00-h59",
			"h60-h95",
			"k00-k93",
			"s00-t98",
			"a00-b99");
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull
	@Valid
	private Range<LocalDate> dateRange;
	private List<CQOr> baseCondition = new ArrayList<>();
	private List<CQOr> features = new ArrayList<>();
	@JsonIgnore
	private User formUser;
	private String joinedConditionName = "";

	private static String[] ACCIDENT_AT_WORK_CODE = new String[] { "Arbeitsunfall" };
	private static String[] NO_ACCIDENT_AT_WORK_CODES = new String[] {
		"sonstiger Unfall",
		"normale Erkrankung",
		"Reha RV",
		"unbekannt",
		"Wegeunfall",
		"Berufskrankheit",
		"Verkehrsunfall",
		"Sportunfall",
		"Raufhandel/Schlägerei",
		"Suizidversuch",
		"Wehrdienstbeschädigung" };

	@JsonIgnore
	private BiConsumer<ColumnDescriptor, CQOr> columnManipulator = (cd, or) -> {
		if (baseCondition.contains(or)) {
			cd.setBaseCondition(true);
		}
		else {
			cd.setBaseCondition(false);
		}
	};

	@Override
	public void init(Namespaces namespaces, User user) {
		this.formUser = user;
		String datasetName = queryGroup.getDataset().getName();
		List<FixedColumn> columns = createBaseDataColumns(datasetName);
		columns.addAll(createICDColumns(namespaces, datasetName));

		this.setFixedFeatures(columns.toArray(new FixedColumn[0]));
		
		// Check if Concepts supplied by user are from ICD Concept
		ConceptId icdId = new ConceptId(queryGroup.getDataset(), "icd");
		long icdConceptCount = features.stream()
			.flatMap(or -> or.getChildren().stream())
			.filter(CQConcept.class::isInstance)
			.map(CQConcept.class::cast)
			.map(c -> c.getIds().get(0).findConcept())
			.filter(id -> id.equals(icdId)).count();
		if(icdConceptCount != features.size()) {
			throw new IllegalArgumentException("Only "+icdConceptCount+" of "+features.size()+" concepts belong to "+ icdId+". However, all need to stem form there at the moment");
		}

		// CNS and sensory organs group
		List<FixedColumn> sense = List
			.of(
				FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "g00-g99"),
				FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "h00-h59"),
				FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "h60-h95"));
		features.add(createFixedOrGroup(namespaces, false, "Nervensystem und Sinnesorgane", sense));

		// digestion organs
		List<FixedColumn> digest = List
			.of(
				FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "k00-k93"),
				FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "a00-b99", "a00-a09"));
		features.add(createFixedOrGroup(namespaces, false, "Verdauungsorgane", digest));

		features.add(createMiscellaneousConcept(namespaces, findMiscellaneousConceptIds(namespaces.resolve(queryGroup.getDataset()))));
		
		// Apply concept configuration to all resolved fields
		features.forEach(or -> {
			or.getChildren().forEach(c -> applyConceptConfig(namespaces,(CQConcept) c));
		});
		// place condition at the end of all fixed columns
		if (!baseCondition.isEmpty()) {
			features.add(baseCondition.get(0));
			joinedConditionName = createJoinedConditionName(baseCondition, namespaces.resolve(queryGroup.getDataset()));
		}
		super.init(namespaces, user);
	}

	@Override
	protected JsonNode toStatisticView() {
		return Jackson.MAPPER.valueToTree(AUStatisticsAPI.from(this));
	}

	@Override
	public ManagedQuery executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		AbsExportForm form = new AbsExportForm();
		form.setColumns(new ArrayList<>(getColumns()));
		form.setDateRange(dateRange);
		form.setFixedFeatures(getFixedFeatures());
		form.setQueryGroup(queryGroup);
		form.setFeatures(features);
		form.init(namespaces, user);

		return new AbsExportGenerator(dataset, user, namespaces).executeQuery(form, DateContextMode.QUARTER_WISE, false);
	}

	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays.asList(new FeatureGroupDescription.GroupFD(features, FeatureGroup.SINGLE_GROUP, columnManipulator));
	}

	@Override
	public List<ManagedExecutionId> getUsedQueries() {
		return Collections.singletonList(queryGroup);
	}

	private List<FixedColumn> createBaseDataColumns(String dataset) {
		List<FixedColumn> columns = new ArrayList<>(10);
		columns.add(createLabeledColumn("Alter", dataset, "alter"));
		columns.add(createLabeledColumn("Geschlecht", dataset, "geschlecht"));
		columns.add(createLabeledColumn("AU-Tage", dataset, "au_dauer"));
		columns.add(createLabeledColumn("Versichertentage", dataset, "versichertentage"));
		return columns;
	}

	private List<FixedColumn> createICDColumns(Namespaces namespaces, String datasetName) {
		List<FixedColumn> columns = new ArrayList<>(8);
		columns.add(createLabeledColumn("Muskel- und Skeletterkrankungen", datasetName, "icd", "m00-m99"));
		columns.add(createLabeledColumn("Atmungsorgane", datasetName, "icd", "j00-j99"));
		columns.add(createLabeledColumn("Kreislauf", datasetName, "icd", "i00-i99"));
		columns.add(createLabeledColumn("Psychische Erkrankungen", datasetName, "icd", "f00-f99"));
		columns.add(createLabeledColumn("Symptome und Affektionen", datasetName, "icd", "r00-r99"));
		columns.add(createLabeledColumn("Hauterkrankung", datasetName, "icd", "l00-l99"));
		columns.add(createLabeledColumn("Neubildungen", datasetName, "icd", "c00-d48"));
		columns.add(createS00T98Column(ACCIDENT_AT_WORK_CODE, ACCIDENT_AT_WORK_LABEL, namespaces, datasetName));
		columns.add(createS00T98Column(NO_ACCIDENT_AT_WORK_CODES, NO_ACCIDENT_AT_WORK_LABEL, namespaces, datasetName));

		addConceptConfig(namespaces, columns);
		return columns;
	}

	/**
	 * Adds configurations for the later resolved columns.
	 * @param namespaces
	 * @param columns
	 */
	private void addConceptConfig(Namespaces namespaces, List<FixedColumn> columns) {
		for (FixedColumn column : columns) {
			chainColumnManipulator(column, new SelectSetter(namespaces));
			chainColumnManipulator(column, new DisabilityTableFilter());
			chainColumnManipulator(column, new DefaultDateSelection(namespaces));
		}
	}
	
	/**
	 * Apply configurations on already supplied CQConcepts.
	 * @param namespaces
	 * @param columns
	 */
	private void applyConceptConfig(Namespaces namespaces, CQConcept concept) {
		new SelectSetter(namespaces).accept(concept);
		new DisabilityTableFilter().accept(concept);
		new DefaultDateSelection(namespaces).accept(concept);
	}

	/**
	 * combines given fixed columns into a OR-ed group
	 * 
	 * @param dataset
	 * @param exclude
	 * @param label
	 * @return
	 */
	private CQOr createFixedOrGroup(Namespaces namespaces, boolean exclude, String label, List<FixedColumn> fixedColumns) {
		CQOr group = new CQOr();
		List<CQElement> elements = new ArrayList<>();
		for (FixedColumn column : fixedColumns) {
			ConceptElement<?> conceptElement = column.resolve(namespaces);
			CQConcept concept = new CQConcept();
			concept.setLabel(label.isEmpty() ? conceptElement.getName() : label);
			concept.setIds(Arrays.asList(conceptElement.getId()));
			concept
				.setTables(
					conceptElement
						.getConcept()
						.getConnectors()
						.stream()
						.filter(connector -> connector.getName().equalsIgnoreCase("au_fall"))
						.map(connector -> {
							CQTable table = new CQTable();
							table.setId(connector.getId());
							table.setConcept(concept);
							table.setFilters(new ArrayList<>());
							return table;
						})
						.collect(Collectors.toList()));
			column.getConceptConfiguration().accept(concept);
			elements.add(concept);
		}
		group.setChildren(elements);

		if (exclude) {
			CQOr result = new CQOr();
			CQNegation negation = new CQNegation();
			result.setChildren(Collections.singletonList(negation));
			negation.setChild(group);
			return result;
		}
		else {
			return group;
		}
	}

	private FixedColumn createS00T98Column(String[] filterValues, String label, Namespaces namespaces, String datasetName) {
		FixedColumn column = FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "icd", "s00-t98");
		column.setConceptConfiguration(concept -> {
			concept.setLabel(label);
			concept.getTables().removeIf(DisabilityTableFilter::isNotDisabilityTable);
			concept.getTables().stream().forEach(table -> {
				CQMultiSelectFilter f = new CQMultiSelectFilter();
				Filter<?> filter = namespaces
					.resolve(FilterId.Parser.INSTANCE.parse(IId.JOINER.join(datasetName, "icd", "au_fall", DISABILITY_FILTER_ID)));
				f.setFilter(filter);
				f.setValue(filterValues);
				table.setFilters(Collections.singletonList(f));
			});
		});
		return column;
	}

	@Override
	protected String[] getAdditionalHeader() {
		return new String[] { "quarter", "date_range" };
	}

	private String createJoinedConditionName(List<CQOr> baseCondition, Dataset dataset) {
		List<ConceptElementId<?>> nameParts = ((CQConcept) baseCondition.get(0).getChildren().get(0)).getIds();
		return Objects.toString(nameParts.get(0).getComponents().get(nameParts.get(0).getComponents().size() - 2));
	}

	private FixedColumn createLabeledColumn(String label, String... nameParts) {
		FixedColumn column = FixedColumn.of(FeatureGroup.SINGLE_GROUP, nameParts);
		column.setConceptConfiguration(concept -> concept.setLabel(label));
		return column;
	}

	/**
	 * wraps an existing concept configuration within date type selection
	 * configurator
	 * 
	 * @param column
	 * @return modified column
	 */
	@RequiredArgsConstructor
	private static class DefaultDateSelection implements Consumer<CQConcept>{
		private final Namespaces namespaces;
		
		@Override
		public void accept(CQConcept concept) {
			concept.getTables().stream().forEach(table -> {
				CQSelectFilter dateFilter = createDateSelectionFilter(namespaces.resolve(table.getId()));
				if (table.getFilters() == null || table.getFilters().isEmpty())
					table.setFilters(new ArrayList<>());
				table.getFilters().add(dateFilter);
			});
		}
		
		private static CQSelectFilter createDateSelectionFilter(Connector connector) {
			if (connector.getValidityDates().size() > 1) {
				CQSelectFilter dateFilter = new CQSelectFilter();
				dateFilter.setValue(DISABILITY_DATE_TYPE);
				dateFilter
					.setFilter(connector.getFilter(new FilterId(connector.getId(), ConqueryConstants.VALIDITY_DATE_SELECTION_FILTER_NAME)));
				return dateFilter;
			}
			return null;
		}
		
	}
	
	/**
	 * wraps an existing concept configuration within selects for exists and
	 * "AU-Tage"
	 * 
	 * @param column
	 * @return modified column
	 */
	@RequiredArgsConstructor
	private static class SelectSetter implements Consumer<CQConcept> {
		
		private final Namespaces namespaces;

		@Override
		public void accept(CQConcept concept) {
			ConceptId id = concept.getIds().get(0).findConcept();
			Select exists = namespaces.resolve(new ConceptSelectId(id, "icd_exists"));
			Select days = namespaces.resolve(new ConnectorSelectId(new ConnectorId(id, "au_fall"), "sum_au"));
			concept.setSelects(List.of(exists, days));
		}
	}


	
	/**
	 * modifies the given fixed column such that only the disability table is used.
	 * Any existing configuration method is wrapped and called after table removal.
	 * 
	 * @param column
	 * @return altered input column
	 */
	@RequiredArgsConstructor
	private static class DisabilityTableFilter implements Consumer<CQConcept> {
		

		@Override
		public void accept(CQConcept concept) {
			concept.getTables().removeIf(DisabilityTableFilter::isNotDisabilityTable);
		}

		private static boolean isNotDisabilityTable(CQTable table) {
			return !table.getId().getConnector().equalsIgnoreCase("au_fall");
		}
	}
	
	private FixedColumn chainColumnManipulator(FixedColumn column, Consumer<CQConcept> manipulator) {
		Consumer<CQConcept> previousConfigurator = column.getConceptConfiguration();
		column.setConceptConfiguration( concept -> {
			manipulator.accept(concept);
			previousConfigurator.accept(concept);
		});
		return column;
	}

	private List<ConceptElementId<?>> findMiscellaneousConceptIds(Dataset dataset) {
		return dataset
			.getConcepts()
			.stream()
			.filter(concept -> concept.getName().equals("icd"))
			.map(TreeConcept.class::cast)
			.flatMap(concept -> concept.getAllChildren().values().stream())
			.filter(this::isMiscellaneousConcept)
			.map(ConceptElement::getId)
			.collect(Collectors.toList());
	}

	/**
	 *
	 * @param concept
	 *            icd concept
	 * @return true if icd concept is not used in distinct category
	 */
	private boolean isMiscellaneousConcept(ConceptElement<?> conceptElement) {
		ConceptElementId<?> id = conceptElement.getId();
		List<Object> nameParts = new ArrayList<Object>();
		id.collectComponents(nameParts);
		// ignore icd node itself or children on the third level
		if (nameParts.size() == 2 || nameParts.size() > 4)
			return false;
		// check children immediately below icd node
		if (nameParts.size() == 3) {
			return isAllowedFirstLevelConcept(Objects.toString(nameParts.get(2)));
		}
		// check children on the second level
		if (nameParts.size() == 4) {
			return isAllowedSecondLevelConcept(Objects.toString(nameParts.get(2)), Objects.toString(nameParts.get(3)));
		}
		return false;
	}

	private boolean isAllowedSecondLevelConcept(String firstLevelName, String secondLevelName) {
		return firstLevelName.equals("a00-b99") && !secondLevelName.equals("a00-a09");
	}

	private boolean isAllowedFirstLevelConcept(String firstLevelName) {

		return !DISALLOWED_FIRST_LEVEL_CONCEPTS.contains(firstLevelName);
	}

	private CQOr createMiscellaneousConcept(Namespaces namespaces, List<ConceptElementId<?>> miscellaneousConceptNames) {
		CQConcept concept = new CQConcept();
		concept.setLabel(MISCELLANEOUS_LABEL);
		concept.setIds(miscellaneousConceptNames);

		CQTable t = new CQTable();
		t.setId(new ConnectorId(new ConceptId(miscellaneousConceptNames.get(0).getDataset(), "icd"), "au_fall"));
		t.setConcept(concept);

		List<FilterValue<?>> filters = new ArrayList<>();
		filters.add(DefaultDateSelection.createDateSelectionFilter(namespaces.resolve(t.getId())));
		t.setFilters(filters);
		concept.setTables(Arrays.asList(t));
		new SelectSetter(namespaces).accept(concept);

		CQOr group = new CQOr();
		group.setChildren(Arrays.asList(concept));

		return group;
	}
}