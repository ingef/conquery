package com.bakdata.eva.forms.common;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TimeAccessedResult {
	@NotNull
	private ManagedExecutionId id;
	@NotNull
	private TimeSelector timestamp;
	private Optional<DatasetId> datasetId;
	private String label;
	
	public DatasetId resolveDatasetId() {
		return datasetId.orElse(id.getDataset());
	}
}