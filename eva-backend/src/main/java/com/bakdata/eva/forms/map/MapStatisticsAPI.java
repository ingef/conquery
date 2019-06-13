package com.bakdata.eva.forms.map;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.models.forms.DateContextMode;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MapStatisticsAPI {
		
	private String type = "MAP";
	@NotNull
	private Range<LocalDate> dateRange;
	@NotNull
	private MapForm.Granularity granularity;
	@NotNull
	private String outcomeColumnName;
	@NotNull
	private String region;
	@NotNull
	private DateContextMode resolution;
	@NotEmpty
	private List<ColumnDescriptor> features = new ArrayList<>();
	private String granularityColumnName;
	private boolean relative;
	@NotNull
	private TimeAccessedResult featureGroup;
	
	public static MapStatisticsAPI from(MapForm o) {
		MapStatisticsAPI form = new MapStatisticsAPI();
		form.relative = o.isRelative();
		form.dateRange = o.getDateRange();
		form.granularity = o.getGranularity();
		form.region = o.getRegion().getRegionCode();
		form.resolution = o.getResolution();
		form.granularityColumnName = o.getGranularityColumnName();
		form.outcomeColumnName = o.getOutcomeColumnName();
		form.features = createFeatureDescriptors(o);
		form.featureGroup = createQueryGroupInfo(o.getQueryGroup());
		
		return form;
	}

	private static TimeAccessedResult createQueryGroupInfo(ManagedExecutionId query) {
		TimeAccessedResult info = new TimeAccessedResult();
		info.setDatasetId(Optional.of(query.getDataset()));
		info.setId(query);
		return info;
	}

	private static List<ColumnDescriptor> createFeatureDescriptors(MapForm o) {
		List<ColumnDescriptor> descriptors = new ArrayList<>(o.getFeatures().size());
		descriptors.addAll(o.getColumns());

		return descriptors;
	}
}
