package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExcelRenderer {
    
    private final static Map<Class<? extends ResultType>,TypeWriter> TYPE_WRITER_MAP = Map.of(
            ResultType.BooleanT.class, ExcelRenderer::writeBooleanCell,
            ResultType.DateT.class, ExcelRenderer::writeDateCell,
            ResultType.IntegerT.class, ExcelRenderer::writeIntegerCell,
            ResultType.MoneyT.class, ExcelRenderer::writeMoneyCell,
            ResultType.NumericT.class, ExcelRenderer::writeNumericCell
    );

    private final XSSFWorkbook workbook;
    private final ImmutableMap<String, CellStyle> styles;


    public ExcelRenderer(ExcelConfig config) {
        workbook = new XSSFWorkbook();
        styles = config.generateStyles(workbook);
    }

    @FunctionalInterface
    private interface TypeWriter{
        void writeCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles);
    }

    public void renderToStream(
            PrintSettings cfg,
            List<String> idHeaders,
            ManagedExecution<?> exec,
            OutputStream outputStream) throws IOException {
        List<ResultInfo> info = exec.getResultInfo();


        // TODO internationalize
        Sheet sheet = workbook.createSheet("Result");

        writeHeader(sheet,workbook,idHeaders,info,cfg);

        writeBody(sheet, info,cfg, exec.streamResults());

        workbook.write(outputStream);

    }

    private void writeHeader(
            Sheet sheet,
            XSSFWorkbook workbook,
            List<String> idHeaders,
            List<ResultInfo> infos,
            PrintSettings cfg){
        final CellStyle style = styles.get(ExcelConfig.HEADER_STYLE);
        Row header = sheet.createRow(0);


        int currentColumn = 1;
        for (String idHeader : idHeaders) {
            Cell headerCell = header.createCell(currentColumn);
            headerCell.setCellValue(idHeader);
            headerCell.setCellStyle(style);
            currentColumn++;
        }

        for (ResultInfo info : infos) {
            Cell headerCell = header.createCell(currentColumn);
            headerCell.setCellValue(info.getUniqueName(cfg));
            headerCell.setCellStyle(style);
            currentColumn++;
        }
    }

    private void writeBody(
            Sheet sheet,
            List<ResultInfo> infos,
            PrintSettings cfg,
            Stream<EntityResult> resultLines) {

        // Row 1 is the Header the data starts at 2
        AtomicInteger currentRow = new AtomicInteger(2);
        resultLines.forEach(l -> this.writeRowsForEntity(infos,l, () -> sheet.createRow(currentRow.getAndIncrement()), cfg));
    }

    private void writeRowsForEntity(
            List<ResultInfo> infos,
            EntityResult internalRow,
            Supplier<Row> externalRowSupplier,
            PrintSettings settings){
        String[] ids = settings.getIdMapper().map(internalRow).getExternalId();


        for (Object[] resultValues : internalRow.listResultLines()) {
            Row row = externalRowSupplier.get();
            // Write id cells
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

                // Fallback to string if type is not explicitly registered
                TypeWriter typeWriter = TYPE_WRITER_MAP.getOrDefault(resultInfo.getType().getClass(), ExcelRenderer::writeStringCell);

                typeWriter.writeCell(resultInfo, settings, dataCell, resultValue, styles);
            }
        }
    }

    // Type specific cell writers

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
        cell.setCellStyle(styles.get(ExcelConfig.DATE_STYLE));
    }

    public static void writeIntegerCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(settings.getIntegerFormat().format(((Number) value).longValue()));
    }

    public static void writeNumericCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        cell.setCellValue(settings.getIntegerFormat().format(((Number) value).doubleValue()));
    }

    public static void writeMoneyCell(ResultInfo info, PrintSettings settings, Cell cell, Object value, Map<String, CellStyle> styles) {
        CellStyle currencyStyle = styles.get(ExcelConfig.CURRENCY_STYLE_PREFIX + settings.getCurrency().getCurrencyCode());
        if(currencyStyle == null){
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
