package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;
	@NonNull
	private final String name;
	
	public SelectResultInfo(PrintSettings settings, Select select, CQConcept cqConcept) {
		this.select = select;
		this.cqConcept = cqConcept;
		this.name = settings.columnName(this);
	}

	@Override
	public ResultType getType() {
		return select.getResultType();
	}
}
