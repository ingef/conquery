package com.bakdata.conquery.mode.cluster;

import static com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolver;
import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolverUtil;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.ExternalId;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.IdColumnUtil;

public class ClusterEntityResolver implements EntityResolver {

	@Override
	public ResolveStatistic resolveEntities(
			@NotEmpty String[][] values,
			List<String> format,
			EntityIdMap mapping,
			IdColumnConfig idColumnConfig,
			DateReader dateReader,
			boolean onlySingles
	) {
		final Map<String, CDateSet> resolved = new HashMap<>();
		final List<String[]> unresolvedDate = new ArrayList<>();
		final List<String[]> unresolvedId = new ArrayList<>();

		// extract dates from rows
		final CDateSet[] rowDates = readDates(values, format, dateReader);

		// Extract extra data from rows by Row, to be collected into by entities
		// Row -> Column -> Value
		final Map<String, String>[] extraDataByRow = EntityResolverUtil.readExtras(values, format);

		final List<Function<String[], ExternalId>> readers = IdColumnUtil.getIdReaders(format, idColumnConfig.getIdMappers());

		// We will not be able to resolve anything...
		if (readers.isEmpty()) {
			return EntityResolver.ResolveStatistic.forEmptyReaders(values);
		}

		// Entity -> Column -> Values
		final Map<String, Map<String, List<String>>> extraDataByEntity = new HashMap<>();

		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {

			final String[] row = values[rowNum];

			// Try to resolve the id first, because it has higher priority for the uploader than the dates
			String resolvedId = tryResolveId(row, readers, mapping);
			if (resolvedId == null) {
				unresolvedId.add(row);
				continue;
			}

			if (rowDates[rowNum] == null) {
				unresolvedDate.add(row);
				continue;
			}

			// read the dates from the row
			resolved.put(resolvedId, rowDates[rowNum]);

			// Entity was resolved for row, so we collect the data.
			collectExtraData(extraDataByRow, rowNum, extraDataByEntity, resolvedId);
		}

		verifyOnlySingles(onlySingles, extraDataByEntity);
		return new EntityResolver.ResolveStatistic(resolved, extraDataByEntity, unresolvedDate, unresolvedId);
	}
}
