package com.bakdata.conquery.apiv1.query;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@EqualsAndHashCode
public abstract class CQElement implements Visitable {

	/**
	 * Allows the user to define labels.
	 */
	@Setter
	@Getter
	private String label = null;

	public String getUserOrDefaultLabel(Locale locale) {
		// Prefer the user label
		if (label != null) {
			return label;
		}
		return defaultLabel(locale);
	}

	@NotNull
	public String defaultLabel(Locale locale) {
		// Fallback to CPSType#id() implementation is provided or class name
		CPSType type = this.getClass().getAnnotation(CPSType.class);
		if (type != null) {
			return type.id();
		}
		return this.getClass().getSimpleName();
	}

	/**
	 * Allows a query element to initialize data structures from resources, that are only available on the {@link ManagerNode}.
	 * The contract is:
	 * - no data structures are allowed to be altered, that were deserialized from a request and are serialized into a permanent storage
	 * - all initialized data structures must be annotated with {@link InternalOnly} so they only exist at runtime between and in the communication between {@link ManagerNode} and {@link ShardNode}s
	 *
	 * @param context
	 * @return
	 */
	public abstract void resolve(QueryResolveContext context);

	public abstract QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan);

	public Set<ManagedExecutionId> collectRequiredQueries() {
		Set<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
	}

	@JsonIgnore
	public abstract List<ResultInfo> getResultInfos();

	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}

	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return new RequiredEntities(context.getBucketManager().getEntities().keySet());
	}
}
