package com.bakdata.conquery.models.common;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
public class ColumnUtils {
	public static boolean assertValidColumnTypes(Identifiable holder, ColumnId column, Set<MajorTypeId> acceptedColumnTypes) {
		final Column resolved = column.resolve();
		final boolean acceptable = acceptedColumnTypes.contains(resolved.getType());

		if (!acceptable) {
			log.error("Issue with `{}`, column `{}` is of ype `{}`. Not one of [{}]", holder.getId(), resolved.getId(), resolved.getType(), acceptedColumnTypes);
		}

		return acceptable;
	}
}
