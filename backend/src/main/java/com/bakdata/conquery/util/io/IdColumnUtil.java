package com.bakdata.conquery.util.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.mapping.AutoIncrementingPseudomizer;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.identifiable.mapping.FullIdPrinter;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdColumnUtil {

	/**
	 * If a column contains an ID, create a reader for that ID.
	 */
	public static List<Function<String[], EntityIdMap.ExternalId>> getIdReaders(List<String> format, Map<String, ColumnConfig> idMappers) {
		List<Function<String[], EntityIdMap.ExternalId>> out = new ArrayList<>(format.size());

		for (int index = 0; index < format.size(); index++) {
			final ColumnConfig mapper = idMappers.get(format.get(index));

			if (mapper == null) {
				continue;
			}

			final int finalIndex = index;
			out.add(row -> mapper.read(row[finalIndex]));
		}

		return out;
	}


	/**
	 * Try to create a {@link FullIdPrinter} for user if they are allowed. If not allowed to read ids, they will receive a pseudomized result instead.
	 */
	public static IdPrinter getIdPrinter(Subject owner, ManagedExecution execution, Namespace namespace, List<ColumnConfig> ids) {
		final int size = (int) ids.stream().filter(ColumnConfig::isPrint).count();

		final int pos = IntStream.range(0, ids.size())
								 .filter(idx -> ids.get(idx).isFillAnon())
								 .findFirst()
								 .orElseThrow();

		if (owner.isPermitted(execution.getDataset(), Ability.PRESERVE_ID)) {
			// todo(tm): The integration of ids in the sql connector needs to be properly managed
			if (execution instanceof SqlManagedQuery) {
				return entityResult -> EntityPrintId.from(String.valueOf(entityResult.getEntityId()));
			}
			return new FullIdPrinter(namespace.getStorage().getPrimaryDictionary(), namespace.getStorage().getIdMapping(), size, pos);
		}


		return new AutoIncrementingPseudomizer(size, pos);
	}
}
