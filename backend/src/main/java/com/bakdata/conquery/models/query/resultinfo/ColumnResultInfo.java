package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ColumnResultInfo extends ResultInfo {

	private final Column column;
	private final ResultType type;
	private final String description;
	private final Set<SemanticType> semantics;
	private final ResultPrinters.Printer printer;


	public ColumnResultInfo(Column column, ResultType type, Set<SemanticType> semantics, ResultPrinters.Printer printer, String description) {
		this.column = column;
		this.type = type;
		this.description = description;
		this.semantics = semantics;
		this.printer = printer;
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return null;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return column.getTable().getLabel() + " " + column.getLabel();
	}

	@Override
	public ColumnDescriptor asColumnDescriptor(PrintSettings settings, UniqueNamer collector) {
		return ColumnDescriptor.builder()
							   .label(defaultColumnName(settings))
							   .defaultLabel(getColumn().getLabel())
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(getDescription())
							   .build();
	}
}
