package com.bakdata.conquery.io.result;

import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;

public record ResultExportContext(
		ManagedExecution execution,
		SingleTableResult result,
		Subject subject,
		PrintSettings printSettings,
		List<ResultInfo> idResultInfos,
		List<ResultInfo> resultInfos,
		OptionalLong limit) {

	public ResultExportContext {
		idResultInfos = List.copyOf(idResultInfos);
		resultInfos = List.copyOf(resultInfos);
	}

	public Stream<EntityResult> streamResults() {
		return result.streamResults(limit);
	}
}
