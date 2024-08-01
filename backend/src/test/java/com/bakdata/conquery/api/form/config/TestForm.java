package com.bakdata.conquery.api.form.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;

public abstract class TestForm extends Form implements InternalForm {

	@Override
	public ManagedExecution toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new ManagedInternalForm<>(this, user, submittedDataset, storage);
	}

	@Override
	public Map<String, Query> createSubQueries() {
		return Collections.emptyMap();
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Collections.emptySet();
	}

	@Override
	public void resolve(QueryResolveContext context) {

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
		@Nullable
		@Override
		public JsonNode getValues() {
			return null;
		}
	}

	@CPSType(id = "TEST_FORM_REL_URL", base = QueryDescription.class)
	public static class Rel extends TestForm {
		@Nullable
		@Override
		public JsonNode getValues() {
			return null;
		}
	}
}
