package com.bakdata.eva.forms.description.relative;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.eva.EvaConstants;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.EventPeriods;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.forms.description.DescriptionFormBaseStatisticAPI;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelativeDescriptionFormStatisticAPI extends DescriptionFormBaseStatisticAPI {

	private String type = "DESCRIPTION_FORM_RELATIVE_TIME";
	@NotNull
	@Valid
	private TimeAccessedResult queryGroup;
	@NotNull
	@NotEmpty
	private List<ColumnDescriptor> features = new ArrayList<>();
	@NotNull
	@NotEmpty
	private List<ColumnDescriptor> outcomes = new ArrayList<>();
	@NotNull
	@Valid
	private EventPeriods time;

	public static RelativeDescriptionFormStatisticAPI from(RelativeDescriptionForm in) {
		RelativeDescriptionFormStatisticAPI form = new RelativeDescriptionFormStatisticAPI();
		form.queryGroup = in.getQueryGroup();
		form.resolution = in.getResolution();

		List<ColumnDescriptor> descriptors = new ArrayList<>(in.getFeatures().size());
		descriptors.addAll(in.getColumns());
		/*
		 * Here we split the columns into features and outcomes for the JSON.
		 */
		Pattern featureP = Pattern.compile("^" + EvaConstants.FEATURE_PREFIX);
		Pattern outcomeP = Pattern.compile("^" + EvaConstants.OUTCOME_PREFIX);
		form.features.addAll(descriptors.stream().filter(d -> featureP.matcher(d.getColumn()).find()).collect(Collectors.toList()));
		form.outcomes.addAll(descriptors.stream().filter(d -> outcomeP.matcher(d.getColumn()).find()).collect(Collectors.toList()));

		form.time = createTimeData(in);

		return form;
	}

	private static EventPeriods createTimeData(RelativeDescriptionForm in) {
		EventPeriods p = new EventPeriods();
		p.setCountAfter(in.getTimeCountAfter());
		p.setCountBefore(in.getTimeCountBefore());
		p.setIndexDate(in.getIndexDate());
		p.setUnit(in.getTimeUnit());
		return p;
	}
}
