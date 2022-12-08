package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import c10n.C10N;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;
import com.bakdata.conquery.models.query.resultinfo.LocalizedDefaultResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;

@CPSType(id = "AND", base = CQElement.class)
public class CQAnd extends CQElement implements ExportForm.DefaultSelectSettable {

	@Getter
	@Setter
	@NotEmpty
	@Valid
	private List<CQElement> children;

	@Getter
	@Setter
	private Optional<Boolean> createExists = Optional.empty();

	@Getter
	@Setter
	@JsonView(View.InternalCommunication.class)
	private DateAggregationAction dateAction;

	@Override
	public void setDefaultExists() {
		if (createExists.isEmpty()) {
			createExists = Optional.of(true);
		}
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		Preconditions.checkNotNull(dateAction);

		QPNode[] nodes = new QPNode[children.size()];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = children.get(i).createQueryPlan(context, plan);
		}

		final QPNode node = AndNode.of(Arrays.asList(nodes), dateAction);

		if (createExists()) {
			final ExistsAggregator existsAggregator = new ExistsAggregator(node.collectRequiredTables());
			existsAggregator.setReference(node);
			plan.registerAggregator(existsAggregator);
		}

		return node;
	}

	private boolean createExists() {
		return createExists.orElse(false);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		for (CQElement c : children) {
			c.collectRequiredQueries(requiredQueries);
		}
	}

	@Override
	public void resolve(QueryResolveContext context) {
		Preconditions.checkNotNull(context.getDateAggregationMode());

		dateAction = determineDateAction(context);
		children.forEach(c -> c.resolve(context));
	}

	private DateAggregationAction determineDateAction(QueryResolveContext context) {
		return switch (context.getDateAggregationMode()) {
			case NONE -> DateAggregationAction.BLOCK;
			case MERGE -> DateAggregationAction.MERGE;
			case LOGICAL, INTERSECT -> DateAggregationAction.INTERSECT;
		};
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		List<ResultInfo> resultInfos = new ArrayList<>();
		for (CQElement c : children) {
			resultInfos.addAll(c.getResultInfos());
		}

		if (createExists()) {
			resultInfos.add(new LocalizedDefaultResultInfo(this::getUserOrDefaultLabel, this::defaultLabel, ResultType.BooleanT.INSTANCE, Set.of()));
		}

		return resultInfos;
	}

	@Override
	public String getUserOrDefaultLabel(Locale locale) {
		// Prefer the user label
		if (getLabel() != null) {
			return getLabel();
		}
		return QueryUtils.createDefaultMultiLabel(children, " " + C10N.get(CQElementC10n.class, locale).and() + " ", locale);
	}

	@Override
	public String defaultLabel(Locale locale) {
		// This forces the default label on children even if there was a user label
		return QueryUtils.createTotalDefaultMultiLabel(children, " " + C10N.get(CQElementC10n.class, locale).and() + " ", locale);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		for (CQElement c : children) {
			c.visit(visitor);
		}
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		RequiredEntities current = null;

		for (int index = 0; index < getChildren().size(); index++) {
			final RequiredEntities next = getChildren().get(index).collectRequiredEntities(context);

			if (current == null) {
				current = next;
			}
			else {
				current = current.intersect(next);
			}
		}

		return current;
	}
}
