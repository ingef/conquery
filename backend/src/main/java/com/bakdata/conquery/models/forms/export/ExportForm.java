package com.bakdata.conquery.models.forms.export;

import static com.bakdata.conquery.ConqueryConstants.SINGLE_RESULT_TABLE_POSTFIX;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=Form.class)
public class ExportForm extends Form implements Visitable, NamespacedIdHolding {
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
	@Override
	public Collection<ManagedExecutionId> getUsedQueries() {
		return Arrays.asList(queryGroup);
	}

	@Override
	protected String[] getAdditionalHeader() {
		return timeMode.getAdditionalHeader();
	}

	@Override
	public Map<String, List<ManagedQuery>> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		return
			ImmutableMap.of(SINGLE_RESULT_TABLE_POSTFIX,timeMode.executeQuery(dataset, user, namespaces));
	}

	@Override
	public void init(Namespaces namespaces, User user) {
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		timeMode.visit(visitor);
	}

	@Override
	public Set<NamespacedId> collectNamespacedIds() {
		return Set.of(queryGroup);
	}

}
