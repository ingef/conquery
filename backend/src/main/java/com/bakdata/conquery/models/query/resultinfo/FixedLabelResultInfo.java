package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Allows to generate result names, e.g. for CSV-headers, depending on the
 * provided locale.
 * The {@link FixedLabelResultInfo#localizedDefaultLabelProvider} is expected to
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
@ToString
public abstract class FixedLabelResultInfo extends ResultInfo {


	@Getter
	private final ResultType type;

	public FixedLabelResultInfo(ResultType type, Set<SemanticType> semantics) {
		super(semantics);
		this.type = type;
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		return printerFactory.printerFor(getType(), printSettings);
	}

	@Override
	public abstract String userColumnName(PrintSettings printSettings);

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return userColumnName(printSettings);
	}

	@Override
	public String getDescription() {
		return ""; // TODO what do? Localize description as well?
	}


}
