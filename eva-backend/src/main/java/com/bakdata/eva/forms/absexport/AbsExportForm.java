package com.bakdata.eva.forms.absexport;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ColumnNamer;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.models.forms.DateContextMode;
import com.bakdata.eva.models.forms.FeatureGroup;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM_ABSOLUTE_TIME", base=Form.class)
public class AbsExportForm extends Form {
	@NotNull @Valid
	private Range<LocalDate> dateRange;
	@NotNull @Valid
	private ManagedExecutionId queryGroup;
	@NotEmpty
	private List<CQOr> features;
	
	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays.asList(
			new FeatureGroupDescription.GroupFD(features, FeatureGroup.OUTCOME)
		);
	}
	
	@Override
	protected String[] getAdditionalHeader() {
		return new String[]{"quarter", "date_range"};
	}
	

	@Override
	public Collection<ManagedExecutionId> getUsedQueries() {
		return Arrays.asList(queryGroup);
	}

	@Override
	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		return Collections.singletonList(
			new AbsExportGenerator(dataset, user, namespaces)
				.executeQuery(this, DateContextMode.QUARTER_WISE, true)
		);
	}

	@Override
	public ColumnNamer getColumnNamer() {
		// May not be used for the same reason as in ExportForm
		return new ColumnNamer();
	}
}
