package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import lombok.Getter;

@Getter
public class ExplicitExtractingSelect<V> extends ExtractingSqlSelect<V> implements ExplicitSelect {

	private final SqlSelectId sqlSelectId;

	private ExplicitExtractingSelect(SqlSelectId selectId, String table, String column, Class<V> columnClass) {
		super(table, column, columnClass);
		this.sqlSelectId = selectId;
	}

	public static <V> ExplicitExtractingSelect<V> fromSqlSelect(SqlSelectId sqlSelectId, String table, String column, Class<V> columnClass) {
		return new ExplicitExtractingSelect<V>(sqlSelectId, table, column, columnClass);
	}

	public static <V> ExplicitExtractingSelect<V> fromSelect(Select select, String table, String column, Class<V> columnClass) {
		return new ExplicitExtractingSelect<V>(SqlSelectId.fromSelect(select), table, column, columnClass);
	}

}
