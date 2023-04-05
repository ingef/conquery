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

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.collect.MoreCollectors;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated {@link ManagedExecution} to properly display/combine the two Queries submitted by {@link EntityPreviewForm}.
 * This mostly delegates to {@link EntityPreviewForm#VALUES_QUERY_NAME}, but embeds the result of {@link EntityPreviewForm#INFOS_QUERY_NAME} into {@link EntityPreviewStatus#getInfos()}.
 */
@CPSType(id = "ENTITY_PREVIEW_EXECUTION", base = ManagedExecution.class)
@ToString
public class EntityPreviewExecution extends ManagedInternalForm<EntityPreviewForm> {

	@ToString.Exclude
	private PreviewConfig previewConfig;

	protected EntityPreviewExecution(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public EntityPreviewExecution(EntityPreviewForm entityPreviewQuery, User user, Dataset submittedDataset, MetaStorage storage) {
		super(entityPreviewQuery, user, submittedDataset, storage);
	}

	@Override
	public boolean isSystem() {
		// This Form should NEVER be started manually. Nor persisted
		return true;
	}

	@Override
	public void doInitExecutable() {
		super.doInitExecutable();
		previewConfig = getNamespace().getPreviewConfig();
	}

	/**
	 * Collects status of {@link EntityPreviewForm#getValuesQuery()} and {@link EntityPreviewForm#getInfoCardQuery()}.
	 * <p>
	 * Most importantly to {@link EntityPreviewStatus#setInfos(List)} to for infos of entity.
	 */
	@Override
	public FullExecutionStatus buildStatusFull(Subject subject) {

		initExecutable(getNamespace(), getConfig());

		final EntityPreviewStatus status = new EntityPreviewStatus();
		setStatusFull(status, subject);
		status.setQuery(getValuesQuery().getQuery());

		final PrintSettings printSettings = new PrintSettings(true, I18n.LOCALE.get(), getNamespace(), getConfig(), null, previewConfig::resolveSelectLabel);

		status.setInfos(transformQueryResultToInfos(getInfoCardExecution(), printSettings));

		status.setTimeStratifiedInfos(toChronoInfos(previewConfig, getSubQueries(), printSettings));

		return status;
	}

	@JsonIgnore
	private ManagedQuery getValuesQuery() {
		return getSubQueries().get(EntityPreviewForm.VALUES_QUERY_NAME);
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
		return getSubQueries().get(EntityPreviewForm.INFOS_QUERY_NAME);
	}

	@NotNull
	private List<EntityPreviewStatus.TimeStratifiedInfos> toChronoInfos(PreviewConfig previewConfig, Map<String, ManagedQuery> subQueries, PrintSettings printSettings) {
		final List<EntityPreviewStatus.TimeStratifiedInfos> timeStratifiedInfos = new ArrayList<>();

		for (PreviewConfig.TimeStratifiedSelects description : previewConfig.getTimeStratifiedSelects()) {
			final ManagedQuery query = subQueries.get(description.label());

			final EntityResult entityResult = query.streamResults().collect(MoreCollectors.onlyElement());

			final Map<SelectId, PreviewConfig.InfoCardSelect> select2desc =
					description.selects().stream()
							   .collect(Collectors.toMap(PreviewConfig.InfoCardSelect::select, Function.identity()));

			// Group lines by year and quarter.
			final Function<Object[], Map<String, Object>> lineTransformer = createLineToMapTransformer(query.getResultInfos(), select2desc, printSettings);
			final List<EntityPreviewStatus.YearEntry> yearEntries = createYearEntries(entityResult, lineTransformer);

			final Object[] completeResult = getCompleteLine(entityResult);

			// get descriptions, but drop everything that isn't a select result as the rest is already structured
			final List<ColumnDescriptor> columnDescriptors = createChronoColumnDescriptors(query, select2desc);


			final EntityPreviewStatus.TimeStratifiedInfos infos = new EntityPreviewStatus.TimeStratifiedInfos(description.label(), description.description(), columnDescriptors, lineTransformer.apply(completeResult), yearEntries);

			timeStratifiedInfos.add(infos);
		}

		return timeStratifiedInfos;
	}

	@NotNull
	private List<EntityPreviewStatus.YearEntry> createYearEntries(EntityResult entityResult, Function<Object[], Map<String, Object>> lineTransformer) {
		final Map<Integer, Object[]> yearLines = getYearLines(entityResult);
		final Map<Integer, Map<Integer, Object[]>> quarterLines = getQuarterLines(entityResult);

		final List<EntityPreviewStatus.YearEntry> yearEntries = new ArrayList<>();

		yearLines.forEach((year, yearLine) -> {

			final List<EntityPreviewStatus.QuarterEntry> quarterEntries = new ArrayList<>();
			quarterLines.getOrDefault(year, Collections.emptyMap())
						.forEach((quarter, line) -> quarterEntries.add(new EntityPreviewStatus.QuarterEntry(quarter, lineTransformer.apply(line))));

			quarterEntries.sort(Comparator.comparingInt(EntityPreviewStatus.QuarterEntry::quarter));

			yearEntries.add(new EntityPreviewStatus.YearEntry(year, lineTransformer.apply(yearLine), quarterEntries));
		});

		yearEntries.sort(Comparator.comparingInt(EntityPreviewStatus.YearEntry::year));

		return yearEntries;
	}

	private Object[] getCompleteLine(EntityResult entityResult) {
		for (Object[] line : entityResult.listResultLines()) {

			// Since we know the dates are always aligned we need to only respect their starts.
			final LocalDate date = CDate.toLocalDate(((List<Integer>) line[AbsoluteFormQuery.TIME_INDEX]).get(0));

			final int year = date.getYear();
			final int quarter = QuarterUtils.getQuarter(date);

			if (Resolution.valueOf((String) line[AbsoluteFormQuery.RESOLUTION_INDEX]) == Resolution.COMPLETE) {
				return line;
			}
		}

		throw new IllegalStateException("Result has no row for COMPLETE");
	}

	/**
	 * Query contains both YEARS and QUARTERS lines: Group them.
	 *
	 * @return
	 */
	private static Map<Integer, Map<Integer, Object[]>> getQuarterLines(EntityResult entityResult) {
		final Map<Integer, Map<Integer, Object[]>> quarterLines = new HashMap<>();

		for (Object[] line : entityResult.listResultLines()) {
			if (Resolution.valueOf((String) line[AbsoluteFormQuery.RESOLUTION_INDEX]) != Resolution.QUARTERS) {
				continue;
			}

			// Since we know the dates are always aligned we need to only respect their starts.
			final LocalDate date = CDate.toLocalDate(((List<Integer>) line[AbsoluteFormQuery.TIME_INDEX]).get(0));

			final int year = date.getYear();
			final int quarter = QuarterUtils.getQuarter(date);

			quarterLines.computeIfAbsent(year, (ignored) -> new HashMap<>(4)).put(quarter, line);
		}

		return quarterLines;
	}

	/**
	 * Query contains both YEARS and QUARTERS lines: Group them.
	 *
	 * @return
	 */
	private static Map<Integer, Object[]> getYearLines(EntityResult entityResult) {

		final Map<Integer, Object[]> yearLines = new HashMap<>();

		for (Object[] line : entityResult.listResultLines()) {

			if (Resolution.valueOf((String) line[AbsoluteFormQuery.RESOLUTION_INDEX]) != Resolution.YEARS) {
				continue;
			}

			// Since we know the dates are always aligned we need to only respect their starts.
			final LocalDate date = CDate.toLocalDate(((List<Integer>) line[AbsoluteFormQuery.TIME_INDEX]).get(0));

			final int year = date.getYear();

			yearLines.put(year, line);
		}

		return yearLines;
	}

	/**
	 * Creates a transformer printing lines, transformed into a Map of label->value.
	 * Null values are omitted.
	 */
	private static Function<Object[], Map<String, Object>> createLineToMapTransformer(List<ResultInfo> resultInfos, Map<SelectId, PreviewConfig.InfoCardSelect> select2desc, PrintSettings printSettings) {


		final int size = resultInfos.size();
		final String[] columnNames = new String[size];

		for (int index = 0; index < size; index++) {
			final ResultInfo resultInfo = resultInfos.get(index);

			if (resultInfo instanceof SelectResultInfo selectResultInfo) {
				columnNames[index] = select2desc.get(selectResultInfo.getSelect().getId()).label();
			}
		}

		return line -> {
			final Map<String, Object> out = new HashMap<>(size);

			for (int column = 0; column < size; column++) {
				final String columnName = columnNames[column];

				if (columnName == null) {
					continue;
				}

				final String value = resultInfos.get(column).getType().printNullable(printSettings, line[column]);

				if (Strings.isBlank(value)) {
					continue;
				}

				out.put(columnName, value);
			}

			return out;
		};
	}

	/**
	 * For the selects in result infos, build ColumnDescriptors using definitions (label and description) from PreviewConfig.
	 */
	private static List<ColumnDescriptor> createChronoColumnDescriptors(SingleTableResult query, Map<SelectId, PreviewConfig.InfoCardSelect> select2desc) {

		final List<ColumnDescriptor> columnDescriptions = new ArrayList<>();

		for (ResultInfo info : query.getResultInfos()) {
			if (info instanceof SelectResultInfo selectResultInfo) {
				final PreviewConfig.InfoCardSelect desc = select2desc.get(selectResultInfo.getSelect().getId());

				// We build these by hand because they are labeled and described by config.
				columnDescriptions.add(ColumnDescriptor.builder()
													   .label(desc.label())
													   .defaultLabel(desc.label())
													   .type(info.getType().typeInfo())
													   .semantics(info.getSemantics())
													   .description(desc.description())
													   .build());
			}
		}


		return columnDescriptions;
	}

	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		status.setColumnDescriptions(generateColumnDescriptions());
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions() {
		final List<ColumnDescriptor> descriptors = getValuesQuery().generateColumnDescriptions();

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
	protected void setAdditionalFieldsForStatusWithSource(Subject subject, FullExecutionStatus status) {
		status.setColumnDescriptions(generateColumnDescriptions());
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), getFlatSubQueries().entrySet()
														   .stream()
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
