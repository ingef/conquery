package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExcelRenderer {


    public static final String EURO_FORMAT = "euroFormat";
    public static final String DATE_FORMAT = "dateFormat";

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

        XSSFDataFormat dataFormat = workbook.createDataFormat();

        Map<String,CellStyle> styles = new HashMap<>();
        XSSFCellStyle styleEuro = workbook.createCellStyle();
        styleEuro.setDataFormat(dataFormat.getFormat("_(\"€\"* #,##0.00_);_(\"€\"* (#,##0.00);_(\"€\"* \"-\"??_);_(@_)"));
        styles.put(EURO_FORMAT,styleEuro);
        XSSFCellStyle styleDate = workbook.createCellStyle();
        styleDate.setDataFormat(dataFormat.getFormat("d-m-yyyy"));
        styles.put(DATE_FORMAT,styleDate);



        // TODO internationalize
        Sheet sheet = workbook.createSheet("Result");

        writeHeader(sheet,workbook,idHeaders,info,cfg);

        writeBody(sheet, info,cfg, exec.streamResults(), styles);

        workbook.write(outputStream);

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
                resultInfo.getType().writeExcelCell(resultInfo,settings,dataCell, resultValue, styles);
                currentColumn++;
            }
        }
    }
}
