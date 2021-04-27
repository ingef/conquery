package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExcelRenderer {


    public static final short EURO_FORMAT = (short) 0x32;

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
            String[] idHeaders,
            ManagedExecution<?> exec) throws IOException {
        List<ResultInfo> infos = ManagedExecution.getResultInfos(exec);

        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFDataFormat dataformat = workbook.createDataFormat();
        dataformat.putFormat(EURO_FORMAT, "_(\"€\"* #,##0.00_);_(\"€\"* (#,##0.00);_(\"€\"* \"-\"??_);_(@_)");

        // TODO internationalize
        Sheet sheet = workbook.createSheet("Result");


        writeHeader(sheet,workbook,idHeaders,infos,cfg);

        writeBody(sheet,workbook,idHeaders,infos,cfg, ManagedExecution.getResults(exec));


    }

    private static void writeHeader(
            Sheet sheet,
            XSSFWorkbook workbook,
            String[] idHeaders,
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
            XSSFWorkbook workbook,
            String[] idHeaders,
            List<ResultInfo> infos,
            PrintSettings cfg,
            Stream<EntityResult> resultLines) {

        // Row 1 is the Header the data starts at 2
        AtomicInteger currentRow = new AtomicInteger(2);
        resultLines.forEach(l -> {
            setExcelRow(infos,l, () -> sheet.createRow(currentRow.getAndIncrement()), cfg);
        });

    }

    private static void setExcelRow(List<ResultInfo> infos, EntityResult internalRow, Supplier<Row> externalRowSupplier, PrintSettings settings){
        String[] ids = settings.getIdMapper().map(internalRow);


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
                resultInfo.getType().writeExcelCell(resultInfo,settings,dataCell, resultValue);
                currentColumn++;
            }
        }
    }
}
