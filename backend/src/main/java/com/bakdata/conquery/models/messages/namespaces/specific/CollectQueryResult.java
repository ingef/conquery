package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workers send their part of the query result to ManagerNode for assembly.
 */
@CPSType(id = "COLLECT_QUERY_RESULT", base = NamespacedMessage.class)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
//TODO flatten this into ShardResult
public class CollectQueryResult extends NamespaceMessage {

	private ShardResult result;

	@Override
	public void react(DistributedNamespace context) throws Exception {
		log.info("Received {} of size {}", result, result.getResults().size());

		result.addResult(context.getExecutionManager());
	}
}
