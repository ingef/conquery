package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class CountSqlSelect implements SqlSelect {

	private final Field<Object> columnToCount;
	private final String alias;
	private final CountType countType;

	@Override
	public Field<Integer> select() {
		Field<Integer> countField = countType == CountType.DISTINCT ? DSL.countDistinct(columnToCount) : DSL.count(columnToCount);
		return countField.as(alias);
	}

	@Override
	public Field<BigDecimal> aliased() {
		return DSL.field(alias, BigDecimal.class);
	}

	@Override
	public String columnName() {
		return columnToCount.getName();
	}

	public enum CountType {
		DEFAULT,
		DISTINCT;

		public static CountType fromBoolean(boolean value) {
			return value ? DISTINCT : DEFAULT;
		}
	}

}
