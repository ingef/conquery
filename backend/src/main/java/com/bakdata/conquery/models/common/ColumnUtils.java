package com.bakdata.conquery.models.common;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ColumnUtils {
	public static boolean assertValidColumnTypes(ColumnId column, Set<MajorTypeId> acceptedColumnTypes) {
		final Column resolved = column.resolve();
		return acceptedColumnTypes.contains(resolved.getType());
	}
}
