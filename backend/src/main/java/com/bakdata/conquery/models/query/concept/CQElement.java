package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class CQElement implements Visitable {

	/**
	 * Allows the user to define labels.
	 */
	@Setter
	private String label = null;

	public String getLabel(PrintSettings settings){
		return label;
	}

	/**
	 * Allows a query element to initialize data structures from resources, that are only available on the {@link ManagerNode}.
	 * The contract is:
	 * 	- no data structures are allowed to be altered, that were deserialized from a request and are serialized into a permanent storage
	 *  - all initialized data structures must be annotated with {@link InternalOnly} so they only exist at runtime between and in the communication between {@link ManagerNode} and {@link ShardNode}s
	 * @param context
	 * @return
	 */
	public abstract void resolve(QueryResolveContext context);

	public abstract QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan);

	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {}
	
	
	public Set<ManagedExecutionId> collectRequiredQueries() {
		HashSet<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	public ResultInfoCollector collectResultInfos(PrintSettings cfg) {
		ResultInfoCollector collector = new ResultInfoCollector();
		collectResultInfos(collector, cfg);
		return collector;
	}
	
	public abstract void collectResultInfos(ResultInfoCollector collector, PrintSettings cfg);

	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
