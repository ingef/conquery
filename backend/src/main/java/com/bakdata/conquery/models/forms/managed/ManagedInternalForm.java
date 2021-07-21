package com.bakdata.conquery.models.forms.managed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *	Execution type for simple forms, that are completely executed within Conquery and produce a single table as result.
 */
@CPSType(base = ManagedExecution.class, id = "INTERNAL_FORM")
public class ManagedInternalForm extends ManagedForm implements SingleTableResult {


	public ManagedInternalForm(Form form, User user, Dataset submittedDataset) {
		super(form, user, submittedDataset);
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry) {
		return getSubQueries().values().iterator().next().get(0).generateColumnDescriptions(datasetRegistry);
	}

	@Override
	@JsonIgnore
	public List<ResultInfo> getResultInfo() {
		if(getSubQueries().size() != 1) {
			throw new UnsupportedOperationException("Cannot gather result info when multiple tables are generated");
		}
		return getSubQueries().values().iterator().next().get(0).getResultInfo();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		if(subQueries.size() != 1) {
			// Get the query, only if there is only one query set in the whole execution
			throw new UnsupportedOperationException("Cannot return the result query of a multi query form");
		}
		return subQueries.values().iterator().next().stream().flatMap(ManagedQuery::streamResults);
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), getFlatSubQueries().entrySet().stream()
														   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
	}
}
