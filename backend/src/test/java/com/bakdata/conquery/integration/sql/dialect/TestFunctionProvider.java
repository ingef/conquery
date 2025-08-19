package com.bakdata.conquery.integration.sql.dialect;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.conf.ParamType;

public interface TestFunctionProvider {

	default String createTableStatement(Table<Record> table, List<Field<?>> columns, DSLContext dslContext) {
		return dslContext.createTable(table)
						 .columns(columns)
						 .getSQL(ParamType.INLINED);
	}

	default void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement, DSLContext dslContext)
			throws SQLException {
		String insertIntoTableStatement = dslContext.insertInto(table, columns)
													.valuesOfRows(content)
													.getSQL(ParamType.INLINED);
		statement.execute(insertIntoTableStatement);
	}

	default String createDropTableStatement(Table<Record> table, DSLContext dslContext) {
		return dslContext.dropTableIfExists(table)
						 .getSQL(ParamType.INLINED);
	}

}
