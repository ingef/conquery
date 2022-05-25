package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class WorkerId extends AId<WorkerInformation> {

	private final DatasetId dataset;
	private final String worker;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(worker);
	}

	public static enum Parser implements IdUtil.Parser<WorkerId> {
		INSTANCE;

		@Override
		public WorkerId parseInternally(IdIterator parts) {
			String name = parts.next();
			return new WorkerId(DatasetId.Parser.INSTANCE.parse(parts), name);
		}
	}
}
