package com.bakdata.conquery.models.query;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.function.Function;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import lombok.Getter;
import lombok.ToString;

/**
 * @implNote eager cache everything here, this helps avoid mistakes when rendering values.
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class PrintSettings {

	private static final Function<Locale, NumberFormat> NUMBER_FORMAT = NumberFormat::getNumberInstance;
	private static final Function<Locale, NumberFormat> DECIMAL_FORMAT = (locale) -> {
		NumberFormat fmt = NumberFormat.getNumberInstance(locale);
		fmt.setMaximumFractionDigits(Integer.MAX_VALUE);
		return fmt;
	};

	private static final LocaleConfig.ListFormat UNPRETTY_LIST_FORMAT = new LocaleConfig.ListFormat("{", ",", "}");
	private static final String UNPRETTY_DATERANGE_SEPERATOR = "/";
	private static final DateTimeFormatter UNPRETTY_DATEFORMATTER = DateTimeFormatter.ISO_DATE;

	@ToString.Include
	private final boolean prettyPrint;
	@ToString.Include
	private final Locale locale;
	private final String dateFormat;
	private final DateTimeFormatter dateFormatter;
	private final NumberFormat decimalFormat;
	private final NumberFormat integerFormat;
	private final NumberFormat currencyFormat;
	private final Currency currency;

	private final Function<SelectResultInfo, String> columnNamer;

	private final String dateRangeSeparator;

	private final LocaleConfig.ListFormat listFormat;

	private final PrintIdMapper idMapper;

	public PrintSettings(
		boolean prettyPrint,
		Locale locale,
		ConqueryConfig config,
		PrintIdMapper idMapper,
		Function<SelectResultInfo, String> columnNamer) {
		this(
			prettyPrint,
			locale,
			config,
			idMapper,
			columnNamer,
			DECIMAL_FORMAT.apply(locale),
			NUMBER_FORMAT.apply(locale));
	}

	public PrintSettings(
		boolean prettyPrint,
		Locale locale,
		ConqueryConfig config,
		PrintIdMapper idMapper,
		Function<SelectResultInfo, String> columnNamer,
		NumberFormat decimalFormat,
		NumberFormat numberFormat) {
		this.prettyPrint = prettyPrint;
		this.locale = locale;
		this.currency = config.getPreprocessor().getParsers().getCurrency();
		this.columnNamer = columnNamer;
		this.idMapper = idMapper;

		this.integerFormat = numberFormat;
		this.decimalFormat = decimalFormat;

		this.listFormat = prettyPrint ? config.getLocale().getListFormats().getFirst() : UNPRETTY_LIST_FORMAT;
		this.dateRangeSeparator = prettyPrint ? config.getLocale()
			.findDateRangeSeparator(
				locale) : UNPRETTY_DATERANGE_SEPERATOR;

		this.dateFormat = config.getLocale().findDateFormat(locale);
		this.dateFormatter = prettyPrint ? DateTimeFormatter.ofPattern(dateFormat) : UNPRETTY_DATEFORMATTER;

		this.currencyFormat = DecimalFormat.getCurrencyInstance(locale);
		currencyFormat.setCurrency(currency);
		currencyFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
	}


	/**
	 * @implNote We are cloning, because {@link NumberFormat} is NOT thread safe.
	 */
	public NumberFormat getIntegerFormat() {
		return (NumberFormat) integerFormat.clone();
	}

	/**
	 * @implNote We are cloning, because {@link NumberFormat} is NOT thread safe.
	 */
	public DecimalFormat getCurrencyFormat() {
		return (DecimalFormat) currencyFormat.clone();
	}

	/**
	 * @implNote We are cloning, because {@link NumberFormat} is NOT thread safe.
	 */
	public NumberFormat getDecimalFormat() {
		return (NumberFormat) decimalFormat.clone();
	}
}
