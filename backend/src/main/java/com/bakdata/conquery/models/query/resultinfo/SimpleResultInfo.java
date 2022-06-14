package com.bakdata.conquery.models.query.resultinfo;

import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class SimpleResultInfo extends ResultInfo {

	private final String name;
	private final ResultType type;
	private final List<SemanticType> semantics;

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return null;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return name;
	}
}
