package com.bakdata.conquery.sql.conversion.model.filter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class FlagCondition implements WhereCondition {

	private final List<Field<Boolean>> flagFields;

	public static FlagCondition onColumn(Map<String, Column> flags, Set<String> selectedFlags) {
		List<Field<Boolean>> flagFields = getRequiredColumns(flags, selectedFlags)
				.stream()
				.map(column -> DSL.field(DSL.name(column.getTable().getName(), column.getName()), Boolean.class))
				.toList();
		return new FlagCondition(flagFields);
	}

	/**
	 * @return Columns names of a given flags map that match the selected flags of the filter value.
	 */
	public static List<Column> getRequiredColumns(Map<String, Column> flags, Collection<String> selectedFlags) {
		return selectedFlags.stream()
					 .map(flags::get)
					 .toList();
	}

	@Override
	public Condition condition() {
		return flagFields.stream()
						 .map(DSL::condition)
						 .map(Field::isTrue)
						 .reduce(Condition::or)
						 .orElseThrow(() -> new IllegalArgumentException("Can't construct a FlagCondition with an empty flag field list."));
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
