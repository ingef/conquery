package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.mostlyai.MostlyAiExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Form that forwards the results of the referenced {@link ManagedQuery} to the
 * synthetization service of Mostly-AI. (see {@link MostlyAiExecution})
 * <p>
 * The result of this form is the synthesized data provided in a
 * zipped folder as it is provided by the service.
 */
@Getter
@CPSType(id = "MOSTLY_AI_FORM", base = QueryDescription.class)
@EqualsAndHashCode(callSuper = true)
public class MostlyAiForm extends Form {


	@NotNull
	@JsonProperty("queryGroup")
	@Setter(AccessLevel.PRIVATE)
	private ManagedExecutionId queryGroupId;


	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private ManagedQuery queryGroup;

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset, MetaStorage storage) {
		return Map.of(ConqueryConstants.SINGLE_RESULT_TABLE_NAME, List.of(queryGroup));
	}

	@Override
	public String getLocalizedTypeLabel() {
		return "Mostly AI Synthetization";
	}

	@Override
	public MostlyAiExecution toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new MostlyAiExecution(this, user, submittedDataset, storage);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryGroupId);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
