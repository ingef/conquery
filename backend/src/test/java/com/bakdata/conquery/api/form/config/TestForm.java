package com.bakdata.conquery.api.form.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;

public abstract class TestForm extends Form {

	@Override
	public ManagedExecution toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new ManagedInternalForm(this, user, submittedDataset, storage);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Collections.emptySet();
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset, MetaStorage storage) {
		return Collections.emptyMap();
	}

	@Override
	public String getLocalizedTypeLabel() {
		return null;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}

	@CPSType(id = "TEST_FORM_ABS_URL", base = QueryDescription.class)
	public static class Abs extends TestForm {
	}

	@CPSType(id = "TEST_FORM_REL_URL", base = QueryDescription.class)
	public static class Rel extends TestForm {
	}
}
