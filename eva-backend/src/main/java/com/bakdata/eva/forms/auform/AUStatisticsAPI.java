package com.bakdata.eva.forms.auform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.eva.forms.common.ColumnDescriptor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AUStatisticsAPI {

	public static final String BASECONDITION_LABEL = "Grundbedingung";
	private String type = "AU";
	@NotNull
	private ManagedExecutionId id;
	@NotNull
	private Range<LocalDate> dateRange;
	@NotNull
	@NotEmpty
	private List<ColumnDescriptor> features;

	
	public static AUStatisticsAPI from(AUForm auForm) {
		AUStatisticsAPI form = new AUStatisticsAPI();
		form.dateRange = auForm.getDateRange();
		form.id = auForm.getQueryGroup();
		form.features = createFeatureDescriptors(auForm);

		return form;
	}

	private static List<ColumnDescriptor> createFeatureDescriptors(AUForm o) {
		List<ColumnDescriptor> descriptors = new ArrayList<>(o.getFeatures().size());
		descriptors.addAll(o.getColumns());
		//baseCondition was empty so insert an indicator
		if(o.getBaseCondition().isEmpty()) {
			descriptors.add(
				ColumnDescriptor
				.builder()
				.label("NONE")
				.description("NONE")
				.column("NONE")
				.type(ResultType.STRING)
				.baseCondition(true)
				.build()
			);
		}
		return descriptors;
	}
}
