package com.bakdata.conquery.io.result.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ExcelSheetNameC10n;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.ExcelResultPrinters;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;

public class ExcelRenderer {

	public static final int MAX_LINES = 1_048_576;
	public static final int CHARACTER_WIDTH_DIVISOR = 256;
	public static final int AUTOFILTER_SPACE_WIDTH = 3;
	private final SXSSFWorkbook workbook;
	private final ExcelConfig config;
	private final PrintSettings settings;
	private final ImmutableMap<String, CellStyle> styles;
	public ExcelRenderer(ExcelConfig config, PrintSettings settings) {
		workbook = new SXSSFWorkbook();
		this.config = config;
		styles = config.generateStyles(workbook, settings);
		this.settings = settings;
	}

	public <E extends ManagedExecution & SingleTableResult> void renderToStream(List<ResultInfo> idHeaders, E exec, OutputStream outputStream, OptionalLong limit, PrintSettings printSettings)
			throws IOException {
		final List<ResultInfo> resultInfosExec = exec.getResultInfos();

		setMetaData(exec);

		final SXSSFSheet sheet = workbook.createSheet(C10N.get(ExcelSheetNameC10n.class, I18n.LOCALE.get()).result());
		try {
			sheet.setDefaultColumnWidth(config.getDefaultColumnWidth());

			// Create a table environment inside the excel sheet
			final XSSFTable table = createTableEnvironment(exec, sheet);

			writeHeader(sheet, idHeaders, resultInfosExec, table, printSettings);

			final int writtenLines = writeBody(sheet, resultInfosExec, exec.streamResults(OptionalLong.of(limit.orElse(MAX_LINES))), new ExcelResultPrinters());

			postProcessTable(sheet, table, writtenLines, idHeaders.size());

			workbook.write(outputStream);
		}
		finally {
			workbook.dispose();
		}

	}

	/**
	 * Include meta data in the xlsx such as the title, owner/author, tag and the name of this instance.
	 */
	private <E extends ManagedExecution & SingleTableResult> void setMetaData(E exec) {
		final POIXMLProperties.CoreProperties coreProperties = workbook.getXSSFWorkbook().getProperties().getCoreProperties();
		coreProperties.setTitle(exec.getLabelWithoutAutoLabelSuffix());

		final User owner = exec.getOwner();
		coreProperties.setCreator(owner != null ? owner.getLabel() : config.getApplicationName());
		coreProperties.setKeywords(String.join(" ", exec.getTags()));
		final POIXMLProperties.ExtendedProperties extendedProperties = workbook.getXSSFWorkbook().getProperties().getExtendedProperties();
		extendedProperties.setApplication(config.getApplicationName());
	}

	/**
	 * Create a table environment, which improves mainly the visuals of the produced table.
	 */
	@NotNull
	private XSSFTable createTableEnvironment(ManagedExecution exec, SXSSFSheet sheet) {
		final XSSFTable table = sheet.getWorkbook().getXSSFWorkbook().getSheet(sheet.getSheetName()).createTable(null);

		final CTTable cttable = table.getCTTable();
		table.setName(exec.getLabelWithoutAutoLabelSuffix());
		cttable.setTotalsRowShown(false);

		final CTTableStyleInfo styleInfo = cttable.addNewTableStyleInfo();
		// Not sure how important this name is
		styleInfo.setName("TableStyleMedium2");
		styleInfo.setShowColumnStripes(false);
		styleInfo.setShowRowStripes(true);
		return table;
	}

