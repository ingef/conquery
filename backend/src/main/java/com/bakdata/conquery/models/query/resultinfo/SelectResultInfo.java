package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;

	@Override
	public ResultType getType() {
		return select.getResultType();
	}

	@Override
	public String getName(PrintSettings settings) {
		return settings.columnName(this);
	}
	
	@Override
	public ColumnDescriptor asColumnDescriptor(PrintSettings settings) {
		return ColumnDescriptor.builder()
			.label(getUniqueName(settings))
			.userConceptLabel(cqConcept.getLabel())
			.type(getType().toString())
			.selectId(select.getId())
			.build();
	}

	@Override
	public MajorTypeId getInternalType() {
		return select.getInternalType();
	}
}
