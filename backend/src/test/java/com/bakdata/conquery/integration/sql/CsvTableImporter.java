package com.bakdata.conquery.integration.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.sql.dialect.TestSqlDialect;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.google.common.base.Strings;
import com.univocity.parsers.csv.CsvParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.impl.BuiltInDataType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.postgres.extensions.types.DateRange;

@Slf4j
public class CsvTableImporter {

	private static final int DEFAULT_VARCHAR_LENGTH = 25; // HANA will use 1 as default otherwise
	private final DSLContext dslContext;
	private final DateRangeParser dateRangeParser;
	private final CsvParser csvReader;
	private final TestSqlDialect testSqlDialect;
	private final DatabaseConfig databaseConfig;

	public CsvTableImporter(DSLContext dslContext, TestSqlDialect testSqlDialect, DatabaseConfig databaseConfig) {
		this.dslContext = dslContext;
		this.dateRangeParser = new DateRangeParser(new ConqueryConfig());
		this.csvReader = new CSVConfig().withParseHeaders(true).createParser();
		this.testSqlDialect = testSqlDialect;
		this.databaseConfig = databaseConfig;
	}

	/**
	 * Imports the table into the database that is connected to the {@link org.jooq.DSLContext DSLContext}
	 * of this {@link com.bakdata.conquery.integration.sql.CsvTableImporter CSVTableImporter}.
	 */
	public void importTableIntoDatabase(RequiredTable requiredTable) {

		Table<Record> table = DSL.table(DSL.name(requiredTable.getName()));
		List<RequiredColumn> allRequiredColumns = this.getAllRequiredColumns(requiredTable);
		List<Field<?>> columns = this.createFieldsForColumns(allRequiredColumns);
		List<RowN> content = this.getTablesContentFromCSV(requiredTable.getCsv(), allRequiredColumns);

		// we directly use JDBC because JOOQ can't cope with some custom types like daterange
		dslContext.connection((Connection connection) -> {
			try (Statement statement = connection.createStatement()) {
				dropTable(table, statement);
				createTable(table, columns, statement);
				insertValuesIntoTable(table, columns, content, statement);
			}
		});
	}

	private List<RequiredColumn> getAllRequiredColumns(RequiredTable table) {
		ArrayList<RequiredColumn> requiredColumns = new ArrayList<>();
		requiredColumns.add(table.getPrimaryColumn());
		requiredColumns.addAll(Arrays.stream(table.getColumns()).toList());
		return requiredColumns;
	}

	private List<Field<?>> createFieldsForColumns(List<RequiredColumn> requiredColumns) {
		return requiredColumns.stream()
							  .map(this::createField)
							  .collect(Collectors.toList());
	}

	@SneakyThrows
	private List<RowN> getTablesContentFromCSV(ResourceFile csvFile, List<RequiredColumn> requiredColumns) {

		List<List<Object>> castedContent = new ArrayList<>();

		List<com.univocity.parsers.common.record.Record> records = csvReader.parseAllRecords(csvFile.stream());

		for (com.univocity.parsers.common.record.Record record : records) {
			List<Object> castEntriesOfRow = new ArrayList<>(requiredColumns.size());

			for (RequiredColumn column : requiredColumns) {
				castEntriesOfRow.add(castEntryAccordingToColumnType(record.getValue(column.getName(), null), column.getType()));
			}

			castedContent.add(castEntriesOfRow);
		}

		return castedContent.stream()
							.map(DSL::row)
							.toList();
	}

	private void dropTable(Table<Record> table, Statement statement) {
		try {
			String dropTableStatement = testSqlDialect.getTestFunctionProvider().createDropTableStatement(table, dslContext);
			statement.execute(dropTableStatement);
		}
		catch (SQLException e) {
			log.debug("Dropping table {} failed.", table.getName(), e);
		}
	}

	private void createTable(Table<Record> table, List<Field<?>> columns, Statement statement) throws SQLException {
		String createTableStatement = testSqlDialect.getTestFunctionProvider().createTableStatement(table, columns, dslContext);

		log.debug("Creating table: {}", createTableStatement);
		statement.execute(createTableStatement);
	}

	private void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement) throws SQLException {
		// encountered empty new line
		if (content.isEmpty()) {
			return;
		}
		log.debug("Inserting into table: {}", content);
		testSqlDialect.getTestFunctionProvider().insertValuesIntoTable(table, columns, content, statement, dslContext);
	}

	private Field<?> createField(RequiredColumn requiredColumn) {
		DataType<?> dataType = switch (requiredColumn.getType()) {
			case STRING -> SQLDataType.VARCHAR(DEFAULT_VARCHAR_LENGTH);
			case INTEGER -> SQLDataType.INTEGER;
			case BOOLEAN -> SQLDataType.BOOLEAN;
			// TODO (ja) how do we handle REAL and DECIMAL properly?
			case REAL, DECIMAL, MONEY -> SQLDataType.DECIMAL(10, 2);
			case DATE -> SQLDataType.DATE;
			case DATE_RANGE -> new BuiltInDataType<>(DateRange.class, "daterange");
		};

		// Set all columns except 'pid' to nullable, important for ClickHouse compatibility
		if (!requiredColumn.getName().equals(databaseConfig.getPrimaryColumn())) {
			dataType = dataType.nullable(true);
		}

		return DSL.field(DSL.name(requiredColumn.getName()), dataType);
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
				yield DateRange.dateRange(dateRange.getMin() != null ? Date.valueOf(dateRange.getMin()) : null,
										  dateRange.getMax() != null ? Date.valueOf(dateRange.getMax()) : null
				);
			}
		};
	}

}
