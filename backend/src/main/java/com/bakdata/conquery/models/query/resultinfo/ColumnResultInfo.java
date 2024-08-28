package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ColumnResultInfo extends ResultInfo {

	private final Column column;
	private final ResultType type;
	private final String description;
	private final Printer printer;


	public ColumnResultInfo(Column column, ResultType type, Set<SemanticType> semantics, Printer printer, String description, PrintSettings settings) {
		super(semantics, settings);
		this.column = column;
		this.type = type;
		this.description = description;
		this.printer = printer;
	}

	@Override
	public String userColumnName() {
		return column.getTable().getLabel() + " " + column.getLabel();
	}

	@Override
	public String defaultColumnName() {
		return userColumnName();
	}

}
