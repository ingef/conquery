package com.bakdata.conquery.models.query.resultinfo;

import java.util.Locale;
import java.util.function.Function;

import c10n.C10N;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Allows to generate result names, e.g. for CSV-headers, depending on the
 * provided locale.
 * The {@link LocalizedDefaultResultInfo#localizedDefaultProvider} is expected to
 * use {@link C10N} (Cosmopolitan) like this:
 * <pre>
 *  (locale) -> C10N.get(ExampleC10n.class, locale).example()
 * </pre>
 * Where the example class might look like this.
 * <pre>
 *  import c10n.annotations.*;
 *  
 *  public interface ExampleC10n {
 *  	
 *  	&#064;En("example")
 *  	&#064;De("Beispiel")
 *  	String example();
 *  }
 * </pre>
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LocalizedDefaultResultInfo extends ResultInfo {

	private final String userLabel;
	private final Function<Locale, String> localizedDefaultProvider;
	@Getter
	private final ResultType type;

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return userLabel;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return localizedDefaultProvider.apply(printSettings.getLocale());
	}
}
