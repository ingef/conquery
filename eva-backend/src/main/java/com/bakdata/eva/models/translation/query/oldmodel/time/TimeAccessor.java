package com.bakdata.eva.models.translation.query.oldmodel.time;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;

public enum TimeAccessor {
	FIRST {
		@Override
		public TemporalSampler translate(DatasetId datasetId) {
			return TemporalSampler.EARLIEST;
		}
	},
	LAST {
		@Override
		public TemporalSampler translate(DatasetId datasetId) {
			return TemporalSampler.LATEST;
		}
	},
	RANDOM {
		@Override
		public TemporalSampler translate(DatasetId datasetId) {
			return TemporalSampler.RANDOM;
		}
	};

	public abstract TemporalSampler translate(DatasetId datasetId);
}
