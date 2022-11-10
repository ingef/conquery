package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.stream.Stream;

public interface SingleTableResult {

	List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry);

	@JsonIgnore
	List<ResultInfo> getResultInfos();

	Stream<EntityResult> streamResults();

	@JsonIgnore
	long resultRowCount();

}
