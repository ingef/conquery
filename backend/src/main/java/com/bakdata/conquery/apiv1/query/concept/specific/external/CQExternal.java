package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.ExternalNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows uploading lists of entities.
 */
@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
public class CQExternal extends CQElement {

	private static final String FORMAT_EXTRA = "EXTRA";

	/**
	 * Describes the format of {@code values}, how to extract data from each row:
	 * <p>
	 * - Must contain at least one of {@link FrontendConfig.UploadConfig#getIds()}.
	 * - May contain names of {@link DateFormat}s.
	 * - Lines filled with {@code FORMAT_EXTRA} are added as extra data to output.
	 *
	 * @implSpec Every name we do not know is implicitly ignored.
	 */
	@Getter
	@NotEmpty
	private final List<String> format;

	@Getter
	@NotEmpty
	private final String[][] values;

	/**
	 * Maps from Entity to the computed time-frame.
	 */
	@Getter
	@InternalOnly
	private Map<Integer, CDateSet> valuesResolved;

	/**
	 * Contains the uploaded additional data for each column for each entity.
	 * <p>
	 * Column -> Entity -> Value(s)
	 *
	 * @implNote FK: I would prefer to implement this as a guava table, but they cannot be deserialized with Jackson so we implement the Table manually.
	 */
	@InternalOnly
	@Getter
	private Map<Integer, Map<String, List<String>>> extra;

	public CQExternal(@NotEmpty List<String> format, @NotEmpty String[][] values) {
		this.format = format;
		this.values = values;
	}


	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		if (valuesResolved == null) {
			throw new IllegalStateException("CQExternal needs to be resolved before creating a plan");
		}

		// Allocate at once, the maximum possible size
		final Map<String, ConstantValueAggregator<List<String>>> extraAggregators = new HashMap<>(format.size());

		if (extra != null) {
			for (int col = 0; col < format.size(); col++) {
				if (!format.get(col).equals(FORMAT_EXTRA)) {
					continue;
				}

				final String column = values[0][col];

				final ConstantValueAggregator aggregator = new ConstantValueAggregator(null, new ResultType.ListT(ResultType.StringT.INSTANCE));
				plan.registerAggregator(aggregator);

				if(extraAggregators.put(column, aggregator) != null){
					log.error("Multiple Aggregators for same Column");
				}
			}
		}

