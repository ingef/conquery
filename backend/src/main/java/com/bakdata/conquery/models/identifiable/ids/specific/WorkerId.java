package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.worker.ShardId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class WorkerId extends Id<WorkerInformation> {

	private final ShardId shardId;
	private final DatasetId dataset;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
	}

	public static enum Parser implements IdUtil.Parser<WorkerId> {
		INSTANCE;

		@Override
		public WorkerId parseInternally(IdIterator parts) {
			return new WorkerId(ShardId.Parser.INSTANCE.parse(parts), DatasetId.Parser.INSTANCE.parse(parts));
		}
	}
}
