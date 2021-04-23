package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public class ExcelRenderer {

    public static void renderToStream(
            PrintSettings cfg,
            String[] idHeaders,
            ManagedExecution<?> exec) throws IOException {
        List<ResultInfo> infos = ManagedExecution.getResultInfos(exec);

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Persons");

        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);

    }

    private static void writeHeader(
            Sheet sheet,
            XSSFWorkbook workbook,
            String[] idHeaders,
            List<ResultInfo> infos,
            PrintSettings cfg){


        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

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
            PrintSettings cfg) {

    }
}
