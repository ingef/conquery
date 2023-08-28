package com.bakdata.conquery.integration.sql;

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
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.BuiltInDataType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.postgres.extensions.types.DateRange;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CsvTableImporter {

	private static final int DEFAULT_VARCHAR_LENGTH = 25; // HANA will use 1 as default otherwise
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

	public List<EntityResult> readExpectedEntities(Path csv) throws IOException {
		List<String[]> rawEntities = this.csvReader.parseAll(Files.newInputStream(csv));
		List<EntityResult> results = new ArrayList<>(rawEntities.size());
		for (int i = 0; i < rawEntities.size(); i++) {
			String[] row = rawEntities.get(i);
			results.add(new SqlEntityResult(i + 1, row[0], Arrays.copyOfRange(row, 1, row.length)));
		}
		return results;
	}

	private void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement) throws SQLException {
		for (RowN rowN : content) {
			// e.g. HANA does not support bulk insert, so we insert row by row
			String insertRowStatement = dslContext.insertInto(table, columns)
												  .values(rowN)
												  .getSQL(ParamType.INLINED);
			log.info("Inserting into table: {}", insertRowStatement);
			statement.execute(insertRowStatement);
		}
	}

	private void createTable(Table<Record> table, List<Field<?>> columns, Statement statement) throws SQLException {
		String createTableStatement = dslContext.createTable(table)
												.columns(columns)
												.getSQL(ParamType.INLINED);
		log.info("Creating table: {}", createTableStatement);
		statement.execute(createTableStatement);
	}

	private void dropTable(Table<Record> table, Statement statement) {
		try {
			// DROP TABLE IF EXISTS is not supported in HANA, we just ignore possible errors if the table does not exist
			String dropTableStatement = dslContext.dropTable(table)
												  .getSQL(ParamType.INLINED);
			statement.execute(dropTableStatement);
		}
		catch (SQLException e) {
			log.info("Dropping table {} failed.", table.getName(), e);
		}
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
			case STRING -> SQLDataType.VARCHAR(DEFAULT_VARCHAR_LENGTH);
			case INTEGER -> SQLDataType.INTEGER;
			case BOOLEAN -> SQLDataType.BOOLEAN;
			// TODO: temporary workaround until we cast ResultSet elements back
			case REAL -> SQLDataType.DECIMAL(10, 2);
			case DECIMAL, MONEY -> SQLDataType.DECIMAL;
			case DATE -> SQLDataType.DATE;
			case DATE_RANGE -> new BuiltInDataType<>(DateRange.class, "daterange");
		};
		return DSL.field(DSL.name(requiredColumn.getName()), dataType);
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
