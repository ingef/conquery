package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.PrintSettings;
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
	private final SimpleResultType type;

	@Override
	public String getName(PrintSettings settings) {
		return name;
	}
}
