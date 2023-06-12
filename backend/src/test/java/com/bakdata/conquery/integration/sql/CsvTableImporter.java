package com.bakdata.conquery.integration.sql;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.sql.execution.SqlEntityResult;
import com.google.common.base.Strings;
import com.univocity.parsers.csv.CsvParser;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.BuiltInDataType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.postgres.extensions.types.DateRange;

public class CsvTableImporter {

	private final DSLContext dslContext;
	private final DateRangeParser dateRangeParser;
	private final CsvParser csvReader;

	public CsvTableImporter(DSLContext dslContext) {
		this.dslContext = dslContext;
		this.dateRangeParser = new DateRangeParser(new ConqueryConfig());
		this.csvReader = new CSVConfig().withSkipHeader(true).createParser();
	}

	/**
	 * Imports the table into the database that is connected to the {@link org.jooq.DSLContext DSLContext}
	 * of this {@link com.bakdata.conquery.integration.sql.CsvTableImporter CSVTableImporter}.
	 */
	public void importTableIntoDatabase(RequiredTable requiredTable) {

		Table<Record> table = DSL.table(requiredTable.getName());
		List<RequiredColumn> allRequiredColumns = this.getAllRequiredColumns(requiredTable);
		List<Field<?>> columns = this.createFieldsForColumns(allRequiredColumns);
		List<RowN> content = this.getTablesContentFromCSV(requiredTable.getCsv(), allRequiredColumns);

		// because we currently won't shut down the container between the testcases, we drop tables upfront if they
		// exist to ensure consistency if table names of different testcases are the same
		String dropTableStatement = dslContext.dropTableIfExists(table)
											  .getSQL(ParamType.INLINED);

		String createTableStatement = dslContext.createTable(table)
												.columns(columns)
												.getSQL(ParamType.INLINED);

		String insertIntoTableStatement = dslContext.insertInto(table, columns)
													.valuesOfRows(content)
													.getSQL(ParamType.INLINED);

		// we directly use JDBC because JOOQ can't cope with PostgreSQL custom types
		dslContext.connection((Connection connection) -> {
			try (Statement statement = connection.createStatement()) {
				statement.execute(dropTableStatement);
				statement.execute(createTableStatement);
				statement.execute(insertIntoTableStatement);
			}
		});
	}

	public List<EntityResult> readExpectedEntities(Path csv) throws IOException {
		List<String[]> rawEntities = this.csvReader.parseAll(Files.newInputStream(csv));
		List<EntityResult> results = new ArrayList<>(rawEntities.size());
		for (int i = 0; i < rawEntities.size(); i++) {
			String[] row = rawEntities.get(i);
			results.add(new SqlEntityResult(i + 1, row[0], Arrays.copyOfRange(row, 1, row.length)));
		}
		return results;
	}


	private List<Field<?>> createFieldsForColumns(List<RequiredColumn> requiredColumns) {
		return requiredColumns.stream()
							  .map(this::createField)
							  .collect(Collectors.toList());
	}

	private List<RequiredColumn> getAllRequiredColumns(RequiredTable table) {
		ArrayList<RequiredColumn> requiredColumns = new ArrayList<>();
		requiredColumns.add(table.getPrimaryColumn());
		requiredColumns.addAll(Arrays.stream(table.getColumns()).toList());
		return requiredColumns;
	}

	private Field<?> createField(RequiredColumn requiredColumn) {
		DataType<?> dataType = switch (requiredColumn.getType()) {
			case STRING -> SQLDataType.VARCHAR;
			case INTEGER -> SQLDataType.INTEGER;
			case BOOLEAN -> SQLDataType.BOOLEAN;
			case REAL -> SQLDataType.REAL;
			case DECIMAL, MONEY -> SQLDataType.DECIMAL;
			case DATE -> SQLDataType.DATE;
			case DATE_RANGE -> new BuiltInDataType<>(DateRange.class, "daterange");
		};
		return DSL.field(requiredColumn.getName(), dataType);
	}

	@SneakyThrows
	private List<RowN> getTablesContentFromCSV(ResourceFile csvFile, List<RequiredColumn> requiredColumns) {
		List<String[]> rawContent = this.csvReader.parseAll(csvFile.stream());
		List<List<Object>> castedContent = this.castContent(rawContent, requiredColumns);
		return castedContent.stream()
							.map(DSL::row)
							.toList();
	}

	/**
	 * Casts all values of each row to the corresponding type of the column the value refers to.
	 */
	private List<List<Object>> castContent(List<String[]> rawContent, List<RequiredColumn> requiredColumns) {
		List<List<Object>> castedContent = new ArrayList<>(rawContent.size());
		for (String[] row : rawContent) {
			List<Object> castEntriesOfRow = new ArrayList<>(row.length);
			for (int i = 0; i < row.length; i++) {
				MajorTypeId type = requiredColumns.get(i).getType();
				castEntriesOfRow.add(this.castEntryAccordingToColumnType(row[i], type));
			}
			castedContent.add(castEntriesOfRow);
		}
		return castedContent;
	}

	private Object castEntryAccordingToColumnType(String entry, MajorTypeId type) {

		// if the entry from the CSV is empty, the value in the database should be null
		if (Strings.isNullOrEmpty(entry)) {
			return null;
		}

		return switch (type) {
			case STRING -> entry;
			case BOOLEAN -> Boolean.valueOf(entry);
			case INTEGER -> Integer.valueOf(entry);
			case REAL -> Float.valueOf(entry);
			case DECIMAL, MONEY -> new BigDecimal(entry);
			case DATE -> Date.valueOf(entry);
			case DATE_RANGE -> {
				CDateRange dateRange = this.dateRangeParser.parse(entry);
				yield DateRange.dateRange(Date.valueOf(dateRange.getMin()), Date.valueOf(dateRange.getMax()));
			}
		};
	}

}
