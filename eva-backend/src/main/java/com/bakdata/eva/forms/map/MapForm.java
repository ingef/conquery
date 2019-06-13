package com.bakdata.eva.forms.map;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQMultiSelectFilter;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
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
@CPSType(id = "MAP_FORM", base = Form.class)
public class MapForm extends StatisticForm {

	@NotNull
	@Valid
	private Range<LocalDate> dateRange;
	@NotNull
	private Granularity granularity;
	@NotNull
	private Region region;
	@NotNull
	private DateContextMode resolution;
	@NotNull
	@Valid
	private ManagedExecutionId queryGroup;
	@NotNull
	private boolean relative;
	@NotNull
	private List<CQOr> features = new ArrayList<>();
	@JsonIgnore
	private String selectedConceptLabel = "";
	@JsonIgnore
	private CQOr outcomeGroup;
	@JsonIgnore
	private String outcomeColumnName;
	@JsonIgnore
	private String granularityColumnName;

	@Getter
	@RequiredArgsConstructor
	public enum Granularity {
		FEDERAL_STATE("bundesland", "bundesland", "bundesland"), DISTRICT("kgs", "kgs", "kreise"), ZIP_CODE("plz", "regionale_daten", "plz"), KV("kv", "wohnort-kv", "wohnort-kv");

		private final String conceptName;
		private final String connectorName;
		private final String filterName;
	}

	@Getter
	@RequiredArgsConstructor
	public enum Region {
		DEUTSCHLAND("DE"), SCHLESWIG_HOLSTEIN("01"), HAMBURG("02"), NIEDERSACHSEN("03"), BREMEN("04"), NORDRHEIN_WESTFALEN("05"), HESSEN("06"), RHEINLAND_PFALZ("07"), BADEN_WUERTTEMBERG("08"), BAYERN("09"), SAARLAND("10"), BERLIN("11"), BRANDENBURG("12"), MECKLENBURG_VORPOMMERN("13"), SACHSEN("14"), SACHSEN_ANHALT("15"), THUERINGEN("16");

		private final String regionCode;
	}

	@JsonIgnore
	private BiConsumer<ColumnDescriptor, CQOr> columnManipulator = (cd, or) -> {
		if (or.equals(outcomeGroup)) {
			outcomeColumnName = cd.getColumn();
		}
		CQElement elem = or.getChildren().get(0);
		if (elem instanceof CQConcept) {
			if (((CQConcept) elem).getLabel().equals(granularity.getConceptName())) {
				granularityColumnName = cd.getColumn();
			}

		}
	};

	@Override
	public void init(Namespaces namespaces, User user) {
		List<FixedColumn> columns = createDefaultColumns(namespaces, queryGroup.getDataset().getName(), granularity, region);
		this.setFixedFeatures(columns.toArray(new FixedColumn[columns.size()]));
		outcomeGroup = features.get(0);
		selectedConceptLabel = extractSelectedConceptLabel();
		super.init(namespaces, user);
	}

	@Override
	public ManagedQuery executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		AbsExportForm form = new AbsExportForm();
		form.setColumns(new ArrayList<>(getColumns()));
		form.setDateRange(getDateRange());
		form.setFeatures(getFeatures());
		form.setFixedFeatures(getFixedFeatures());
		form.setQueryGroup(this.getQueryGroup());

		return new AbsExportGenerator(dataset, user, namespaces).executeQuery(form, resolution, false);
	}

	@Override
	protected JsonNode toStatisticView() {
		return Jackson.MAPPER.valueToTree(MapStatisticsAPI.from(this));
	}

	@Override
	protected String[] getAdditionalHeader() {
		return new String[] { "quarter", "date_range" };
	}

	@Override
	public List<ManagedExecutionId> getUsedQueries() {
		return Collections.singletonList(queryGroup);
	}

	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Collections.singletonList(new FeatureGroupDescription.GroupFD(features, FeatureGroup.SINGLE_GROUP, columnManipulator));
	}

	private static List<FixedColumn> createDefaultColumns(Namespaces namespaces, String datasetName, Granularity granularity, Region region) {
		List<FixedColumn> columns = new ArrayList<>();
		switch (granularity) {
			case ZIP_CODE:
				columns.add(FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, Granularity.ZIP_CODE.getConceptName()));
				break;
			case DISTRICT:
				columns.add(FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, Granularity.DISTRICT.getConceptName()));
				break;
			case KV:
				columns.add(FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, Granularity.KV.getConceptName()));
				break;
			default:
				break;
		}
		columns.add(createFederalStateColumn(namespaces, datasetName, region));
		columns.add(FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "geschlecht"));
		columns.add(FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, "alter"));

		return columns;
	}

	private static FixedColumn createFederalStateColumn(Namespaces namespaces, String datasetName, Region region) {
		FixedColumn fc = FixedColumn.of(FeatureGroup.SINGLE_GROUP, datasetName, Granularity.FEDERAL_STATE.getConceptName());
		if (region == Region.DEUTSCHLAND)
			return fc;
		// find persons from selected region only
		fc.setConceptConfiguration(concept -> concept.getTables().stream().forEach(table -> {
			CQMultiSelectFilter f = new CQMultiSelectFilter();
			Filter<?> filter = namespaces
				.resolve(
					FilterId.Parser.INSTANCE
						.parse(
							IId.JOINER
								.join(
									datasetName,
									Granularity.FEDERAL_STATE.getConceptName(),
									Granularity.FEDERAL_STATE.getConnectorName(),
									Granularity.FEDERAL_STATE.getFilterName())));
			f.setFilter(filter);
			f.setValue(new String[] { region.getRegionCode() });
			table.setFilters(Collections.singletonList(f));
		}));
		return fc;
	}

	private String extractSelectedConceptLabel() {
		return features
			.stream()
			.flatMap(group -> group.getChildren().stream())
			.map(element -> (CQConcept) element)
			.map(CQConcept::getLabel)
			.collect(Collectors.joining("_"));
	}
}
