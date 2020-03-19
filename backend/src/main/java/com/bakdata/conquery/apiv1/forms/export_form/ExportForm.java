package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=QueryDescription.class)
public class ExportForm extends Form implements NamespacedIdHolding {
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
	@NotNull @NotEmpty
	private List<DateContextMode> resolution = List.of(DateContextMode.COMPLETE);
	
	private boolean alsoCreateCoarserSubdivisions = true;

	@Override
	public void visit(Consumer<Visitable> visitor) {
		timeMode.visit(visitor);
	}

	@Override
	public Set<NamespacedId> collectNamespacedIds() {
		return Set.of(queryGroup);
	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		return Map.of(
			ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
			List.of(
				timeMode.createSpecializedQuery(namespaces, userId, submittedDataset)
					.toManagedExecution(namespaces, userId, submittedDataset)));
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroup);
	}
	
	@ValidationMethod(message = "Abort Form creation, because coarser subdivision are requested and multiple (or none) resolutions are given. With 'alsoCreateCoarserSubdivisions' set to true, provide only one resolution.")
	public boolean isValidResolutionSetting() {
		if(alsoCreateCoarserSubdivisions && resolution.size() != 1) {
			return false;
		}
		return true;
	}

}
