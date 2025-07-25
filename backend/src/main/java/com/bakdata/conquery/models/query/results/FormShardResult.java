package com.bakdata.conquery.models.query.results;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@CPSType(id = "FORM_SHARD_RESULT", base = NamespacedMessage.class)
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
public class FormShardResult extends ShardResult {

	@Getter
	@Setter
	@ToString.Include
	private ManagedExecutionId formId;

	public FormShardResult(ManagedExecutionId formId, ManagedExecutionId subQueryId, WorkerId workerId) {
		super(subQueryId, workerId);
		this.formId = formId;
	}

	/**
	 * Distribute the result to a sub query.
	 */
	@Override
	protected void addResult(DistributedExecutionManager executionManager) {
		final ManagedInternalForm<?> managedInternalForm = (ManagedInternalForm<?>) executionManager.getExecution(getFormId());
		final ManagedQuery subQuery = managedInternalForm.getSubQuery(getQueryId());

		if (subQuery == null) {
			throw new IllegalStateException("Subquery %s did not belong to form %s. Known subqueries: %s".formatted(getQueryId(),
																													formId,
																													managedInternalForm.getSubQueries()
			));
		}


		executionManager.handleQueryResult(this, subQuery);

		// Fail the whole execution if a subquery fails
		if (ExecutionState.FAILED.equals(subQuery.getState())) {
			managedInternalForm.fail(getError().orElseThrow(() -> new IllegalStateException(String.format("Query[%s] failed but no error was set.", subQuery.getId()))));
		}

		if (managedInternalForm.allSubQueriesDone()) {
			managedInternalForm.finish(ExecutionState.DONE);
		}

	}

}
