package com.bakdata.eva.forms.description.absolute;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.absexport.AbsExportForm;
import com.bakdata.eva.forms.absexport.AbsExportGenerator;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.description.DescriptionFormBase;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id="DESCRIPTION_FORM_ABSOLUTE_TIME", base=Form.class)
public class AbsDescriptionForm extends DescriptionFormBase {

	@NotNull
	@Valid
	private ManagedExecutionId queryGroup;
	@NotNull
	@Valid
	private Range<LocalDate> dateRange;
	
	@Override
	protected JsonNode toStatisticView() {
		return Jackson.MAPPER.valueToTree(AbsDescriptionFormStatisticAPI.from(this));
	}

	@Override
	public List<ManagedQuery> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		AbsExportForm form = new AbsExportForm();
		form.setColumns(new ArrayList<>(getColumns()));
		form.setDateRange(dateRange);
		form.setFixedFeatures(getFixedFeatures());
		form.setQueryGroup(getQueryGroupId());
		form.setFeatures(getFeatures());
		form.init(namespaces, formUser);
		
		return Collections.singletonList(
			new AbsExportGenerator(dataset, user, namespaces).executeQuery(form, resolution, false)
		);
	}
	
	@Override
	protected List<FeatureGroup> asGroupType() {
		return List.of(FeatureGroup.SINGLE_GROUP);
	}

	@Override
	protected ManagedExecutionId getQueryGroupId() {
		return queryGroup;
	}
	
	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays.asList(new FeatureGroupDescription.GroupFD(getFeatures(), FeatureGroup.SINGLE_GROUP, getColumnManipulator()));
	}
}
