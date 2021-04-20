package com.bakdata.conquery.models.query;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Getter;
import lombok.ToString;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;

@Getter @ToString(onlyExplicitlyIncluded = true)
public class PrintSettings {

	private static final Function<Locale,NumberFormat> NUMBER_FORMAT = (locale) -> NumberFormat.getNumberInstance(locale);
	private static final Function<Locale,NumberFormat> DECIMAL_FORMAT = (locale) -> {
		NumberFormat fmt = NumberFormat.getNumberInstance(locale);
		fmt.setMaximumFractionDigits(Integer.MAX_VALUE);
		return fmt;
	};

	@ToString.Include
	private final boolean prettyPrint;
	@ToString.Include
	private final Locale locale;
	private final NumberFormat decimalFormat;
	private final NumberFormat integerFormat;
	private final Currency currency;

	/**
	 * Use the registry to resolve ids to objects/labels where this was not done yet, such as {@link CQConcept#getElements()}.
	 */
	private final DatasetRegistry datasetRegistry;

	private final Function<SelectResultInfo, String> columnNamer;

	private final String listElementDelimiter = ", ";
	private final String listElementEscaper = "\\";
	private final String listPrefix = "{";
	private final String listPostfix = "}";

	public PrintSettings(boolean prettyPrint, Locale locale, DatasetRegistry datasetRegistry, ConqueryConfig config, Function<SelectResultInfo, String> columnNamer) {
		this.prettyPrint = prettyPrint;
		this.locale = locale;
		this.datasetRegistry = datasetRegistry;
		this.currency = config.getPreprocessor().getParsers().getCurrency();

		this.columnNamer = columnNamer;

		this.integerFormat = NUMBER_FORMAT.apply(locale);
		this.decimalFormat = DECIMAL_FORMAT.apply(locale);
	}

	public PrintSettings(boolean prettyPrint, Locale locale, DatasetRegistry datasetRegistry,ConqueryConfig config) {
		this(prettyPrint, locale, datasetRegistry, config, null);
	}
}