		return new ExternalNode(context.getStorage().getDataset().getAllIdsTable(), valuesResolved, extra, extraAggregators);
	}

	/**
	 * For each row try and collect all dates.
	 *
	 * @return Row -> Dates
	 */
	private static CDateSet[] readDates(String[][] values, List<String> format, DateReader dateReader, FrontendConfig.UploadConfig queryUpload) {
		final CDateSet[] out = new CDateSet[values.length];

		List<DateFormat> dateFormats = format.stream().map(queryUpload::resolveDateFormat).collect(Collectors.toList());


		// If no format provided, put empty dates into output.
		if (dateFormats.stream().allMatch(Objects::isNull)) {
			// Initialize empty
			for (int row = 0; row < values.length; row++) {
				out[row] = CDateSet.create();
			}
			return out;
		}

		for (int row = 1; row < values.length; row++) {
			try {
				final CDateSet dates = CDateSet.create();

				// Collect all specified dates into a single set.
				for (int col = 0; col < dateFormats.size(); col++) {
					final DateFormat dateFormat = dateFormats.get(col);

					if (dateFormat == null) {
						continue;
					}
					dateFormat.readDates(values[row][col], dateReader, dates);
				}

				if (dates.isEmpty()) {
					continue;
				}

				if (out[row] == null) {
					out[row] = CDateSet.create();
				}

				out[row].addAll(dates);
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
			}
		}

		return out;
	}

	@Override
	public void resolve(QueryResolveContext context) {
		final ResolveStatistic resolved =
				resolveEntities(values, format,
								context.getNamespace().getStorage().getIdMapping(),
								context.getConfig().getFrontend().getQueryUpload(),
								context.getConfig().getLocale().getDateReader()
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
		private final Map<Integer, CDateSet> resolved;

		/**
		 * Entity -> Column -> Values
		 */
		@JsonIgnore
		private final Map<Integer, Map<String, List<String>>> extra;

		private final List<String[]> unreadableDate;
		private final List<String[]> unresolvedId;

	}

	/**
	 * Helper method to try and resolve entities in values using the specified format.
	 */
	public static ResolveStatistic resolveEntities(@NotEmpty String[][] values, @NotEmpty List<String> format, EntityIdMap mapping, FrontendConfig.UploadConfig queryUpload, @NotNull DateReader dateReader) {
		final Map<Integer, CDateSet> resolved = new Int2ObjectOpenHashMap<>();

		final List<String[]> unresolvedDate = new ArrayList<>();
		final List<String[]> unresolvedId = new ArrayList<>();

		// extract dates from rows
		final CDateSet[] rowDates = readDates(values, format, dateReader, queryUpload);

		// Extract extra data from rows by Row, to be collected into by entities
		// Row -> Column -> Value
		final Map<String, String>[] extraDataByRow = readExtras(values, format);

		final List<Function<String[], EntityIdMap.ExternalId>> readers = queryUpload.getIdReaders(format);

		// We will not be able to resolve anything...
		if (readers.isEmpty()) {
			return new ResolveStatistic(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), List.of(values));
		}

		// Entity -> Column -> Values
		final Map<Integer, Map<String, List<String>>> extraDataByEntity = new HashMap<>();

		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {

			final String[] row = values[rowNum];

			if (rowDates[rowNum] == null) {
				unresolvedDate.add(row);
				continue;
			}

			int resolvedId = tryResolveId(row, readers, mapping);

			if (resolvedId == -1) {
				unresolvedId.add(row);
				continue;
			}

			//read the dates from the row
			resolved.put(resolvedId, rowDates[rowNum]);

			// Entity was resolved for row so we collect the data.
			if (extraDataByRow[rowNum] != null) {

				for (Map.Entry<String, String> entry : extraDataByRow[rowNum].entrySet()) {
					extraDataByEntity.computeIfAbsent(resolvedId, (ignored) -> new HashMap<>())
									 .computeIfAbsent(entry.getKey(), (ignored) -> new ArrayList<>())
									 .add(entry.getValue());
				}
			}
		}

		return new ResolveStatistic(resolved, extraDataByEntity, unresolvedDate, unresolvedId);
	}

	/**
	 * Try to extract a {@link com.bakdata.conquery.models.identifiable.mapping.EntityIdMap.ExternalId} from the row,
	 * then try to map it to an internal {@link com.bakdata.conquery.models.query.entity.Entity}
	 */
	private static int tryResolveId(String[] row, List<Function<String[], EntityIdMap.ExternalId>> readers, EntityIdMap mapping) {
		int resolvedId = -1;

		for (Function<String[], EntityIdMap.ExternalId> reader : readers) {
			final EntityIdMap.ExternalId externalId = reader.apply(row);

			if (externalId == null) {
				continue;
			}

			int innerResolved = mapping.resolve(externalId);

			if (innerResolved == -1) {
				continue;
			}

			// Only if all resolvable ids agree on the same entity, do we return the id.
			if (resolvedId != -1 && innerResolved != resolvedId) {
				log.error("`{}` maps to different Entities", (Object) row);
				continue;
			}

			resolvedId = innerResolved;
		}
		return resolvedId;
	}

	/**
	 * Try and extract Extra data from input to be returned as extra-data in output.
	 * <p>
	 * Line -> ( Column -> Value )
	 */
	private static Map<String, String>[] readExtras(String[][] values, List<String> format) {
		final String[] names = values[0];
		final Map<String, String>[] extrasByRow = new Map[values.length];


		for (int line = 1; line < values.length; line++) {
			for (int col = 0; col < format.size(); col++) {
				if (!format.get(col).equals(FORMAT_EXTRA)) {
					continue;
				}


				if (extrasByRow[line] == null) {
					extrasByRow[line] = new HashMap<>(names.length);
				}

				extrasByRow[line].put(names[col], values[line][col]);
			}
		}


		return extrasByRow;
	}


	@Override
	public void collectResultInfos(List<ResultInfo> collector) {
		if (extra == null) {
			return;
		}
		for (int col = 0; col < format.size(); col++) {
			if (!format.get(col).equals(FORMAT_EXTRA)) {
				continue;
			}

			String column = values[0][col];

			collector.add(new SimpleResultInfo(column, new ResultType.ListT(ResultType.StringT.INSTANCE)));
		}
	}


	@JsonIgnore
	@ValidationMethod(message = "Values and Format are not of same width.")
	public boolean isAllSameLength() {
		final int expected = format.size();
		return Arrays.stream(values).mapToInt(a -> a.length).allMatch(v -> expected == v);
	}
}
