package com.bakdata.conquery.models.query.resultinfo;

public interface SelectNameExtractor {
	String columnName(SelectResultInfo descriptor);
}