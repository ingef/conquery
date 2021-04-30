package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExcelRenderer {


    public static final String EURO_FORMAT = "euroFormat";
    public static final String DATE_FORMAT = "dateFormat";

    @FunctionalInterface
    private interface TypeWriter{
        void writeCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles);
    }


    private static CellStyle generateHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

        return headerStyle;
    }

    public static void renderToStream(
            PrintSettings cfg,
            List<String> idHeaders,
            ManagedExecution<?> exec,
            OutputStream outputStream) throws IOException {
        List<ResultInfo> info = exec.getResultInfo();

        XSSFWorkbook workbook = new XSSFWorkbook();


        Map<String, CellStyle> styles = generateStyles(workbook);


        // TODO internationalize
        Sheet sheet = workbook.createSheet("Result");

        writeHeader(sheet,workbook,idHeaders,info,cfg);

        writeBody(sheet, info,cfg, exec.streamResults(), styles);

        workbook.write(outputStream);

    }

    @NotNull
    private static Map<String, CellStyle> generateStyles(XSSFWorkbook workbook) {
        XSSFDataFormat dataFormat = workbook.createDataFormat();
        Map<String,CellStyle> styles = new HashMap<>();

        XSSFCellStyle styleEuro = workbook.createCellStyle();
        styleEuro.setDataFormat(dataFormat.getFormat("#,##0.00 €"));
        styles.put(EURO_FORMAT,styleEuro);

        XSSFCellStyle styleDate = workbook.createCellStyle();
        styleDate.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));
        styles.put(DATE_FORMAT,styleDate);

        return styles;
    }

    private static void writeHeader(
            Sheet sheet,
            XSSFWorkbook workbook,
            List<String> idHeaders,
            List<ResultInfo> infos,
            PrintSettings cfg){


        Row header = sheet.createRow(0);


        CellStyle headerStyle = generateHeaderStyle(workbook);

        int currentColumn = 1;
        for (String idHeader : idHeaders) {
            Cell headerCell = header.createCell(currentColumn);
            headerCell.setCellValue(idHeader);
            headerCell.setCellStyle(headerStyle);
            currentColumn++;
        }

        for (ResultInfo info : infos) {
            Cell headerCell = header.createCell(currentColumn);
            headerCell.setCellValue(info.getUniqueName(cfg));
            headerCell.setCellStyle(headerStyle);
            currentColumn++;
        }
    }

    private static void writeBody(
            Sheet sheet,
            List<ResultInfo> infos,
            PrintSettings cfg,
            Stream<EntityResult> resultLines,
            Map<String, CellStyle> styles) {

        // Row 1 is the Header the data starts at 2
        AtomicInteger currentRow = new AtomicInteger(2);
        resultLines.forEach(l -> setExcelRow(infos,l, () -> sheet.createRow(currentRow.getAndIncrement()), cfg, styles));

    }

    private static void setExcelRow(
            List<ResultInfo> infos,
            EntityResult internalRow,
            Supplier<Row> externalRowSupplier,
            PrintSettings settings,
            Map<String, CellStyle> styles){
        String[] ids = settings.getIdMapper().map(internalRow).getExternalId();


        for (Object[] resultValues : internalRow.listResultLines()) {
            Row row = externalRowSupplier.get();
            // Write Id cells
            int currentColumn = 1;
            for (String id : ids) {
                Cell idCell = row.createCell(currentColumn);
                idCell.setCellValue(id);
                currentColumn++;
            }

            // Write data cells
            for (int i = 0; i < infos.size(); i++) {
                ResultInfo resultInfo =  infos.get(i);
                Object resultValue =  resultValues[i];
                Cell dataCell = row.createCell(currentColumn);
                currentColumn++;
                if (resultValue == null) {
                    continue;
                }
                TypeWriter typeWriter = TYPE_WRITER_MAP.get(resultInfo.getType().getClass());
                if (typeWriter == null) {
                    // Fallback to string if type is not explicitly registered
                    typeWriter = ExcelRenderer::writeStringCell;
                }
                typeWriter.writeCell(resultInfo, settings, dataCell, resultValue, styles);
            }
        }
    }

    private static void writeStringCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles){
        cell.setCellValue(info.getType().printNullable(settings,value));
    }

    private static void writeBooleanCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        if (value instanceof Boolean) {
            Boolean aBoolean = (Boolean) value;
            cell.setCellValue(aBoolean.booleanValue());
        }
        cell.setCellValue(info.getType().printNullable(settings,value));
    }

    private static void writeDateCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        if(!(value instanceof Number)) {
            throw new IllegalStateException("Expected an Number but got an '" + (value != null ? value.getClass().getName() : "no type") + "' with the value: " + value );
        }
        cell.setCellValue(CDate.toLocalDate(((Number)value).intValue()));
        cell.setCellStyle(styles.get(DATE_FORMAT));
    }

    public static void writeIntegerCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(settings.getIntegerFormat().format(((Number) value).longValue()));
    }

    public static void writeNumericCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(settings.getIntegerFormat().format(((Number) value).doubleValue()));
    }

    public static void writeMoneyCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        if(settings.getCurrency().equals(Currency.getInstance("EUR"))){
            // Print as euro
            cell.setCellStyle(styles.get(EURO_FORMAT));
            cell.setCellValue(
                    new BigDecimal(((Number) value).longValue()).movePointLeft(settings.getCurrency().getDefaultFractionDigits()).doubleValue()
            );
            return;
        }
        // Print as cents or what ever the minor currency unit is
        cell.setCellValue(((Number) value).longValue());
    }


    private final static Map<Class<? extends ResultType>,TypeWriter> TYPE_WRITER_MAP = Map.of(
            ResultType.BooleanT.class, ExcelRenderer::writeBooleanCell,
            ResultType.DateT.class, ExcelRenderer::writeDateCell,
            ResultType.IntegerT.class, ExcelRenderer::writeIntegerCell,
            ResultType.MoneyT.class, ExcelRenderer::writeMoneyCell,
            ResultType.NumericT.class, ExcelRenderer::writeNumericCell
    );
}
