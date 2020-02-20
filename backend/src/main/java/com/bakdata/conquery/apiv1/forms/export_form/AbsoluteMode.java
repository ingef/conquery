package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.forms.export.AbsExportGenerator;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter @Setter
@CPSType(id="ABSOLUTE", base=Mode.class)
public class AbsoluteMode extends Mode {
	@NotNull @Valid
	private Range<LocalDate> dateRange;

	@NotEmpty
	private List<CQElement> features;

//	@Override
//	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
//		return Collections.singletonList(
//			new AbsExportGenerator(dataset, user, namespaces)
//				.executeQuery(this, DateContextMode.QUARTER_WISE)
//		);
//	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		features.forEach(e -> visitor.accept(e));
	}

	@Override
	public List<IQuery> createSpecializedQuery(Namespaces namespaces) {
		return List.of(AbsExportGenerator.generate(namespaces, this, DateContextMode.QUARTER_WISE));
	}
}
