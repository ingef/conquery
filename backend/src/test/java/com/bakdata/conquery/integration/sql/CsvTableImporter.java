package com.bakdata.conquery.integration.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.sql.dialect.TestDialectBundle;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.DatabaseConnectionConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.util.DateReader;
import com.univocity.parsers.csv.CsvParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.Strings;
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
	private final DateReader dateReader;
	private final CsvParser csvReader;
	private final TestDialectBundle testSqlDialect;
	private final DatabaseConnectionConfig databaseConfig;

	public CsvTableImporter(DSLContext dslContext, TestDialectBundle testSqlDialect, DatabaseConnectionConfig databaseConfig) {
		this.dslContext = dslContext;
		this.dateReader = new LocaleConfig().getDateReader();
		this.csvReader = new CSVConfig().withParseHeaders(true).createParser();
		this.testSqlDialect = testSqlDialect;
		this.databaseConfig = databaseConfig;
	}

	@SneakyThrows
	public Set<String> collectAllIds(ResourceFile csvFile, RequiredColumn idColumn) {
		Set<String> allIds = new HashSet<>();

		List<com.univocity.parsers.common.record.Record> records = csvReader.parseAllRecords(csvFile.stream());

		for (com.univocity.parsers.common.record.Record record : records) {

			String raw = record.getString(idColumn.getName());

			allIds.add(raw);
		}

		return allIds;
	}

	public void importAllIds(Collection<RequiredTable> tables) {

		Set<String> allIds = tables.stream()
								   .flatMap(table -> collectAllIds(table.getCsv(), table.getPrimaryColumn()).stream())
								   .collect(Collectors.toSet());

		Table<Record> table = DSL.table(name("entities"));
		List<Field<?>> columns = List.of(field(name("pid"), SQLDataType.VARCHAR(20)));


		List<RowN> content = allIds.stream()
								   .map(Collections::singletonList)
								   .map(DSL::row)
								   .toList();

		// we directly use JDBC because JOOQ can't cope with some custom types like daterange
		dslContext.connection((Connection connection) -> {
			try (Statement statement = connection.createStatement()) {
				dropTable(table, statement);
				createTable(table, columns, statement);
				insertValuesIntoTable(table, columns, content, statement);
			}
		});
	}

	public void createTable(RequiredTable requiredTable) {
		Table<Record> table = DSL.table(name(requiredTable.getName()));
		List<RequiredColumn> allRequiredColumns = getAllRequiredColumns(requiredTable);
		List<Field<?>> columns = createFieldsForColumns(allRequiredColumns);

		// we directly use JDBC because JOOQ can't cope with some custom types like daterange
		dslContext.connection((Connection connection) -> {
			try (Statement statement = connection.createStatement()) {
				dropTable(table, statement);
				createTable(table, columns, statement);
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

		return DSL.field(name(requiredColumn.getName()), dataType);
	}

	/**
	 * Imports the table into the database that is connected to the {@link org.jooq.DSLContext DSLContext}
	 * of this {@link com.bakdata.conquery.integration.sql.CsvTableImporter CSVTableImporter}.
	 */
	public void importTableIntoDatabase(RequiredTable requiredTable) {

		Table<Record> table = DSL.table(name(requiredTable.getName()));
		List<RequiredColumn> allRequiredColumns = getAllRequiredColumns(requiredTable);
		List<Field<?>> columns = createFieldsForColumns(allRequiredColumns);
		List<RowN> content = getTablesContentFromCSV(requiredTable.getCsv(), allRequiredColumns);

		// we directly use JDBC because JOOQ can't cope with some custom types like daterange
		dslContext.connection((Connection connection) -> {
			try (Statement statement = connection.createStatement()) {
				insertValuesIntoTable(table, columns, content, statement);
			}
		});
	}

	@SneakyThrows
	private List<RowN> getTablesContentFromCSV(ResourceFile csvFile, List<RequiredColumn> requiredColumns) {
		csvReader.beginParsing(csvFile.stream());
		List<com.univocity.parsers.common.record.Record> records = csvReader.parseAllRecords();
		List<List<Object>> castedContent = readRecords(records, requiredColumns);
		return castedContent.stream()
							.map(DSL::row)
							.toList();
	}

	private void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement) throws SQLException {
		// encountered empty new line
		if (content.isEmpty()) {
			return;
		}
		log.debug("Inserting into table: {}", content);
		testSqlDialect.getTestFunctionProvider().insertValuesIntoTable(table, columns, content, statement, dslContext);
	}

	/**
	 * Casts all values of each row to the corresponding type of the column the value refers to.
	 */
	private List<List<Object>> readRecords(List<com.univocity.parsers.common.record.Record> rawContent, List<RequiredColumn> requiredColumns) {
		List<List<Object>> castedContent = new ArrayList<>();
		rawContent.forEach(row -> {
			List<Object> castEntriesOfRow = new ArrayList<>(requiredColumns.size());
			for (RequiredColumn col : requiredColumns) {
				try {
					castEntriesOfRow.add(this.readAccordingToColumnType(row, col.getName(), col.getType()));
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Failed to read value %s for %s".formatted(row.getString(col.getName()), col), e);
				}
			}
			castedContent.add(castEntriesOfRow);
		});
		return castedContent;
	}

	private Object readAccordingToColumnType(com.univocity.parsers.common.record.Record record, String column, MajorTypeId type) {

		// if the entry from the CSV is empty, the value in the database should be null
		if (Strings.isNullOrEmpty(record.getString(column))) {
			return null;
		}

		return switch (type) {
			case STRING -> record.getString(column);
			case BOOLEAN -> record.getBoolean(column);
			case INTEGER -> record.getInt(column);
			case REAL -> record.getDouble(column);
			case DECIMAL, MONEY -> record.getBigDecimal(column);
			case DATE -> dateReader.parseToLocalDate(record.getString(column));
			case DATE_RANGE -> {
				CDateRange dateRange = dateReader.parseToCDateRange(record.getString(column));
				yield DateRange.dateRange(dateRange.getMin() != null ? Date.valueOf(dateRange.getMin()) : null, true,
										  dateRange.getMax() != null ? Date.valueOf(dateRange.getMax()) : null, true
				);
			}
		};
	}

}
