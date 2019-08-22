package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter @AllArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {
	private boolean prettyPrint = true;

	@Override
	public String columnName(SelectResultInfo info) {
		if(prettyPrint) {
			return info.getSelect().getLabel();
		}
		else {
			return info.getSelect().getId().toStringWithoutDataset();
		}
	}
}
