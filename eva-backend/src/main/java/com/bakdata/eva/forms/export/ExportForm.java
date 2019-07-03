package com.bakdata.eva.forms.export;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ColumnNamer;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.models.forms.EventIndex;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.bakdata.eva.models.forms.Resolution;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="EXPORT_FORM", base=Form.class)
public class ExportForm extends Form {
	@NotNull
	private Resolution timeUnit;
	@Min(0)
	private int timeCountBefore;
	@Min(0)
	private int timeCountAfter;
	@NotNull
	private EventIndex indexDate;
	@NotNull @Valid
	private TimeAccessedResult queryGroup;
	@NotEmpty
	private List<CQOr> features;
	@NotEmpty
	private List<CQOr> outcomes;
	
	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays.asList(
			new FeatureGroupDescription.GroupFD(features, FeatureGroup.FEATURE),
			new FeatureGroupDescription.GroupFD(outcomes, FeatureGroup.OUTCOME)
		);
	}
	
	@Override
	public Collection<ManagedExecutionId> getUsedQueries() {
		return Arrays.asList(queryGroup.getId());
	}

	@Override
	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		return Collections.singletonList(
			new ExportGenerator(dataset, user, namespaces).execute(this, true)
		);
	}

	@Override
	public ColumnNamer getColumnNamer() {
		// May not be used, because there is no Json generated for the REnd and the column names are made from the ResultInfo of the generated query
		return new ColumnNamer();
	}
}
