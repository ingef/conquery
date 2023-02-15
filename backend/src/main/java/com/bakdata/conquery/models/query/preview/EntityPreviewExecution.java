package com.bakdata.conquery.models.query.preview;

import java.util.ArrayList;
import java.util.List;
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
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.collect.MoreCollectors;

/**
 * Dedicated {@link ManagedExecution} to properly display/combine the two Queries submitted by {@link EntityPreviewForm}.
 * This mostly delegates to {@link EntityPreviewForm#VALUES_QUERY_NAME}, but embeds the result of {@link EntityPreviewForm#INFOS_QUERY_NAME} into {@link EntityPreviewStatus#getInfos()}.
 */
@CPSType(id = "ENTITY_PREVIEW_EXECUTION", base = ManagedExecution.class)
public class EntityPreviewExecution extends ManagedInternalForm<EntityPreviewForm> implements SingleTableResult {

	private PreviewConfig previewConfig;

	@Override
	public boolean isSystem() {
		// This Form should NEVER be started manually. Nor persisted
		return true;
	}

	protected EntityPreviewExecution(@JacksonInject(useInput = OptBoolean.FALSE) MetaStorage storage) {
		super(storage);
	}

	public EntityPreviewExecution(EntityPreviewForm entityPreviewQuery, User user, Dataset submittedDataset, MetaStorage storage) {
		super(entityPreviewQuery, user, submittedDataset, storage);
	}

	/**
	 * Takes a ManagedQuery, and transforms its result into a List of {@link EntityPreviewStatus.Info}.
	 * The format of the query is an {@link AbsoluteFormQuery} containing a single line for one person. This should correspond to {@link EntityPreviewForm#VALUES_QUERY_NAME}.
	 */
	private List<EntityPreviewStatus.Info> transformQueryResultToInfos(ManagedQuery infoCardExecution, Namespace namespace, ConqueryConfig config) {


		// Submitted Query is a single line of an AbsoluteFormQuery => MultilineEntityResult with a single line.
		final MultilineEntityResult result = (MultilineEntityResult) infoCardExecution.streamResults().collect(MoreCollectors.onlyElement());
		final Object[] values = result.getValues().get(0);

		final List<EntityPreviewStatus.Info> extraInfos = new ArrayList<>(values.length);
		final PrintSettings printSettings = new PrintSettings(true, I18n.LOCALE.get(), namespace, config, null, previewConfig::resolveSelectLabel);

		// We are only interested in the Select results.
		for (int index = AbsoluteFormQuery.FEATURES_OFFSET; index < infoCardExecution.getResultInfos().size(); index++) {
			final ResultInfo resultInfo = infoCardExecution.getResultInfos().get(index);

			final String printed = resultInfo.getType().printNullable(printSettings, values[index]);

			extraInfos.add(new EntityPreviewStatus.Info(
					resultInfo.userColumnName(printSettings),
					printed,
					resultInfo.getType().typeInfo(),
					resultInfo.getDescription(),
					resultInfo.getSemantics()
			));
		}

		return extraInfos;
	}

	@Override
	public void doInitExecutable() {
		super.doInitExecutable();
		previewConfig = getNamespace().getPreviewConfig();
	}

	/**
	 * Collects status of {@link EntityPreviewForm#getValuesQuery()} and {@link EntityPreviewForm#getInfoCardQuery()}.
	 * <p>
	 * Most importantly to {@link EntityPreviewStatus#setInfos(List)} to for infos of entity.
	 */
	@Override
	public FullExecutionStatus buildStatusFull(Subject subject) {

		initExecutable(getNamespace(), getConfig());

		final EntityPreviewStatus status = new EntityPreviewStatus();
		setStatusFull(status, subject);
		status.setQuery(getValuesQuery().getQuery());

		status.setInfos(transformQueryResultToInfos(getInfoCardExecution(), getNamespace(), getConfig()));

		return status;
	}

	@Override
	protected void setAdditionalFieldsForStatusWithColumnDescription(Subject subject, FullExecutionStatus status) {
		status.setColumnDescriptions(generateColumnDescriptions());
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
	public List<ColumnDescriptor> generateColumnDescriptions() {
		final List<ColumnDescriptor> descriptors = getValuesQuery().generateColumnDescriptions();

		for (ColumnDescriptor descriptor : descriptors) {
			if (descriptor.getSemantics()
						  .stream()
						  .anyMatch(semanticType -> semanticType instanceof SemanticType.SecondaryIdT desc
													&& previewConfig.isGroupingColumn(desc.getSecondaryId()))) {
				descriptor.getSemantics().add(new SemanticType.GroupT());
			}

			if (descriptor.getSemantics()
						  .stream()
						  .anyMatch(semanticType -> semanticType instanceof SemanticType.ColumnT desc && previewConfig.isHidden(desc.getColumn()))) {
				descriptor.getSemantics().add(new SemanticType.HiddenT());
			}
		}


		return descriptors;
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return getValuesQuery().getResultInfos();
	}

	@Override
	public Stream<EntityResult> streamResults() {
		return getValuesQuery().streamResults();
	}

	@Override
	public long resultRowCount() {
		return getValuesQuery().resultRowCount();
	}
}
