package com.bakdata.conquery.models.query.concept;

import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.Data;

@Data
public class ResultInfo {

	private final String name;
	private final ResultType type;
}
