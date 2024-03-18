package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
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

	/**
	 * Distribute the result to a sub query.
	 *
	 * @param executionManager
	 */
	@Override
	public void addResult(DistributedExecutionManager executionManager, MetaStorage storage) {
		final ManagedInternalForm<?> managedInternalForm = (ManagedInternalForm<?>) storage.getExecution(getQueryId());
		final ManagedQuery managedExecution = (ManagedQuery) storage.getExecution(getSubQueryId());

		final ManagedQuery subQuery = managedInternalForm.getSubQuery(getSubQueryId());
		//TODO probably better to just send a nested ShardResult, than do this weird distinction.

		if (getError().isPresent()) {
			managedExecution.fail(getError().get());
		}
		else {
			executionManager.handleQueryResult(this, getSubQueryId());
		}

		// Fail the whole execution if a subquery fails
		if (ExecutionState.FAILED.equals(subQuery.getState())) {
			managedExecution.fail(
					getError().orElseThrow(
							() -> new IllegalStateException(String.format("Query [%s] failed but no error was set.", managedExecution.getId()))
					)
			);
		}

		if (managedInternalForm.allSubQueriesDone()) {
			managedExecution.finish(ExecutionState.DONE);
		}

	}

}
