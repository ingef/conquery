package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.collect.ImmutableMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExcelRenderer {

    private final static Map<Class<? extends ResultType>, TypeWriter> TYPE_WRITER_MAP = Map.of(
            ResultType.DateT.class, ExcelRenderer::writeDateCell,
            ResultType.IntegerT.class, ExcelRenderer::writeIntegerCell,
            ResultType.MoneyT.class, ExcelRenderer::writeMoneyCell,
            ResultType.NumericT.class, ExcelRenderer::writeNumericCell
    );

    private final SXSSFWorkbook workbook;
    private final ExcelConfig config;
    private final ImmutableMap<String, CellStyle> styles;


    public ExcelRenderer(ExcelConfig config) {
        workbook = new SXSSFWorkbook();
        this.config = config;
        styles = config.generateStyles(workbook);
    }

    @FunctionalInterface
    private interface TypeWriter {
        void writeCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles);
    }

    public <E extends ManagedExecution<?> & SingleTableResult> void renderToStream(
            PrintSettings cfg,
            List<String> idHeaders,
            E exec,
            OutputStream outputStream) throws IOException {
        List<ResultInfo> info = exec.getResultInfo();


        // TODO internationalize
        SXSSFSheet sheet = workbook.createSheet("Result");
        try {
            sheet.setDefaultColumnWidth(config.getDefaultColumnWidth());

            // Create a table environment inside the excel sheet
            XSSFTable table = createTableEnvironment(exec, sheet);

            writeHeader(sheet, idHeaders, info, cfg, table);

            int writtenLines = writeBody(sheet, info, cfg, exec.streamResults());

            postProcessTable(idHeaders, sheet, table, writtenLines);

            workbook.write(outputStream);
        } finally {
            workbook.dispose();
        }

    }

    /**
     * Do postprocessing on the result to improve the visuals:
     * - Set the area of the table environment
     * - Freeze the id columns
     * - Add autofilters (not for now)
     */
    private void postProcessTable(List<String> idHeaders, SXSSFSheet sheet, XSSFTable table, int writtenLines) {
        // Extend the table area to the added data
        CellReference topLeft = new CellReference(0, 0);
        CellReference bottomRight = new CellReference(writtenLines + 1, table.getColumnCount() - 1);
        AreaReference newArea = new AreaReference(topLeft, bottomRight, workbook.getSpreadsheetVersion());
        table.setArea(newArea);

        // TODO Add auto filters. This won't work with excel yet
        //sheet.setAutoFilter(new CellRangeAddress(1, 1, 1, bottomRight.getCol()));

        // Freeze Header and id columns
        sheet.createFreezePane(idHeaders.size(), 1);
    }

    /**
     * Create a table environment, which improves mainly the visuals of the produced table.
     */
    @NotNull
    private XSSFTable createTableEnvironment(ManagedExecution<?> exec, SXSSFSheet sheet) {
        XSSFTable table = sheet.getWorkbook().getXSSFWorkbook().getSheet(sheet.getSheetName()).createTable(null);

        CTTable cttable = table.getCTTable();
        table.setName(exec.getLabelWithoutAutoLabelSuffix());
        cttable.setTotalsRowShown(false);

        CTTableStyleInfo styleInfo = cttable.addNewTableStyleInfo();
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
            List<String> idHeaders,
            List<ResultInfo> infos,
            PrintSettings cfg,
            XSSFTable table) {

        CTTableColumns columns = table.getCTTable().addNewTableColumns();
        columns.setCount(idHeaders.size() + infos.size());

        {
            Row header = sheet.createRow(0);
            // First to create the columns and track them for auto size before the first row is written
            int currentColumn = 0;
            for (String idHeader : idHeaders) {
                CTTableColumn column = columns.addNewTableColumn();
                // Table column ids MUST be set and MUST start at 1, excel will fail otherwise
                column.setId(currentColumn + 1);
                column.setName(idHeader);

                Cell headerCell = header.createCell(currentColumn);
                headerCell.setCellValue(idHeader);

                currentColumn++;
            }

            for (ResultInfo info : infos) {
                final String columnName = info.getUniqueName(cfg);
                CTTableColumn column = columns.addNewTableColumn();
                column.setId(currentColumn + 1);
                column.setName(columnName);

                Cell headerCell = header.createCell(currentColumn);
                headerCell.setCellValue(columnName);

                currentColumn++;
            }
        }
    }

    private int writeBody(
            Sheet sheet,
            List<ResultInfo> infos,
            PrintSettings cfg,
            Stream<EntityResult> resultLines) {

        // Row 0 is the Header the data starts at 1
        AtomicInteger currentRow = new AtomicInteger(1);
        return resultLines.mapToInt(l -> this.writeRowsForEntity(infos, l, () -> sheet.createRow(currentRow.getAndIncrement()), cfg)).sum();
    }

    /**
     * Writes the result lines for each entity.
     */
    private int writeRowsForEntity(
            List<ResultInfo> infos,
            EntityResult internalRow,
            Supplier<Row> externalRowSupplier,
            PrintSettings settings) {
        String[] ids = settings.getIdMapper().map(internalRow).getExternalId();

        int writtenLines = 0;

        for (Object[] resultValues : internalRow.listResultLines()) {
            Row row = externalRowSupplier.get();
            // Write id cells
            int currentColumn = 0;
            for (String id : ids) {
                Cell idCell = row.createCell(currentColumn);
                idCell.setCellValue(id);
                currentColumn++;
            }

            // Write data cells
            for (int i = 0; i < infos.size(); i++) {
                ResultInfo resultInfo = infos.get(i);
                Object resultValue = resultValues[i];
                Cell dataCell = row.createCell(currentColumn);
                currentColumn++;
                if (resultValue == null) {
                    continue;
                }

                // Fallback to string if type is not explicitly registered
                TypeWriter typeWriter = TYPE_WRITER_MAP.getOrDefault(resultInfo.getType().getClass(), ExcelRenderer::writeStringCell);

                typeWriter.writeCell(resultInfo, settings, dataCell, resultValue, styles);
            }
            writtenLines++;
        }
        return writtenLines;
    }

    // Type specific cell writers

    private static void writeStringCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(info.getType().printNullable(settings, value));
    }

    /**
     * Is not used at the moment because at least the german Excel does not seem to understand its own boolean format.
     */
    private static void writeBooleanCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        if (value instanceof Boolean) {
            Boolean aBoolean = (Boolean) value;
            cell.setCellValue(aBoolean);
        }
        cell.setCellValue(info.getType().printNullable(settings, value));
    }

    private static void writeDateCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        if (!(value instanceof Number)) {
            throw new IllegalStateException("Expected an Number but got an '" + (value != null ? value.getClass().getName() : "no type") + "' with the value: " + value);
        }
        cell.setCellValue(CDate.toLocalDate(((Number) value).intValue()));
        cell.setCellStyle(styles.get(ExcelConfig.DATE_STYLE));
    }

    public static void writeIntegerCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(((Number) value).longValue());
        cell.setCellStyle(styles.get(ExcelConfig.INTEGER_STYLE));
    }

    public static void writeNumericCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(((Number) value).doubleValue());
        cell.setCellStyle(styles.get(ExcelConfig.NUMERIC_STYLE));
    }

    public static void writeMoneyCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        CellStyle currencyStyle = styles.get(ExcelConfig.CURRENCY_STYLE_PREFIX + settings.getCurrency().getCurrencyCode());
        if (currencyStyle == null) {
            // Print as cents or what ever the minor currency unit is
            cell.setCellValue(value.toString());
            return;
        }
        cell.setCellStyle(currencyStyle);
        cell.setCellValue(
                new BigDecimal(((Number) value).longValue()).movePointLeft(settings.getCurrency().getDefaultFractionDigits()).doubleValue()
        );
    }
}
