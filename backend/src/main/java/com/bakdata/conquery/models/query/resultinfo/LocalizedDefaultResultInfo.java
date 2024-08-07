package com.bakdata.conquery.models.query.resultinfo;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.CheckForNull;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

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
@EqualsAndHashCode(callSuper = true)
public class LocalizedDefaultResultInfo extends ResultInfo {

	@NonNull
	private final Function<PrintSettings, String> localizedLabelProvider;
	@NonNull
	private final Function<PrintSettings, String> localizedDefaultLabelProvider;
	@Getter
	private final ResultType type;

	@Getter
	private final ResultPrinters.Printer printer;

	public LocalizedDefaultResultInfo(@NonNull Function<PrintSettings, String> localizedLabelProvider, @CheckForNull Function<PrintSettings, String> localizedDefaultLabelProvider, ResultType type, @CheckForNull ResultPrinters.Printer printer, Set<SemanticType> semantics) {
		super(semantics);
		this.localizedLabelProvider = localizedLabelProvider;
		this.localizedDefaultLabelProvider = Objects.requireNonNullElse(localizedDefaultLabelProvider, localizedLabelProvider);
		this.type = type;
		this.printer = Objects.requireNonNullElse(printer, ResultPrinters.defaultPrinter(type));
	}


	@Override
	public String userColumnName(PrintSettings printSettings) {
		return localizedLabelProvider.apply(printSettings);
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return localizedDefaultLabelProvider.apply(printSettings);
	}

	@Override
	public String getDescription() {
		return ""; // TODO what do? Localize description as well?
	}



	//	TODO @Override
//	public String toString() {
//		return "LocalizedDefaultResultInfo{" +
//			   "localizedLabelProvider=" + localizedLabelProvider.apply(Locale.ROOT) +
//			   ", localizedDefaultLabelProvider=" + localizedDefaultLabelProvider.apply(Locale.ROOT) +
//			   ", type=" + type +
//			   '}';
//	}
}
