package com.bakdata.conquery.models.query.resultinfo;

import java.util.Locale;
import java.util.function.Function;

import c10n.C10N;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Allows to generate result names, e.g. for CSV-headers, depending on the
 * provided locale.
 * The {@link LocalizedSimpleResultInfo#localizedNameProvider} is expected to
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
public class LocalizedSimpleResultInfo extends ResultInfo {
	
	private final Function<Locale, String> localizedNameProvider;
	@Getter
	private final SimpleResultType type;

	@Override
	protected String getName(PrintSettings settings) {
		return localizedNameProvider.apply(settings.getLocale());
	}
}
