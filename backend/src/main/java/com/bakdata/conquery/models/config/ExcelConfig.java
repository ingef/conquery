package com.bakdata.conquery.models.config;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

@Data
public class ExcelConfig {

	public static String DATE_STYLE = "date";
	public static String CURRENCY_STYLE_PREFIX = "currency_";
	private static final String BASIC_STYLE = "basic";
	public static final String NUMERIC_STYLE = "numeric";
	public static final String INTEGER_STYLE = "integer";

	private static final Map<String, CellStyler> FALLBACK_STYLES = Map.of(
			BASIC_STYLE, new CellStyler(),
			CURRENCY_STYLE_PREFIX+"EUR", new CellStyler().withDataFormatString("#,##0.00 €"),
			NUMERIC_STYLE, new CellStyler().withDataFormatString("#,##0.00"),
			INTEGER_STYLE, new CellStyler().withDataFormatString("#,###"),
			DATE_STYLE, new CellStyler().withDataFormatString("yyyy-mm-dd")
	);

	/**
	 * User provided styles. Currently existing styles such as NUMERIC_STYLE, DATE_STYLE and BASIC_STYLE
	 * can be overridden. Styles for specific currencies might be added by prefixing the currency identifier
	 * with "currency_".
	 */
	private Map<String, CellStyler> styles = Collections.emptyMap();

	/**
	 * The column width in characters. See {@link SXSSFSheet#getDefaultColumnWidth()}.
	 */
	@Min(1)
	private int defaultColumnWidth = 30;


	public ImmutableMap<String, CellStyle> generateStyles(SXSSFWorkbook workbook){
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
		private String font = null;
		@Min(1)
		private Short fontHeightInPoints = null;
		private Boolean bold = null;
		@NotNull
		private FillPatternType fillPattern = null;
		@Min(0)
		private Short foregroundColors = null;

		private String dataFormatString = null;

		private CellStyle generateStyle(SXSSFWorkbook workbook) {
			DataFormat dataFormat = workbook.createDataFormat();
			CellStyle style = workbook.createCellStyle();
			if (fillPattern != null){
				style.setFillPattern(fillPattern);
			}
			if (foregroundColors != null) {
				style.setFillForegroundColor(foregroundColors);
			}

			if (dataFormatString != null){
				style.setDataFormat(dataFormat.getFormat(dataFormatString));
			}

			if (fontHeightInPoints != null || bold != null) {
				Font xFont = workbook.createFont();
				xFont.setFontName(font);
				if (fontHeightInPoints != null) {
					xFont.setFontHeightInPoints(fontHeightInPoints);
				}
				if (bold != null) {
					xFont.setBold(bold);
				}
				style.setFont(xFont);
			}

			return style;
		}
	}
}
