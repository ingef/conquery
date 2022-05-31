package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.arx.ArxExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@CPSType(id = "ARX_FORM", base = QueryDescription.class)
public class ArxForm extends Form {


	@NotNull
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		return Map.of(ConqueryConstants.SINGLE_RESULT_TABLE_NAME, List.of(queryGroup));
	}

	@Override
	public String getLocalizedTypeLabel() {
		return "ARX Anonymization";
	}

	@Override
	public ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset) {
		return new ArxExecution(this, user, submittedDataset);
	}

	@Override
	public Set<ManagedExecution<?>> collectRequiredQueries() {
		return Set.of(queryGroup);
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
