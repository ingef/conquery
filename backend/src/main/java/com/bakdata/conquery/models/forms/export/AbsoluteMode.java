package com.bakdata.conquery.models.forms.export;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.models.forms.DateContextMode;
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

	@Override
	public String[] getAdditionalHeader() {
		return new String[]{"quarter", "date_range"};
	}

	@Override
	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		return Collections.singletonList(
			new AbsExportGenerator(dataset, user, namespaces)
				.executeQuery(this, DateContextMode.QUARTER_WISE)
		);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		features.forEach(e -> visitor.accept(e));
	}
}
