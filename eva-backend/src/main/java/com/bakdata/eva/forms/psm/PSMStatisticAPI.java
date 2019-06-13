package com.bakdata.eva.forms.psm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.eva.EvaConstants;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.EventPeriods;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PSMStatisticAPI {

	private String type = "PSM";
	private boolean automaticFeatureSelection;
	@NotNull
	@Valid
	private EventPeriods time;
	private float caliper;
	private int matchingPartners;
	@NotNull
	private List<Map<String, Object>> outlier = new ArrayList<>();
	@NotNull
	private TimeAccessedResult controlGroup;
	@NotNull
	private TimeAccessedResult featureGroup;
	@NotEmpty
	private List<ColumnDescriptor> features = new ArrayList<>();
	@NotEmpty
	private List<ColumnDescriptor> outcomes = new ArrayList<>();

	public static PSMStatisticAPI from(PSMForm o) {
		PSMStatisticAPI form = new PSMStatisticAPI();
		form.automaticFeatureSelection = o.isAutomaticVariableSelection();
		form.time = new EventPeriods();
		{
			form.time.setCountAfter(o.getTimeCountAfter());
			form.time.setCountBefore(o.getTimeCountBefore());
			form.time.setIndexDate(o.getIndexDate());
			form.time.setUnit(o.getTimeUnit());
		}
		form.caliper = o.getCaliper();
		form.matchingPartners = o.getMatchingPartners();
		if (o.getExcludeOutliersMaxMoney() != null) {
			form.outlier.add(ImmutableMap.of("mode", "PRICE", "threshold", o.getExcludeOutliersMaxMoney()));
		}
		if (o.isExcludeOutliersDead())
			form.outlier.add(Collections.singletonMap("mode", "ALIVE"));
		if (form.outlier.isEmpty())
			form.outlier.add(Collections.singletonMap("mode", "NO"));
		form.controlGroup = o.getControlGroup();
		form.featureGroup = o.getFeatureGroup();
		 List<ColumnDescriptor> descriptors = new ArrayList<>(o.getFeatures().size());
		 descriptors.addAll(o.getColumns());

		/*
		 * Here we split the columns into features and outcomes for the JSON.
		 */
		Pattern featureP = Pattern.compile("^" + EvaConstants.FEATURE_PREFIX);
		Pattern outcomeP = Pattern.compile("^" + EvaConstants.OUTCOME_PREFIX);
		form.features
			.addAll(
				descriptors
					.stream()
					.filter(d -> featureP.matcher(d.getColumn()).find())
					.collect(Collectors.toList()));
		form.outcomes
			.addAll(
				descriptors
				.stream()
				.filter(d -> outcomeP.matcher(d.getColumn()).find())
				.collect(Collectors.toList()));

		return form;
	}
}