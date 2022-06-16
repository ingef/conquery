package com.bakdata.conquery.models.query.resultinfo;

import java.util.Optional;
import java.util.function.Function;

import java.util.Collections;
import java.util.Set;
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
	private final Set<SemanticType> semantics;

	public SimpleResultInfo(String name, ResultType type) {
		this(name, type, Collections.emptySet());
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return null;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return name;
	}
}
