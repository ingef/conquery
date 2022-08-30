package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
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
	private final Set<SemanticType> semantics;

	public ColumnResultInfo(Column column, ResultType type, Set<SemanticType> semantics) {
		this.column = column;
		this.type = type;
		this.description = column.getDescription();
		this.semantics = semantics;
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
