package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.externalservice.ResultType;
import lombok.Getter;

@Getter
public class SimpleResultInfo extends ResultInfo {
	public SimpleResultInfo(String name, ResultType type) {
		this.name = name;
		this.type = type;
	}

	private final String name;
	private final ResultType type;
}
