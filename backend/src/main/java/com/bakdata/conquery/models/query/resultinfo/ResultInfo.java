package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@Slf4j
public abstract class ResultInfo {

	public abstract String userColumnName(PrintSettings printSettings);

	/**
	 * Use default label schema which ignores user labels.
	 */
	public abstract String defaultColumnName(PrintSettings printSettings);

	@ToString.Include
	public abstract ResultType getType();

	@ToString.Include
	public abstract Set<SemanticType> getSemantics();

	public abstract String getDescription();

	public abstract ResultPrinters.Printer getPrinter();


	public ColumnDescriptor asColumnDescriptor(PrintSettings settings, UniqueNamer collector) {
		return ColumnDescriptor.builder()
							   .label(collector.getUniqueName(this))
							   .defaultLabel(defaultColumnName(settings))
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(getDescription())
							   .build();
	}

	public final String printNullable(PrintSettings printSettings, Object f) {
		if(f == null){
			return "";
		}

		return print(printSettings, f);
	}

	protected String print(PrintSettings printSettings, Object f) {
		return getPrinter().print(printSettings, f);
	}
}
