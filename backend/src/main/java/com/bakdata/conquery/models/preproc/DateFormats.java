package com.bakdata.conquery.models.preproc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;

public class DateFormats {

	private static ThreadLocal<DateFormats> INSTANCE = ThreadLocal.withInitial(() -> new DateFormats(ConqueryConfig.getInstance().getAdditionalFormats()));
	private static final LocalDate ERROR_DATE = LocalDate.MIN;
	private static final ConcurrentHashMap<String, LocalDate> DATE_CACHE = new ConcurrentHashMap<>(64000, 0.75f, 10);

	public static DateFormats instance() {
		return INSTANCE.get();
	}

	private final Set<DateTimeFormatter> formats = new HashSet<>();
	private DateTimeFormatter lastFormat;

	public DateFormats(String[] additionalFormats) {
		formats.add(toFormat("yyyy-MM-dd"));
		formats.add(toFormat("ddMMMyyyy"));
		formats.add(toFormat("yyyyMMdd"));
		for (String p : additionalFormats) {
			formats.add(toFormat(p));
		}
	}

	private DateTimeFormatter toFormat(String pattern) {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
	}

	private LocalDate tryParse(String value) {
		if (lastFormat != null) {
			try {
				return LocalDate.parse(value, lastFormat);
			}
			catch (DateTimeParseException e) {
				//intentionally left blank
			}
		}
		for (DateTimeFormatter format : formats) {
			if (lastFormat != format) {
				try {
					LocalDate res = LocalDate.parse(value, format);
					lastFormat = format;
					return res;
				}
				catch (DateTimeParseException e) {
					//intentionally left blank
				}
			}
		}
		return ERROR_DATE;
	}

	public boolean isValidDate(String value) {
		try {
			parseToLocalDate(value);
			return true;
		} catch (ParsingException e) {
			return false;
		}
	}

	public LocalDate parseToLocalDate(String value) throws ParsingException {
		try {
			LocalDate d = DATE_CACHE.computeIfAbsent(value, this::tryParse);
			
			if(DATE_CACHE.size()>64000) {
				Iterator<Entry<String, LocalDate>> it = DATE_CACHE.entrySet().iterator();
				it.next();
				it.remove();
			}
			
			if(d!=ERROR_DATE) {
				return d;
			}
			else {
				throw ParsingException.of(value, "date");
			}
		} catch(Exception e) {
			if(e instanceof ParsingException) {
				throw e;
			}
			else {
				throw ParsingException.of(value, "date", e);
			}
		}
	}
}
