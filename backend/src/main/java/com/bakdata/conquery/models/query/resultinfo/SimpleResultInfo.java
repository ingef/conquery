package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;

@Getter
public class SimpleResultInfo extends ResultInfo {
	public SimpleResultInfo(PrintSettings settings, String name, ResultType type) {
		super(settings);
		this.name = name;
		this.type = type;
	}

	private final String name;
	private final ResultType type;
}
