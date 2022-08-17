package com.bakdata.conquery.models.query.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import lombok.NonNull;

@CPSType(id = "ENTITY_PREVIEW_EXECUTION", base = ManagedExecution.class)
public class EntityPreviewExecution extends ManagedForm implements SingleTableResult {

	public EntityPreviewExecution(EntityPreviewForm entityPreviewQuery, User user, Dataset submittedDataset) {
		super(entityPreviewQuery, user, submittedDataset);
	}

	private List<EntityPreviewStatus.Info> transformQueryResultToInfos(ManagedQuery infoCardExecution, DatasetRegistry datasetRegistry, ConqueryConfig config) {
		final MultilineEntityResult result = (MultilineEntityResult) infoCardExecution.streamResults().collect(MoreCollectors.onlyElement());
		final Object[] values = result.getValues().get(0);

		List<EntityPreviewStatus.Info> extraInfos = new ArrayList<>(values.length);
		PrintSettings printSettings = new PrintSettings(true, Locale.getDefault(), datasetRegistry, config, null);


		for (int index = AbsoluteFormQuery.FEATURES_OFFSET; index < infoCardExecution.getResultInfos().size(); index++) {
			ResultInfo resultInfo = infoCardExecution.getResultInfos().get(index);
			String printed = resultInfo.getType().printNullable(printSettings, values[index]);

			extraInfos.add(new EntityPreviewStatus.Info(resultInfo.defaultColumnName(printSettings), printed));
		}
		return extraInfos;
	}

	@Override
	public FullExecutionStatus buildStatusFull(@NonNull MetaStorage storage, Subject subject, DatasetRegistry datasetRegistry, ConqueryConfig config) {

		initExecutable(datasetRegistry, config);

		EntityPreviewStatus status = new EntityPreviewStatus();
		setStatusFull(status, storage, subject, datasetRegistry);

		status.setInfos(transformQueryResultToInfos(getInfoCardExecution(), datasetRegistry, config));
		status.setQuery(null);

		return status;
	}

	@JsonIgnore
	private ManagedQuery getInfoCardExecution() {
		return getSubQueries().get(EntityPreviewForm.INFOS_QUERY_NAME).get(0);
	}

	@JsonIgnore
	private ManagedQuery getValuesQuery() {
		return getSubQueries().get(EntityPreviewForm.VALUES_QUERY_NAME).get(0);
	}


	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), getFlatSubQueries().entrySet().stream()
														   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
	}

	@Override
	public List<ColumnDescriptor> generateColumnDescriptions(DatasetRegistry datasetRegistry) {
		return getValuesQuery().generateColumnDescriptions(datasetRegistry);
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return getValuesQuery().getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return getValuesQuery().streamResults();
	}
}
