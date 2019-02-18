package com.bakdata.conquery.models.config;

import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LocaleConfig {
	@NotNull
	private CurrencyUnit currency = Monetary.getCurrency("EUR");
	@NotNull
	private Locale numberParsingLocale = Locale.ROOT;
	@NotNull
	private Locale frontend = Locale.ROOT;
}
