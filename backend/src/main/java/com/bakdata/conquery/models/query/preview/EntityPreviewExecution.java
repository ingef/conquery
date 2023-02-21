package com.bakdata.conquery.models.query.preview;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated {@link ManagedExecution} to properly display/combine the two Queries submitted by {@link EntityPreviewForm}.
 * This mostly delegates to {@link EntityPreviewForm#VALUES_QUERY_NAME}, but embeds the result of {@link EntityPreviewForm#INFOS_QUERY_NAME} into {@link EntityPreviewStatus#getInfos()}.
 */
@CPSType(id = "ENTITY_PREVIEW_EXECUTION", base = ManagedExecution.class)
public class EntityPreviewExecution extends ManagedForm implements SingleTableResult {

	private PreviewConfig previewConfig;

	EntityPreviewExecution(EntityPreviewForm entityPreviewQuery, User user, Dataset submittedDataset) {
		super(entityPreviewQuery, user, submittedDataset);
	}

	@Override
	public boolean isSystem() {
		// This Form should NEVER be started manually. Nor persisted
		return true;
	}

	@Override
	public void doInitExecutable(@NonNull DatasetRegistry datasetRegistry, ConqueryConfig config) {
		super.doInitExecutable(datasetRegistry, config);
		previewConfig = datasetRegistry.get(getDataset().getId()).getPreviewConfig();
	}

	/**
	 * Collects status of {@link EntityPreviewForm#getValuesQuery()} and {@link EntityPreviewForm#getInfoCardQuery()}.
	 * <p>
	 * Most importantly to {@link EntityPreviewStatus#setInfos(List)} to for infos of entity.
	 */
	@Override
	public FullExecutionStatus buildStatusFull(@NonNull MetaStorage storage, Subject subject, DatasetRegistry datasetRegistry, ConqueryConfig config) {

		initExecutable(datasetRegistry, config);

		final EntityPreviewStatus status = new EntityPreviewStatus();
		setStatusFull(status, storage, subject, datasetRegistry);
		status.setQuery(getValuesQuery().getQuery());

		final PrintSettings printSettings = new PrintSettings(true, I18n.LOCALE.get(), datasetRegistry, config, null, previewConfig::resolveSelectLabel);

		status.setInfos(transformQueryResultToInfos(getInfoCardExecution(), printSettings));

		status.setTimebasedInfos(transformTimeBasedInfos(datasetRegistry, getSubQueries(), printSettings));

		return status;
	}

	@JsonIgnore
	private ManagedQuery getValuesQuery() {
		return getSubQueries().get(EntityPreviewForm.VALUES_QUERY_NAME).get(0);
	}

	/**
	 * Takes a ManagedQuery, and transforms its result into a List of {@link EntityPreviewStatus.Info}.
	 * The format of the query is an {@link AbsoluteFormQuery} containing a single line for one person. This should correspond to {@link EntityPreviewForm#VALUES_QUERY_NAME}.
	 */
	private List<EntityPreviewStatus.Info> transformQueryResultToInfos(ManagedQuery infoCardExecution, PrintSettings printSettings) {


		// Submitted Query is a single line of an AbsoluteFormQuery => MultilineEntityResult with a single line.
		final MultilineEntityResult result = (MultilineEntityResult) infoCardExecution.streamResults().collect(MoreCollectors.onlyElement());
		final Object[] values = result.getValues().get(0);

		final List<EntityPreviewStatus.Info> extraInfos = new ArrayList<>(values.length);

		// We are only interested in the Select results.
		for (int index = AbsoluteFormQuery.FEATURES_OFFSET; index < infoCardExecution.getResultInfos().size(); index++) {
			final ResultInfo resultInfo = infoCardExecution.getResultInfos().get(index);

			final String printed = resultInfo.getType().printNullable(printSettings, values[index]);

			extraInfos.add(new EntityPreviewStatus.Info(
					resultInfo.userColumnName(printSettings),
					printed,
					resultInfo.getType().typeInfo(),
					resultInfo.getDescription(),
					resultInfo.getSemantics()
			));
		}

		return extraInfos;
	}

	@JsonIgnore
	private ManagedQuery getInfoCardExecution() {
		return getSubQueries().get(EntityPreviewForm.INFOS_QUERY_NAME).get(0);
	}

	@NotNull
	private static List<EntityPreviewStatus.TimebasedInfos> transformTimeBasedInfos(DatasetRegistry datasetRegistry, Map<String, List<ManagedQuery>> subQueries, PrintSettings printSettings) {
		final List<EntityPreviewStatus.TimebasedInfos> timebasedInfos = new ArrayList<>();

		for (Map.Entry<String, List<ManagedQuery>> result : subQueries.entrySet()) {

			if (result.getKey().equals(EntityPreviewForm.INFOS_QUERY_NAME)) {
				continue;
			}

			if (result.getKey().equals(EntityPreviewForm.VALUES_QUERY_NAME)) {
				continue;
			}

			final ManagedQuery query = result.getValue().get(0);
			final EntityResult entityResult = query.streamResults().collect(MoreCollectors.onlyElement());

			final int resolutionInfoIdx = 0;
			final int timeIdx = 2;

			// Group lines by quarter, year and resolution.
			final Map<Integer, Object[]> yearLines = new HashMap<>();
			final Map<Integer, Map<Integer, Object[]>> quarterLines = new HashMap<>();

			for (Object[] line : entityResult.listResultLines()) {

				// Since we know the dates are always aligned we need to only respect their starts.
				final LocalDate date = CDate.toLocalDate(((List<Integer>) line[timeIdx]).get(0));

				final int year = date.getYear();
				final int quarter = QuarterUtils.getQuarter(date);

				switch (Resolution.valueOf((String) line[resolutionInfoIdx])) {
					case YEARS -> yearLines.put(year, line);
					case QUARTERS -> quarterLines.computeIfAbsent(year, HashMap::new).put(quarter, line);
					default -> throw new IllegalStateException("Query may only have modes for Quarter and/or Year.");
				}
			}

			final Function<Object[], Map<String, Object>> lineTransformer = createLineTransformer(query.getResultInfos(), printSettings);

			final List<EntityPreviewStatus.TimebasedInfos.YearEntry> yearEntries = new ArrayList<>();

			yearLines.forEach((year, yearLine) -> {

				final List<EntityPreviewStatus.TimebasedInfos.QuarterEntry> quarterEntries = new ArrayList<>();
				quarterLines.getOrDefault(year, Collections.emptyMap())
							.forEach((quarter, line) -> quarterEntries.add(new EntityPreviewStatus.TimebasedInfos.QuarterEntry(quarter, lineTransformer.apply(line))));

				quarterEntries.sort(Comparator.comparingInt(EntityPreviewStatus.TimebasedInfos.QuarterEntry::quarter));


				yearEntries.add(new EntityPreviewStatus.TimebasedInfos.YearEntry(year, lineTransformer.apply(yearLine), quarterEntries));
			});

			yearEntries.sort(Comparator.comparingInt(EntityPreviewStatus.TimebasedInfos.YearEntry::year));

			//TODO probably need to have more than just label (ie descriptions)?
			final EntityPreviewStatus.TimebasedInfos infos =
					new EntityPreviewStatus.TimebasedInfos(result.getKey(), query.generateColumnDescriptions(datasetRegistry), yearEntries);


			timebasedInfos.add(infos);
		}

		return timebasedInfos;
	}

	/**
	 * Creates a transformer printing lines, transformed into a Map of label->value.
	 * Null values are omitted.
	 */
	private static Function<Object[], Map<String, Object>> createLineTransformer(List<ResultInfo> resultInfos, PrintSettings printSettings) {
		final UniqueNamer namer = new UniqueNamer(printSettings);

		final int size = resultInfos.size();
		final String[] columnNames = new String[size];

		for (int index = 0; index < size; index++) {
			final ResultInfo resultInfo = resultInfos.get(index);

			if (!(resultInfo instanceof SelectResultInfo)) {
				continue;
			}

			columnNames[index] = namer.getUniqueName(resultInfo);
		}

		return line -> {
			final Map<String, Object> out = new HashMap<>(size);

			for (int column = 0; column < size; column++) {
				final String columnName = columnNames[column];

				if(columnName == null) {
					continue;
				}

				final Object value = resultInfos.get(column).getType().printNullable(printSettings, line[column]);

				if (value == null) {
					continue;
				}

				out.put(columnName, value);
			}

			return out;
		};
	}

	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(@NonNull MetaStorage storage, Subject subject, FullExecutionStatus status, DatasetRegistry datasetRegistry) {
		status.setColumnDescriptions(generateColumnDescriptions(datasetRegistry));
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry) {
		final List<ColumnDescriptor> descriptors = getValuesQuery().generateColumnDescriptions(datasetRegistry);

		for (ColumnDescriptor descriptor : descriptors) {
			// Add grouping semantics to secondaryIds to group by
			if (descriptor.getSemantics()
						  .stream()
						  .anyMatch(semanticType -> semanticType instanceof SemanticType.SecondaryIdT desc
													&& previewConfig.isGroupingColumn(desc.getSecondaryId()))) {
				descriptor.getSemantics().add(new SemanticType.GroupT());
			}

			// Add hidden semantics to fields flagged for hiding.
			if (descriptor.getSemantics()
						  .stream()
						  .anyMatch(semanticType -> semanticType instanceof SemanticType.ColumnT desc && previewConfig.isHidden(desc.getColumn()))) {
				descriptor.getSemantics().add(new SemanticType.HiddenT());
			}
		}


		return descriptors;
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), getFlatSubQueries().entrySet().stream()
														   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return getValuesQuery().getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return getValuesQuery().streamResults();
	}

	@Override
	public long resultRowCount() {
		return getValuesQuery().resultRowCount();
	}
}
