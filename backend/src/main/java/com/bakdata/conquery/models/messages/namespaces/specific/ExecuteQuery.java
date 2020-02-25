package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Send message to worker to execute {@code query} on the workers associated entities.
 */
@CPSType(id="EXECUTE_QUERY", base=NamespacedMessage.class)
@AllArgsConstructor @NoArgsConstructor @Getter @Setter @ToString(callSuper=true)
public class ExecuteQuery extends WorkerMessage {

	private ManagedQuery query;

	@Override
	public void react(Worker context) throws Exception {
		try {
			ShardResult result = context.getQueryExecutor().execute(new QueryPlanContext(context), query);
			result.getFuture().addListener(()->result.send(context), MoreExecutors.directExecutor());
		} catch(Exception e) {
			ShardResult result = new ShardResult();
			result.setFinishTime(LocalDateTime.now());
			result.setQueryId(query.getId());
			result.setResults(Collections.singletonList(EntityResult.failed(-1, e)));
			context.send(new CollectQueryResult(result));
		}
	}
}
