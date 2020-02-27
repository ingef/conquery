package com.bakdata.conquery.apiv1.forms.export_form;

import java.util.List;
import java.util.function.Consumer;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.TimeSelector;
import com.bakdata.conquery.apiv1.forms.TimeUnit;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.export.RelExportGenerator;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter @Setter
@CPSType(id="RELATIVE", base=Mode.class)
public class RelativeMode extends Mode {
	@NotNull
	private TimeUnit timeUnit;
	@Min(0)
	private int timeCountBefore;
	@Min(0)
	private int timeCountAfter;
	@NotNull
	private IndexPlacement indexPlacement;
	@NotNull
	private TimeSelector indexSelector;
	@NotEmpty
	private List<CQElement> features;
	@NotEmpty
	private List<CQElement> outcomes;

//	@Override
//	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
//		return Collections.singletonList(
//			new RelExportGenerator(dataset, user, namespaces).execute(this, true)
//		);
//	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		features.forEach(e -> visitor.accept(e));
		outcomes.forEach(e -> visitor.accept(e));
	}
	
	@Override
	public RelativeFormQuery createSpecializedQuery(Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		return RelExportGenerator.generate(namespaces, this, userId, submittedDataset);
	}
}
