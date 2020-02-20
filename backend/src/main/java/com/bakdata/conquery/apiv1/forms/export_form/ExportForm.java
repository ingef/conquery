package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=Form.class)
public class ExportForm extends Form implements Visitable, NamespacedIdHolding {
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
//	@Override
//	public Collection<ManagedExecutionId> getUsedQueries() {
//		return Arrays.asList(queryGroup);
//	}

//	@Override
//	public Map<String, List<ManagedQuery>> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
//		return
//			ImmutableMap.of(SINGLE_RESULT_TABLE_POSTFIX,timeMode.executeQuery(dataset, user, namespaces));
//	}

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

	@Override
	public ManagedExecution toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		return new ManagedForm(storage, timeMode.createSpecializedQuery(namespaces), userId, submittedDataset);
	}

}
