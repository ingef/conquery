package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;

import java.util.List;
import java.util.stream.Stream;

public interface SingleTableResult {

	List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry);

	List<ResultInfo> getResultInfo();

	Stream<EntityResult> streamResults();

}
