package com.bakdata.eva.forms.description.absolute;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.description.DescriptionFormBaseStatisticAPI;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbsDescriptionFormStatisticAPI extends DescriptionFormBaseStatisticAPI {
	
	@NotNull
	private UUID id;
	private String type = "DESCRIPTION_FORM_ABSOLUTE_TIME";
	@NotNull
	private Range<LocalDate> dateRange;
	@NotNull
	@NotEmpty
	private List<ColumnDescriptor> features = new ArrayList<>();
	
	public static AbsDescriptionFormStatisticAPI from(AbsDescriptionForm in) {
		AbsDescriptionFormStatisticAPI form = new AbsDescriptionFormStatisticAPI();
		form.id = in.getQueryGroupId().getExecution();
		form.resolution = in.getResolution();
		form.dateRange = in.getDateRange();
		form.features = createFeatureDescriptors(in);
		
		return form;
	}
	
	private static List<ColumnDescriptor> createFeatureDescriptors(AbsDescriptionForm o) {

		List<ColumnDescriptor> descriptors = new ArrayList<>(o.getFeatures().size());
		descriptors.addAll(o.getColumns());
		return descriptors;
	}
}
