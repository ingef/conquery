package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.ExternalId;
import com.bakdata.conquery.util.DateReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityResolverUtil {

	public static final String FORMAT_EXTRA = "EXTRA";

	/**
	 * For each row try and collect all dates.
	 *
	 * @return Row -> Dates
	 */
	public static CDateSet[] readDates(String[][] values, List<String> format, DateReader dateReader) {
		final CDateSet[] out = new CDateSet[values.length];

		final List<DateFormat> dateFormats = format.stream()
												   .map(EntityResolverUtil::resolveDateFormat)
												   // Don't use Stream#toList to preserve null-values
												   .collect(Collectors.toList());

		/*
		 If no format is provided, put empty dates into output.
		 This indicates that no date context was provided and
		 the entries are not restricted by any date restriction,
		 but can also don't contribute to any date aggregation.
		 */
		if (dateFormats.stream().allMatch(Objects::isNull)) {
			// Initialize empty
			for (int row = 0; row < values.length; row++) {
				out[row] = CDateSet.createEmpty();
			}
			return out;
		}

		for (int row = 1; row < values.length; row++) {
			try {
				final CDateSet dates = CDateSet.createEmpty();

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
					out[row] = CDateSet.createEmpty();
				}

				out[row].addAll(dates);
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
			}
		}

		return out;
	}

	public static void collectExtraData(Map<String, String>[] extraDataByRow, int rowNum, Map<String, Map<String, List<String>>> extraDataByEntity, String resolvedId) {
		if (extraDataByRow[rowNum] != null) {
			for (Map.Entry<String, String> entry : extraDataByRow[rowNum].entrySet()) {
				extraDataByEntity.computeIfAbsent(resolvedId, (ignored) -> new HashMap<>())
								 .computeIfAbsent(entry.getKey(), (ignored) -> new ArrayList<>())
								 .add(entry.getValue());
			}
		}
	}

	public static void verifyOnlySingles(boolean onlySingles, Map<String, Map<String, List<String>>> extraDataByEntity) {
		if (!onlySingles) {
			return;
		}
		// Check that there is at most one value per entity and per column
		final boolean alright = extraDataByEntity.values().stream()
												 .map(Map::values)
												 .flatMap(Collection::stream)
												 .allMatch(l -> l.size() <= 1);
		if (!alright) {
			throw new ConqueryError.ExternalResolveOnePerRowError();
		}
	}

	/**
	 * Try to extract a {@link ExternalId} from the row,
	 * then try to map it to an internal {@link com.bakdata.conquery.models.query.entity.Entity}
	 */
	public static String tryResolveId(String[] row, List<Function<String[], ExternalId>> readers, EntityIdMap mapping) {
		String resolvedId = null;

		for (Function<String[], ExternalId> reader : readers) {
			final ExternalId externalId = reader.apply(row);

			if (externalId == null) {
				continue;
			}

			String innerResolved = mapping.resolve(externalId);

			if (innerResolved == null) {
				continue;
			}

			// Only if all resolvable ids agree on the same entity, do we return the id.
			if (resolvedId != null && !innerResolved.equals(resolvedId)) {
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
	public static Map<String, String>[] readExtras(String[][] values, List<String> format) {
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

	/**
	 * Try to resolve a date format, return nothing if not possible.
	 */
	private static DateFormat resolveDateFormat(String name) {
		try {
			return DateFormat.valueOf(name);
		}
		catch (IllegalArgumentException e) {
			return null; // Does not exist
		}
	}

}
