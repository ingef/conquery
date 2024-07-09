package com.bakdata.conquery.apiv1.query.concept.specific.external;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Streams;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows uploading lists of entities.
 */
@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
@NoArgsConstructor
public class CQExternal extends CQElement {

	public static final String FORMAT_EXTRA = "EXTRA";

	/**
	 * Describes the format of {@code values}, how to extract data from each row:
	 * <p>
	 * - Must contain at least one of {@link IdColumnConfig#getIds()}.
	 * - May contain names of {@link DateFormat}s.
	 * - Lines filled with {@code FORMAT_EXTRA} are added as extra data to output.
	 *
	 * @implSpec Every name we do not know is implicitly ignored.
	 */
	@Getter(AccessLevel.PUBLIC)
	@NotEmpty
	private List<String> format;

	@Getter(AccessLevel.PUBLIC)
	@NotEmpty
	private String[][] values;

	@Getter(AccessLevel.PUBLIC)
	private boolean onlySingles = false;

	/**
	 * Maps from Entity to the computed time-frame.
	 */
	@Getter
	@JsonView(View.InternalCommunication.class)
	private Map<String, CDateSet> valuesResolved;

	@Getter(AccessLevel.PRIVATE)
	@JsonView(View.InternalCommunication.class)
	private String[] headers;

	/**
	 * Contains the uploaded additional data for each column for each entity.
	 * <p>
	 * Column -> Entity -> Value(s)
	 *
	 * @implNote FK: I would prefer to implement this as a guava table, but they cannot be deserialized with Jackson so we implement the Table manually.
	 */
	@Getter(AccessLevel.PRIVATE)
	@JsonView(View.InternalCommunication.class)
	private Map<String, Map<String, List<String>>> extra;

	public CQExternal(List<String> format, @NotEmpty String[][] values, boolean onlySingles) {
		this.format = format;
		this.values = values;
		this.onlySingles = onlySingles;
	}

	public boolean containsDates() {
		return format.stream().anyMatch(DateFormat.NAMES::contains);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		if (valuesResolved == null) {
			throw new IllegalStateException("CQExternal needs to be resolved before creating a plan");
		}

		final String[] extraHeaders = Streams.zip(
													 Arrays.stream(headers),
													 format.stream(),
													 (header, format) -> format.equals(FORMAT_EXTRA) ? header : null
											 )
											 .filter(Objects::nonNull)
											 .toArray(String[]::new);

		if (onlySingles) {
			return createExternalNodeOnlySingle(context, plan, extraHeaders);
		}
		return createExternalNodeForList(context, plan, extraHeaders);

	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {

	}

	private ExternalNode<String> createExternalNodeOnlySingle(QueryPlanContext context, ConceptQueryPlan plan, String[] extraHeaders) {
		// Remove zero element Lists and substitute one element Lists by containing String
		final Map<String, Map<String, String>> extraFlat = extra.entrySet().stream()
																.collect(Collectors.toMap(
																		Map.Entry::getKey,
																		entityToRowMap -> entityToRowMap.getValue().entrySet().stream()
																										.filter(headerToValue -> !headerToValue.getValue()
																																			   .isEmpty())
																										.collect(Collectors.toMap(
																												Map.Entry::getKey,
																												headerToValue -> headerToValue.getValue()
																																			  .get(0)
																										))
																));

		final Map<String, ConstantValueAggregator<String>> extraAggregators = new HashMap<>(extraHeaders.length);
		for (String extraHeader : extraHeaders) {
			// Just allocating, the result type is irrelevant here
			final ConstantValueAggregator<String> aggregator = new ConstantValueAggregator<>(null, null);
			extraAggregators.put(extraHeader, aggregator);
			plan.registerAggregator(aggregator);

		}

		return new ExternalNode<>(context.getStorage().getDataset().getAllIdsTable(), valuesResolved, extraFlat, extraHeaders, extraAggregators);
	}

	private ExternalNode<List<String>> createExternalNodeForList(QueryPlanContext context, ConceptQueryPlan plan, String[] extraHeaders) {
		final Map<String, ConstantValueAggregator<List<String>>> extraAggregators = new HashMap<>(extraHeaders.length);
		for (String extraHeader : extraHeaders) {
			// Just allocating, the result type is irrelevant here
			final ConstantValueAggregator<List<String>> aggregator = new ConstantValueAggregator<>(null, null);
			extraAggregators.put(extraHeader, aggregator);
			plan.registerAggregator(aggregator);
		}

		return new ExternalNode<>(
				context.getStorage().getDataset().getAllIdsTable(),
				valuesResolved,
				extra,
				extraHeaders,
				extraAggregators
		);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		headers = values[0];

		final ResolveStatistic resolved =
				context.getNamespace().resolveEntities(values, format,
													   context.getNamespace().getStorage().getIdMapping(),
													   context.getConfig().getIdColumns(),
													   context.getConfig().getLocale().getDateReader(),
													   onlySingles
				);

		if (resolved.getResolved().isEmpty()) {
			throw new ConqueryError.ExternalResolveEmptyError();
		}

		if (!resolved.getUnreadableDate().isEmpty()) {
			log.warn(
					"Could not read {} dates. Not resolved: {}",
					resolved.getUnreadableDate().size(),
					resolved.getUnreadableDate().subList(0, Math.min(resolved.getUnreadableDate().size(), 10))
			);
		}

		if (!resolved.getUnresolvedId().isEmpty()) {
			log.warn(
					"Could not resolve {} ids. Not resolved: {}",
					resolved.getUnresolvedId().size(),
					resolved.getUnresolvedId().subList(0, Math.min(resolved.getUnresolvedId().size(), 10))
			);
		}

		valuesResolved = resolved.getResolved();
		extra = resolved.getExtra();
	}

	@Data
	public static class ResolveStatistic {

		@JsonIgnore
		private final Map<String, CDateSet> resolved;

		/**
		 * Entity -> Column -> Values
		 */
		@JsonIgnore
		private final Map<String, Map<String, List<String>>> extra;

		private final List<String[]> unreadableDate;
		private final List<String[]> unresolvedId;

	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return new RequiredEntities(valuesResolved.keySet());
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		if (extra == null) {
			return Collections.emptyList();
		}
		List<ResultInfo> resultInfos = new ArrayList<>();
		for (int col = 0; col < format.size(); col++) {
			if (!format.get(col).equals(FORMAT_EXTRA)) {
				continue;
			}

			String column = headers[col];

			resultInfos.add(new SimpleResultInfo(column, onlySingles ?
														 ResultType.StringT.INSTANCE :
														 new ResultType.ListT(ResultType.StringT.INSTANCE)));
		}

		return resultInfos;
	}

	@JsonIgnore
	@ValidationMethod(message = "Values and Format are not of same width.")
	public boolean isAllSameLength() {
		final int expected = format.size();
		return Arrays.stream(values).mapToInt(a -> a.length).allMatch(v -> expected == v);
	}

	@JsonIgnore
	@ValidationMethod(message = "Headers are not unique")
	public boolean isHeadersUnique() {
		Set<String> uniqueNames = new HashSet<>();
		Set<String> duplicates = new HashSet<>();

		for (String header : values[0]) {
			if (!uniqueNames.add(header)) {
				duplicates.add(header);
			}
		}

		if (duplicates.isEmpty()) {
			return true;
		}

		log.error("Duplicate Headers {}", duplicates);

		return false;
	}

}
