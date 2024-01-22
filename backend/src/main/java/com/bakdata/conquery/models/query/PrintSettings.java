package com.bakdata.conquery.models.query;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.function.Function;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
	private final Currency currency;

	/**
	 * Use the registry to resolve ids to objects/labels where this was not done yet, such as {@link CQConcept#getElements()}.
	 */
	private final Namespace namespace;

	private final Function<SelectResultInfo, String> columnNamer;

	private final String dateRangeSeparator;

	private final LocaleConfig.ListFormat listFormat;

	private final PrintIdMapper idMapper;

	public PrintSettings(boolean prettyPrint, Locale locale, Namespace namespace, ConqueryConfig config, PrintIdMapper idMapper, Function<SelectResultInfo, String> columnNamer) {
		this.prettyPrint = prettyPrint;
		this.locale = locale;
		this.namespace = namespace;
		this.currency = config.getPreprocessor().getParsers().getCurrency();
		this.columnNamer = columnNamer;
		this.idMapper = idMapper;

		this.integerFormat = NUMBER_FORMAT.apply(locale);
		this.decimalFormat = DECIMAL_FORMAT.apply(locale);

		this.listFormat = prettyPrint ? config.getLocale().getListFormats().get(0) : UNPRETTY_LIST_FORMAT;
		this.dateRangeSeparator = prettyPrint ? config.getLocale().findDateRangeSeparator(locale) : UNPRETTY_DATERANGE_SEPERATOR;

		this.dateFormat = config.getLocale().findDateFormat(locale);
		this.dateFormatter = prettyPrint ? DateTimeFormatter.ofPattern(dateFormat) : UNPRETTY_DATEFORMATTER;

	}

	public PrintSettings(boolean prettyPrint, Locale locale, Namespace namespace, ConqueryConfig config, PrintIdMapper idMapper) {
		this(prettyPrint, locale, namespace, config, idMapper, null);
	}

}
