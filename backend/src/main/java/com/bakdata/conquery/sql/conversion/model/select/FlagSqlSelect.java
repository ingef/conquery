package com.bakdata.conquery.sql.conversion.model.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

public class FlagSqlSelect implements SqlSelect {

	public static final Param<Integer> NUMERIC_TRUE_VAL = DSL.val(1);
	public static final Param<Integer> NUMERIC_FALSE_VAL = DSL.val(0);
	public static final Param<String> EMPTY_VAL = DSL.val("");
	public static final Param<String> OPENING_BRACE = DSL.val("{");
	public static final Param<String> CLOSING_BRACE = DSL.val("}");
	public static final Param<String> COMMA_VAL = DSL.val(",");

	private final Map<String, Field<Boolean>> flagFieldsMap;
	private final String alias;

	public FlagSqlSelect(Map<String, Field<Boolean>> flagFieldsMap, String alias) {
		this.flagFieldsMap = flagFieldsMap;
		this.alias = alias;
	}

	@Override
	public Field<String> select() {

		List<Condition> anyTrueConditions = new ArrayList<>();
		List<Field<String>> flags = new ArrayList<>();

		for (Map.Entry<String, Field<Boolean>> entry : this.flagFieldsMap.entrySet()) {

			Condition anyTrue = DSL.max(DSL.when(entry.getValue().isTrue(), NUMERIC_TRUE_VAL)
										   .otherwise(NUMERIC_FALSE_VAL))
								   .eq(NUMERIC_TRUE_VAL);
			anyTrueConditions.add(anyTrue);

			// we have to prevent null values because then the whole String aggregation is null
			Field<String> flag = DSL.when(anyTrue, DSL.val(entry.getKey()))
									.otherwise(EMPTY_VAL);
			flags.add(flag);
		}

		if (flags.size() > 1) {
			for (int i = 0; i < flags.size() - 1; i++) {
				addCommaAfterFlagIfAnyFollowingFlagIsTrue(anyTrueConditions, flags, i);
			}
		}

		return DSL.concat(
				OPENING_BRACE,
				DSL.concat(flags.toArray(Field[]::new)),
				CLOSING_BRACE
		).as(alias);
	}

	@Override
	public Field<String> aliased() {
		return DSL.field(alias, String.class);
	}

	@Override
	public List<String> columnNames() {
		return this.flagFieldsMap.values().stream()
								 .map(Field::getName)
								 .toList();
	}

	private void addCommaAfterFlagIfAnyFollowingFlagIsTrue(List<Condition> anyTrueConditions, List<Field<String>> flags, int i) {

		List<Condition> conditionsLeft = anyTrueConditions.subList(i, flags.size());

		Condition currentFlagAndAnyFollowingTrue = conditionsLeft.get(0);
		Condition anyFollowingFlagTrue = conditionsLeft.get(1);
		for (int y = 2; y < conditionsLeft.size(); y++) {
			anyFollowingFlagTrue = anyFollowingFlagTrue.or(conditionsLeft.get(y));
		}
		currentFlagAndAnyFollowingTrue = currentFlagAndAnyFollowingTrue.and(anyFollowingFlagTrue);

		Field<String> comma = DSL.when(currentFlagAndAnyFollowingTrue, COMMA_VAL)
								 .otherwise(EMPTY_VAL);

		flags.set(i, DSL.concat(flags.get(i), comma));
	}

}
