package com.bakdata.conquery.models.externalservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.PrintSettings;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResultTypeTest {
	
	static {
		// Initialization of the internationalization
		I18n.init();
	}

	private static final PrintSettings PRETTY = new PrintSettings(true, Locale.ENGLISH, null);
	private static final PrintSettings PRETTY_DE = new PrintSettings(true, Locale.GERMAN, null);
	private static final PrintSettings PLAIN = new PrintSettings(false, Locale.ENGLISH, null);
	
	public static Stream<Arguments> testData() {
		//init global default config
		ConqueryConfig cfg = new ConqueryConfig();
		cfg.getLocale().setNumberParsingLocale(Locale.ROOT);
		cfg.getLocale().setCurrency(Currency.getInstance("EUR"));
		return Stream.of(
			Arguments.of(PRETTY, ResultType.BOOLEAN, true,	"t"),
			Arguments.of(PRETTY, ResultType.BOOLEAN, false,	"f"),
			Arguments.of(PRETTY, ResultType.CATEGORICAL, "test", "test"),
			Arguments.of(PRETTY, ResultType.RESOLUTION, DateContextMode.COMPLETE, "complete"),
			Arguments.of(PRETTY_DE, ResultType.RESOLUTION, DateContextMode.COMPLETE, "Gesamt"),
			Arguments.of(PRETTY, ResultType.DATE, LocalDate.of(2013, 07, 12), "2013-07-12"),
			Arguments.of(PRETTY, ResultType.INTEGER, 51839274, "51,839,274"),
			Arguments.of(PRETTY, ResultType.MONEY, 51839274L, "518,392.74"),
			Arguments.of(PRETTY, ResultType.NUMERIC, 0.2, "0.2"),
			Arguments.of(PRETTY, ResultType.NUMERIC, new BigDecimal("716283712389817246892743124.12312"), "716,283,712,389,817,246,892,743,124.12312"),
			Arguments.of(PRETTY, ResultType.STRING, "test", "test"),
			
			Arguments.of(PLAIN, ResultType.BOOLEAN, true,	"t"),
			Arguments.of(PLAIN, ResultType.BOOLEAN, false,	"f"),
			Arguments.of(PLAIN, ResultType.CATEGORICAL, "test", "test"),
			Arguments.of(PLAIN, ResultType.DATE, LocalDate.of(2013, 07, 12), "2013-07-12"),
			Arguments.of(PLAIN, ResultType.INTEGER, 51839274, "51839274"),
			Arguments.of(PLAIN, ResultType.MONEY, 51839274L, "51839274"),
			Arguments.of(PLAIN, ResultType.NUMERIC, 0.2, "0.2"),
			Arguments.of(PLAIN, ResultType.NUMERIC, new BigDecimal("716283712389817246892743124.12312"), "716283712389817246892743124.12312"),
			Arguments.of(PLAIN, ResultType.STRING, "test", "test"),
			Arguments.of(PLAIN, ResultType.CATEGORICAL, DateContextMode.COMPLETE, "COMPLETE"),
			Arguments.of(PLAIN, ResultType.STRING, ImmutableMap.of("a", 2, "c", 1), "{a=2, c=1}")
		);
	}
	
	
	@ParameterizedTest(name="{0} {1}: {2}") @MethodSource("testData")
	public void testPrinting(PrintSettings cfg, ResultType type, Object value, String expected) throws IOException {
		assertThat(type.printNullable(cfg, value)).isEqualTo(expected);
		String str = Jackson.MAPPER.writeValueAsString(value);
		Object copy = Jackson.MAPPER.readValue(str, Object.class);
		assertThat(type.printNullable(cfg, copy)).isEqualTo(expected);
	}
	
	@ParameterizedTest(name="{1}: {2}") @MethodSource("testData")
	public void testBinaryPrinting(PrintSettings cfg, ResultType type, Object value, String expected) throws IOException {
		assertThat(type.printNullable(cfg, value)).isEqualTo(expected);
		byte[] bytes = Jackson.BINARY_MAPPER.writeValueAsBytes(value);
		Object copy = Jackson.BINARY_MAPPER.readValue(bytes, Object.class);
		assertThat(type.printNullable(cfg, copy)).isEqualTo(expected);
	}
}
