package com.bakdata.conquery.models.query.resultinfo;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import c10n.C10N;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Allows to generate result names, e.g. for CSV-headers, depending on the
 * provided locale.
 * The {@link LocalizedDefaultResultInfo#localizedDefaultLabelProvider} is expected to
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

	@NonNull
	private final Function<Locale, String> localizedLabelProvider;
	@NonNull
	private final Function<Locale, String> localizedDefaultLabelProvider;
	@Getter
	private final ResultType type;

	public LocalizedDefaultResultInfo(Function<Locale, String> localizedLabelProvider, ResultType type) {
		this(localizedLabelProvider,localizedLabelProvider, type);
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return localizedLabelProvider.apply(printSettings.getLocale());
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return localizedDefaultLabelProvider.apply(printSettings.getLocale());
	}

	@Override
	public Optional<Function<Object, Object>> getValueMapper() {
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "LocalizedDefaultResultInfo{" +
			   "localizedLabelProvider=" + localizedLabelProvider.apply(Locale.ROOT) +
			   ", localizedDefaultLabelProvider=" + localizedDefaultLabelProvider.apply(Locale.ROOT) +
			   ", type=" + type +
			   '}';
	}
}
