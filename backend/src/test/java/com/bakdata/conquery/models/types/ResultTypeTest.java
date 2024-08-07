package com.bakdata.conquery.models.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ExternalResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResultTypeTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	
	static {
		// Initialization of the internationalization
		I18n.init();
		//init global default config
		CONFIG.getPreprocessor().getParsers().setCurrency(Currency.getInstance("EUR"));
		CONFIG.getLocale().setDateFormatMapping(Map.of(Locale.GERMAN, "dd.MM.yyyy"));
	}

	private static final PrintSettings PRETTY = new PrintSettings(true, Locale.ENGLISH, null, CONFIG, null);
	private static final PrintSettings PRETTY_DE = new PrintSettings(true, Locale.GERMANY, null, CONFIG, null);
	private static final PrintSettings PLAIN = new PrintSettings(false, Locale.ENGLISH, null, CONFIG, null);

	public static ResultInfo info(ResultType type) {
		return new ExternalResultInfo("col", type);
	}

	@SuppressWarnings("unused")
	public static List<Arguments> testData() {
		return List.of(
			Arguments.of(PRETTY, info(ResultType.Primitive.BOOLEAN), true,	"Yes"),
			Arguments.of(PRETTY, info(ResultType.Primitive.BOOLEAN), false,	"No"),
			Arguments.of(PRETTY, info(ResultType.Primitive.STRING), "test", "test"),
			Arguments.of(PRETTY, ConqueryConstants.RESOLUTION_INFO, Resolution.COMPLETE.name(), "complete"),
			Arguments.of(PRETTY_DE, ConqueryConstants.RESOLUTION_INFO, Resolution.COMPLETE.name(), "Gesamt"),
			Arguments.of(PRETTY, info(ResultType.Primitive.DATE), LocalDate.of(2013, 7, 12).toEpochDay(), "2013-07-12"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.DATE), LocalDate.of(2013, 7, 12).toEpochDay(), "12.07.2013"),
			Arguments.of(PRETTY, info(ResultType.Primitive.DATE_RANGE), List.of(Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue(), Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue()), "2013-07-12"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.DATE_RANGE), List.of(Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue(), Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue()), "12.07.2013"),
			Arguments.of(PRETTY, info(ResultType.Primitive.DATE_RANGE), List.of(Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue(), Long.valueOf(LocalDate.of(2014, 7, 12).toEpochDay()).intValue()), "2013-07-12/2014-07-12"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.DATE_RANGE), List.of(Long.valueOf(LocalDate.of(2013, 7, 12).toEpochDay()).intValue(), Long.valueOf(LocalDate.of(2014, 7, 12).toEpochDay()).intValue()), "12.07.2013 - 12.07.2014"),
			Arguments.of(PRETTY, info(ResultType.Primitive.INTEGER), 51839274, "51,839,274"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.INTEGER), 51839274, "51.839.274"),
			Arguments.of(PRETTY, info(ResultType.Primitive.MONEY), 51839274L, "518,392.74"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.MONEY), 51839274L, "518.392,74"),
			Arguments.of(PRETTY, info(ResultType.Primitive.NUMERIC), 0.2, "0.2"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.NUMERIC), 0.2, "0,2"),
			Arguments.of(PRETTY, info(ResultType.Primitive.NUMERIC), new BigDecimal("716283712389817246892743124.12312"), "716,283,712,389,817,246,892,743,124.12312"),
			Arguments.of(PRETTY_DE, info(ResultType.Primitive.NUMERIC), new BigDecimal("716283712389817246892743124.12312"), "716.283.712.389.817.246.892.743.124,12312"),
			Arguments.of(PRETTY, info(ResultType.Primitive.STRING), "test", "test"),
			
			Arguments.of(PLAIN, info(ResultType.Primitive.BOOLEAN), true,	"1"),
			Arguments.of(PLAIN, info(ResultType.Primitive.BOOLEAN), false,	"0"),
			Arguments.of(PLAIN, info(ResultType.Primitive.STRING), "test", "test"),
			Arguments.of(PLAIN, info(ResultType.Primitive.DATE), LocalDate.of(2013, 7, 12).toEpochDay(), "2013-07-12"),
			Arguments.of(PLAIN, info(ResultType.Primitive.INTEGER), 51839274, "51839274"),
			Arguments.of(PLAIN, info(ResultType.Primitive.MONEY), 51839274L, "51839274"),
			Arguments.of(PLAIN, info(ResultType.Primitive.NUMERIC), 0.2, "0.2"),
			Arguments.of(PLAIN, info(ResultType.Primitive.NUMERIC), new BigDecimal("716283712389817246892743124.12312"), "716283712389817246892743124.12312"),
			Arguments.of(PLAIN, info(ResultType.Primitive.STRING), "test", "test"),
			Arguments.of(PLAIN, info(ResultType.Primitive.STRING), Resolution.COMPLETE.name(), "COMPLETE"), // TODO fk: is supposed not to test the mapping?
			Arguments.of(PLAIN, info(ResultType.Primitive.STRING), ImmutableMap.of("a", 2, "c", 1), "{a=2, c=1}")
		);
	}
	
	
	@ParameterizedTest(name="{0} {1}: {2} -> {3}") @MethodSource("testData")
	public void testPrinting(PrintSettings cfg, ResultInfo info, Object value, String expected) throws IOException {
		final ResultPrinters.Printer printer = info.getPrinter();

		assertThat(printer.print(value, cfg)).isEqualTo(expected);

		final String str = Jackson.MAPPER.writeValueAsString(value);
		final Object copy = Jackson.MAPPER.readValue(str, Object.class);

		assertThat(printer.print(copy, cfg)).isEqualTo(expected);
	}
	
	@ParameterizedTest(name="{1}: {2}") @MethodSource("testData")
	public void testBinaryPrinting(PrintSettings cfg, ResultInfo info, Object value, String expected) throws IOException {
		final ResultPrinters.Printer printer = info.getPrinter();

		assertThat(printer.print(value, cfg)).isEqualTo(expected);

		final byte[] bytes = Jackson.BINARY_MAPPER.writeValueAsBytes(value);
		final Object copy = Jackson.BINARY_MAPPER.readValue(bytes, Object.class);

		assertThat(printer.print(copy, cfg)).isEqualTo(expected);
	}
}
