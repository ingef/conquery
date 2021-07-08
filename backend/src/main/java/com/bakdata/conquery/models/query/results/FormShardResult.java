package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@CPSType(id = "FORM_SHARD_RESULT", base = ShardResult.class)
@EqualsAndHashCode(callSuper = true)
@Getter
public class FormShardResult extends ShardResult {
	private final ManagedExecutionId subQueryId;

	public FormShardResult(ManagedExecutionId queryId, ManagedExecutionId subQueryId, WorkerId workerId) {
		super(queryId, workerId);
		this.subQueryId = subQueryId;
	}
}
