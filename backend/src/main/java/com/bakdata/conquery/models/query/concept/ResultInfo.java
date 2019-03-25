package com.bakdata.conquery.models.query.concept;

import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.Data;
import lombok.experimental.Wither;

@Data @Wither
public class ResultInfo {

	private final String name;
	private final ResultType type;
}
