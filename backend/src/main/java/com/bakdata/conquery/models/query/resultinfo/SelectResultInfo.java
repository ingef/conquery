package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;
	
	public SelectResultInfo(Select select, CQConcept cqConcept) {
		this.select = select;
		this.cqConcept = cqConcept;
	}

	@Override
	public ResultType getType() {
		return select.getResultType();
	}

	@Override
	public String getName(PrintSettings settings) {
		return settings.columnName(this);
	}
}
