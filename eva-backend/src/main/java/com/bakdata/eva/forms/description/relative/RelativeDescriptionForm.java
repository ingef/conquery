package com.bakdata.eva.forms.description.relative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.forms.common.ColumnDescriptor.ColumnType;
import com.bakdata.eva.forms.description.DescriptionFormBase;
import com.bakdata.eva.forms.export.ExportForm;
import com.bakdata.eva.forms.export.ExportGenerator;
import com.bakdata.eva.models.forms.EventIndex;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.bakdata.eva.models.forms.Resolution;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id="DESCRIPTION_FORM_RELATIVE_TIME", base=Form.class)
public class RelativeDescriptionForm extends DescriptionFormBase {

	@NotNull
	@Valid
	private TimeAccessedResult queryGroup;
	@NotNull
	@NotEmpty
	private List<CQOr> outcomes = new ArrayList<>();
	@Min(0)
	private int timeCountAfter;
	@Min(0)
	private int timeCountBefore;
	@NotNull
	private EventIndex indexDate;
	@NotNull
	private Resolution timeUnit = Resolution.QUARTERS;

	
	@Override
	protected JsonNode toStatisticView() {
		return Jackson.MAPPER.valueToTree(RelativeDescriptionFormStatisticAPI.from(this));
	}

	@Override
	public ManagedQuery executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException {
		ExportForm form = new ExportForm();
		form.setColumns(new ArrayList<>(getColumns()));
		form.setTimeCountAfter(timeCountAfter);
		form.setTimeCountBefore(timeCountBefore);
//		form.setFixedFeatures(getFixedFeatures());
		form.setFeatures(getFeatures());
		form.setOutcomes(outcomes);
		form.setQueryGroup(getQueryGroup());
		form.setIndexDate(indexDate);
		form.setTimeUnit(timeUnit);
		form.init(namespaces, user);
		
		return new ExportGenerator(dataset, user, namespaces).execute(form, false);
	}

	@Override
	protected List<FeatureGroup> asGroupType() {
		return List.of(
			FeatureGroup.FEATURE,
			FeatureGroup.OUTCOME); //Was Both
	}

	@Override
	protected ManagedExecutionId getQueryGroupId() {
		return queryGroup.getId();
	}
	
	
	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays.asList(
			new FeatureGroupDescription.GroupFD(getFeatures(), FeatureGroup.FEATURE, getColumnManipulator()),
			new FeatureGroupDescription.GroupFD(getOutcomes(), FeatureGroup.OUTCOME, getColumnManipulator()));
	}
}
