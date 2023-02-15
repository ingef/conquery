package com.bakdata.conquery.models.query;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SingleTableResult {

	List<ColumnDescriptor> generateColumnDescriptions();

	@JsonIgnore
	List<ResultInfo> getResultInfos();

	Stream<EntityResult> streamResults();

	@JsonIgnore
	long resultRowCount();

}
