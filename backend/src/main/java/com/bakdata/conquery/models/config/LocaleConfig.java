package com.bakdata.conquery.models.config;

import java.util.Currency;
import java.util.Locale;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LocaleConfig {
	@NotNull
	private Locale frontend = Locale.ROOT;
}
