package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
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
public class FixedLabelResultInfo extends ResultInfo {

	@NonNull
	private final String localizedLabel;
	@NonNull
	private final String localizedDefaultLabel;
	@Getter
	private final ResultType type;

	@Override
	public Printer getPrinter() {
		return getSettings().getPrinterFactory().printerFor(getType(), getSettings());
	}

	public FixedLabelResultInfo(String label, String defaultLabel, ResultType type, Set<SemanticType> semantics, PrintSettings settings) {
		super(semantics, settings);
		this.localizedLabel = label;
		this.localizedDefaultLabel = defaultLabel;
		this.type = type;
	}


	@Override
	public String userColumnName() {
		return localizedLabel;
	}

	@Override
	public String defaultColumnName() {
		return localizedDefaultLabel;
	}

	@Override
	public String getDescription() {
		return ""; // TODO what do? Localize description as well?
	}


}
