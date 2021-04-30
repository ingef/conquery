package com.bakdata.conquery.models.config;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public class ExcelConfig {

	public static String HEADER_STYLE = "header";
	public static String DATE_STYLE = "date";
	public static String CURRENCY_STYLE_PREFIX = "currency_";
	private static final String BASIC_STYLE = "basic";

	private static final Map<String, CellStyler> FALLBACK_STYLES = Map.of(
			BASIC_STYLE, new CellStyler(),
			HEADER_STYLE, new CellStyler()
							.withBold(true)
							.withFillPattern(FillPatternType.SOLID_FOREGROUND)
							.withForegroundColors(IndexedColors.LIGHT_BLUE.getIndex()),
			CURRENCY_STYLE_PREFIX+"EUR", new CellStyler().withDataFormatString("#,##0.00 €"),
			DATE_STYLE, new CellStyler().withDataFormatString("yyyy-mm-dd")
	);

	/**
	 * User provided styles. Currently existing styles such as HEADER_STYLE, DATE_STYLE and BASIC_STYLE
	 * can be overridden. Styles for specific currencies might be added by prefixing the currency identifier
	 * with "currency_".
	 */
	private Map<String, CellStyler> styles = Collections.emptyMap();


	public ImmutableMap<String, CellStyle> generateStyles(XSSFWorkbook workbook){
		ImmutableMap.Builder<String, CellStyle> styles = ImmutableMap.builder();

		// Build configured styles
		for (Map.Entry<String, CellStyler> entry : this.styles.entrySet()) {
			styles.put(entry.getKey(), entry.getValue().generateStyle(workbook));
		}

		// Add missing basic styles
		for (String s : FALLBACK_STYLES.keySet()) {
			if(this.styles.containsKey(s)){
				continue;
			}
			styles.put(s, FALLBACK_STYLES.get(s).generateStyle(workbook));
		}

		return styles.build();
	}

	@NoArgsConstructor
	@AllArgsConstructor
	@With
	private static class CellStyler {

		@NotBlank
		private String font = "Arial";
		@Min(1)
		private short fontHeightInPoints = 16;
		private boolean bold = false;
		@NotNull
		private FillPatternType fillPattern = FillPatternType.NO_FILL;
		@Min(0)
		private short foregroundColors = IndexedColors.BLACK.getIndex();

		private String dataFormatString;

		private CellStyle generateStyle(XSSFWorkbook workbook) {
			XSSFDataFormat dataFormat = workbook.createDataFormat();
			CellStyle style = workbook.createCellStyle();
			style.setFillForegroundColor(foregroundColors);
			style.setFillPattern(fillPattern);

			XSSFFont xFont = workbook.createFont();
			xFont.setFontName(font);
			xFont.setFontHeightInPoints(fontHeightInPoints);
			xFont.setBold(bold);
			style.setFont(xFont);

			if (dataFormatString != null){
				style.setDataFormat(dataFormat.getFormat(dataFormatString));
			}

			return style;
		}
	}
}
