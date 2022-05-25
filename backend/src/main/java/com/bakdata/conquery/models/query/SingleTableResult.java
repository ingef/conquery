package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.result.arrow.ArrowUtil;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.types.pojo.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface SingleTableResult {

	List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry);

	@JsonIgnore
	List<ResultInfo> getResultInfos();

	Stream<EntityResult> streamResults();



	default Schema getSchema(PrintSettings printSettings) {
		return new Schema(getResultInfos().stream().map(i -> ArrowUtil.createField(i, new UniqueNamer(printSettings))).collect(Collectors.toList()), null);
	}

	void addShardBatch(ArrowRecordBatch recordBatch);
}
