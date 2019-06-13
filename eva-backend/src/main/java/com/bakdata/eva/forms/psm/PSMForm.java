package com.bakdata.eva.forms.psm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.ColumnDescriptor.MatchingType;
import com.bakdata.eva.forms.common.FeatureGroupDescription;
import com.bakdata.eva.forms.common.FixedColumn;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.Matched;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.models.forms.EventIndex;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.bakdata.eva.models.forms.Resolution;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicates;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "PSM_FORM", base = Form.class)
public class PSMForm extends StatisticForm {

	private boolean automaticVariableSelection;
	@NotNull
	private Resolution timeUnit;
	@Min(0)
	private int timeCountBefore;
	@Min(0)
	private int timeCountAfter;
	@NotNull
	private EventIndex indexDate;
	private float caliper;
	private int matchingPartners;
	private boolean excludeOutliersDead;
	private Integer excludeOutliersMaxMoney;
	@NotNull
	@Valid
	private TimeAccessedResult controlGroup;
	@NotNull
	@Valid
	private TimeAccessedResult featureGroup;
	@NotEmpty
	private List<Matched> features;
	@NotEmpty
	private List<Matched> outcomes;

	@JsonIgnore
	private final BiConsumer<ColumnDescriptor, CQOr> featureColumnManipulator = (column, feature) -> {
		Matched f = (Matched) feature;
		column.setMatchingType(f.getMatchingType());
	};

	@JsonIgnore
	private final BiConsumer<ColumnDescriptor, CQOr> outcomeColumnManipulator = (column, feature) -> {
		Matched f = (Matched) feature;
		column.setMatchingType(Optional.of(f.getMatchingType()).filter(Predicates.equalTo(MatchingType.FIXED)).orElse(null));
	};

	public PSMForm() {
		super();
	}

	@Override
	public void init(Namespaces namespaces, User user) {
		// Use the dataset of the feature group for now, as the dataset of the other
		// group is injected later in the PSMGenerator
		String datasetIdControl = controlGroup.getId().getDataset().getName();

		this
			.setFixedFeatures(
				new FixedColumn[] {
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "alter"),
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "geschlecht"),
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "bundesland"),
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "verstorbene_ct"),
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "leistungskosten_2"),
					FixedColumn.of(FeatureGroup.FEATURE, datasetIdControl, "hash"),
					FixedColumn.of(FeatureGroup.OUTCOME, datasetIdControl, "leistungskosten_2") });

		// Set labels of groups, if not done already
		checkAndSetGroupLabel(namespaces, featureGroup);
		checkAndSetGroupLabel(namespaces, controlGroup);
		super.init(namespaces, user);
	}

	@Override
	public Collection<ManagedExecutionId> getUsedQueries() {
		return Arrays.asList(controlGroup.getId(), featureGroup.getId());
	}

	@Override
	public List<FeatureGroupDescription<?>> getFeatureGroupDescriptions() {
		return Arrays
			.asList(
				new FeatureGroupDescription.MatchedFD(features, FeatureGroup.FEATURE, featureColumnManipulator),
				new FeatureGroupDescription.MatchedFD(outcomes, FeatureGroup.OUTCOME, outcomeColumnManipulator));
	}

	@Override
	public ManagedQuery executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException, IOException {
		return new PSMGenerator(dataset, user, namespaces).execute(this);
	}

	@Override
	protected JsonNode toStatisticView() {
		return Jackson.MAPPER.valueToTree(PSMStatisticAPI.from(this));
	}

	/**
	 * Sets the group label if it was not provided by the resource to the label of
	 * the corresponding query.
	 * 
	 * @param namespaces
	 * @param group
	 */
	private static void checkAndSetGroupLabel(Namespaces namespaces, TimeAccessedResult group) {
		if (group.getLabel() == null) {
			group.setLabel(namespaces.getMetaStorage().getExecution(group.getId()).getLabel());
		}
	}
}