	/**
	 * Write the header and initialize the columns for the table environment.
	 * Also autosize the columns according to the header width.
	 */
	private void writeHeader(
			SXSSFSheet sheet,
			List<ResultInfo> idHeaders,
			List<ResultInfo> infos,
			XSSFTable table, PrintSettings printSettings) {

		final CTTableColumns columns = table.getCTTable().addNewTableColumns();
		columns.setCount(idHeaders.size() + infos.size());
		final UniqueNamer uniqueNamer = new UniqueNamer(settings);

		{
			final Row header = sheet.createRow(0);
			// First to create the columns and track them for auto size before the first row is written
			int currentColumn = 0;
			for (ResultInfo idHeader : idHeaders) {
				final CTTableColumn column = columns.addNewTableColumn();
				// Table column ids MUST be set and MUST start at 1, excel will fail otherwise
				column.setId(currentColumn + 1);
				final String uniqueName = uniqueNamer.getUniqueName(idHeader, printSettings);
				column.setName(uniqueName);

				final Cell headerCell = header.createCell(currentColumn);
				headerCell.setCellValue(uniqueName);

				// Track column explicitly, because sheet.trackAllColumnsForAutoSizing() does not work with
				// sheet.getTrackedColumnsForAutoSizing(), if no flush has happened
				sheet.trackColumnForAutoSizing(currentColumn);

				currentColumn++;
			}

			for (ResultInfo info : infos) {
				final String columnName = uniqueNamer.getUniqueName(info, printSettings);
				final CTTableColumn column = columns.addNewTableColumn();
				column.setId(currentColumn + 1);
				column.setName(columnName);

				final Cell headerCell = header.createCell(currentColumn);
				headerCell.setCellValue(columnName);

				sheet.trackColumnForAutoSizing(currentColumn);

				currentColumn++;
			}
		}
	}

	private int writeBody(
			SXSSFSheet sheet,
			List<ResultInfo> infos,
			Stream<EntityResult> resultLines, PrinterFactory printerFactory) {

		// Row 0 is the Header the data starts at 1
		final AtomicInteger currentRow = new AtomicInteger(1);

		final TypeWriter[] writers = infos.stream().map(info -> writer(info.getType(), info.createPrinter(printerFactory, settings), settings)).toArray(TypeWriter[]::new);
		final PrintIdMapper idMapper = settings.getIdMapper();

		final int writtenLines = resultLines.mapToInt(l -> writeRowsForEntity(infos, l, currentRow, sheet, writers, idMapper)).sum();

		// The result was shorter than the number of rows to track, so we auto size here explicitly
		if (writtenLines < config.getLastRowToAutosize()) {
			setColumnWidthsAndUntrack(sheet);
		}

		return writtenLines;
	}

	/**
	 * Do postprocessing on the result to improve the visuals:
	 * - Set the area of the table environment
	 * - Freeze the id columns
	 * - Add autofilters (not for now)
	 */
	private void postProcessTable(SXSSFSheet sheet, XSSFTable table, int writtenLines, int size) {
		// Extend the table area to the added data
		final CellReference topLeft = new CellReference(0, 0);

		// The area must be at least a header row and a data row. If no line was written we include an empty data row so POI is happy
		final CellReference bottomRight = new CellReference(Math.max(1, writtenLines), table.getColumnCount() - 1);
		final AreaReference newArea = new AreaReference(topLeft, bottomRight, workbook.getSpreadsheetVersion());
		table.setArea(newArea);

		// Add auto filters. This must be done on the lower level CTTable. Using SXSSFSheet::setAutoFilter will corrupt the table
		table.getCTTable().addNewAutoFilter();

		// Freeze Header and id columns
		sheet.createFreezePane(size, 1);
	}

	/**
	 * Writes the result lines for each entity.
	 */
	private int writeRowsForEntity(List<ResultInfo> infos, EntityResult internalRow, final AtomicInteger currentRow, SXSSFSheet sheet, TypeWriter[] writers, PrintIdMapper idMapper) {

		final String[] ids = idMapper.map(internalRow).getExternalId();

		int writtenLines = 0;

		for (Object[] line : internalRow.listResultLines()) {
			final int thisRow = currentRow.getAndIncrement();
			final Row row = sheet.createRow(thisRow);

			// Write id cells
			int currentColumn = 0;

			for (String id : ids) {
				final Cell idCell = row.createCell(currentColumn);
				idCell.setCellValue(id);
				currentColumn++;
			}

			// Write data cells
			for (int index = 0; index < infos.size(); index++) {
				final Object value = line[index];
				final Cell dataCell = row.createCell(currentColumn);
				currentColumn++;

				if (value == null) {
					continue;
				}


				// Fallback to string if type is not explicitly registered
				final TypeWriter typeWriter = writers[index];

				typeWriter.writeCell(value, dataCell, styles);
			}

			if (thisRow == config.getLastRowToAutosize()) {
				// Last row rows to track for auto sizing the column width is reached. Untrack to remove the performance penalty.
				setColumnWidthsAndUntrack(sheet);
			}
			writtenLines++;
		}
		return writtenLines;
	}

