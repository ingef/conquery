package com.bakdata.conquery.apiv1.forms.export_form;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.models.forms.util.DateContextMode;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=QueryDescription.class)
public class ExportForm implements Form, NamespacedIdHolding {
	@NotNull
	private ManagedExecutionId queryGroup;
	@NotNull @Valid @JsonManagedReference
	private Mode timeMode;
	
	@NotNull @NotEmpty
	private List<DateContextMode> resolution = List.of(DateContextMode.COMPLETE);
	
	private boolean alsoCreateCoarserSubdivisions = true;

	@JsonIgnore
	private IQuery prerequisite;

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		timeMode.visit(visitor);
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> ids) {
		checkNotNull(ids);
		if(queryGroup != null) {
			ids.add(queryGroup);
		}
	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {
		return Map.of(
			ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
			List.of(
				timeMode.createSpecializedQuery(datasets, userId, submittedDataset)
					.toManagedExecution(datasets, userId, submittedDataset)));
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroup);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		timeMode.resolve(context);
		prerequisite = Form.resolvePrerequisite(context, queryGroup);
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(ExportFormC10n.class, I18n.LOCALE.get()).getType();
	}

}
