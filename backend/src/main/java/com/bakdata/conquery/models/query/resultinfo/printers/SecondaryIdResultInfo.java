package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.Set;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SecondaryIdResultInfo extends ResultInfo {
	private final SecondaryIdDescription secondaryId;
	private final ResultType type;
	private final Set<SemanticType> semantics;
	private final ResultPrinters.Printer printer;

	public SecondaryIdResultInfo(SecondaryIdDescription secondaryId) {
		this(secondaryId,ResultType.Primitive.STRING, Set.of(new SemanticType.SecondaryIdT(secondaryId)), secondaryId.getMapping() == null ? ResultPrinters::printString : new ResultPrinters.MappedPrinter(secondaryId.getMapping()));
	}

	@Override
	public String getDescription() {
		return secondaryId.getDescription();
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return secondaryId.getName();
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return secondaryId.getLabel();
	}

	@Override
	public ColumnDescriptor asColumnDescriptor(PrintSettings settings, UniqueNamer collector) {
		return ColumnDescriptor.builder()
							   .label(defaultColumnName(settings))
							   .defaultLabel(secondaryId.getLabel())
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(secondaryId.getDescription())
							   .build();
	}
}
