package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface CQElement extends Visitable {

	/**
	 * Allows a query element to initialize data structures from resources, that are only available on the {@link ManagerNode}.
	 * The contract is:
	 * 	- no data structures are allowed to be altered, that were deserialized from a request and are serialized into a permanent storage
	 *  - all initialized data structures must be annotated with {@link InternalOnly} so they only exist at runtime between and in the communication between {@link ManagerNode} and {@link ShardNode}s
	 * @param context
	 * @return
	 */
	void resolve(QueryResolveContext context);

	QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan);

	default void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {}
	
	
	default Set<ManagedExecutionId> collectRequiredQueries() {
		HashSet<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	default ResultInfoCollector collectResultInfos() {
		ResultInfoCollector collector = new ResultInfoCollector();
		collectResultInfos(collector);
		return collector;
	}
	
	void collectResultInfos(ResultInfoCollector collector);

	default void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