	@SneakyThrows(IOException.class)
	private void setColumnWidthsAndUntrack(SXSSFSheet sheet) {
		sheet.flushRows();
		for (Integer columnIndex : sheet.getTrackedColumnsForAutoSizing()) {
			sheet.autoSizeColumn(columnIndex);

			// Scale the widths to a 256th of a char
			final int defaultColumnWidth = config.getDefaultColumnWidth() * CHARACTER_WIDTH_DIVISOR;

			// Add a bit extra space for the drop down arrow of the auto filter (so it does not overlap with the column header name)
			int columnWidth = sheet.getColumnWidth(columnIndex) + AUTOFILTER_SPACE_WIDTH * CHARACTER_WIDTH_DIVISOR;

			// Limit the column with to the default width if it is longer
			if (columnWidth > defaultColumnWidth) {
				columnWidth = defaultColumnWidth;
			}
			sheet.setColumnWidth(columnIndex, columnWidth);

			// Disable auto sizing so we don't have a performance penalty
			sheet.untrackColumnForAutoSizing(columnIndex);
		}
	}

	private static TypeWriter writer(ResultType type, Printer printer, PrintSettings settings) {
		if (type instanceof ResultType.ListT<?>) {
			//Excel cannot handle LIST types so we just toString them.
			return (value, cell, styles) -> writeStringCell(cell, value, printer);
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> (value, cell, styles) -> writeBooleanCell(value, cell, printer);
			case INTEGER -> (value, cell, styles) -> writeIntegerCell(value, cell, printer, styles);
			case MONEY -> (value, cell, styles) -> writeMoneyCell(value, cell, printer, settings, styles);
			case NUMERIC -> (value, cell, styles) -> writeNumericCell(value, cell, printer, styles);
			case DATE -> (value, cell, styles) -> writeDateCell(value, cell, printer, styles);
			default -> (value, cell, styles) -> writeStringCell(cell, value, printer);
		};
	}

	// Type specific cell writers
	private static void writeStringCell(Cell cell, Object value, Printer printer) {
		cell.setCellValue((String) printer.apply(value));
	}

	/**
	 * This writer is only used on Columns with the result type {@link ResultType.Primitive#BOOLEAN}, not on complex types such as `LIST[BOOLEAN]`,
	 * because MS Excel can only represent those as strings
	 */
	private static void writeBooleanCell(Object value, Cell cell, Printer printer) {
		cell.setCellValue((Boolean) printer.apply(value));
	}

	public static void writeIntegerCell(Object value, Cell cell, Printer printer, Map<String, CellStyle> styles) {
		cell.setCellValue(((Number) printer.apply(value)).longValue());
		cell.setCellStyle(styles.get(ExcelConfig.INTEGER_STYLE));
	}

	public static void writeMoneyCell(Object valueRaw, Cell cell, Printer printer, PrintSettings settings, Map<String, CellStyle> styles) {

		final BigDecimal value = (BigDecimal) printer.apply(valueRaw);

		final CellStyle currencyStyle = styles.get(ExcelConfig.CURRENCY_STYLE_PREFIX + settings.getCurrency().getCurrencyCode());
		if (currencyStyle == null) {
			// Print as cents or whatever the minor currency unit is
			cell.setCellValue(value.movePointRight(settings.getCurrency().getDefaultFractionDigits()).intValue());
			return;
		}
		cell.setCellStyle(currencyStyle);
		cell.setCellValue(value.doubleValue());
	}

	public static void writeNumericCell(Object value, Cell cell, Printer printer, Map<String, CellStyle> styles) {
		cell.setCellValue(((Number) printer.apply(value)).doubleValue());
		cell.setCellStyle(styles.get(ExcelConfig.NUMERIC_STYLE));
	}

	private static void writeDateCell(Object value, Cell cell, Printer printer, Map<String, CellStyle> styles) {
		cell.setCellValue((LocalDate) printer.apply(value));
		cell.setCellStyle(styles.get(ExcelConfig.DATE_STYLE));
	}

	@FunctionalInterface
	private interface TypeWriter {
		void writeCell(Object value, Cell cell, Map<String, CellStyle> styles);
	}
}
