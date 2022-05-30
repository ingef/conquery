package com.bakdata.conquery.models.forms.arx;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;

public class ArxForm extends Form {
	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		return null;
	}

	@Override
	public String getLocalizedTypeLabel() {
		return null;
	}

	@Override
	public ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset) {
		return null;
	}

	@Override
	public Set<ManagedExecution<?>> collectRequiredQueries() {
		return null;
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}
}
