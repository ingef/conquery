package com.bakdata.eva.models.translation.query.oldmodel.time;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.query.concept.specific.temporal.CQSampled;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TimeAccessedResult {
	@NotNull
	private UUID id;
	@NotNull
	private TimeAccessor timestamp;
	private String datasetId;

	public CQSampled translate(DatasetId dataset) {
		return new CQSampled(new CQReusedQuery(new ManagedExecutionId(dataset, id)), timestamp.translate(dataset));
	}
}
