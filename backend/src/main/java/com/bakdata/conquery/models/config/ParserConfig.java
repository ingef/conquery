package com.bakdata.conquery.models.config;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;

@Data
@Setter
public class ParserConfig {
	/**
	 * Minimum required double precision to switch from floats to doubles.
	 * If set to a positive value, preprocessing will select float values for columns with at least minPrecision ulp.
	 * @see com.bakdata.conquery.models.preproc.parser.specific.RealParser#decideType()
	 * @see Math#ulp(float)
	 */
	private double minPrecision = Double.MIN_VALUE;

	/**
	 * The currency type of currency values that are parsed and processed. For now there can only be one currency in
	 * an instance.
	 */
	@NotNull
	private Currency currency = Currency.getInstance("EUR");

	/**
	 * Date formats that are available for parsing.
	 */
	@NotNull
	private DateFormats dateFormats = new DateFormats();
}
