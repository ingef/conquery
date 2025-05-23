package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.OrNode;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@CPSType(id = "OR", base = CQElement.class)
@ToString
public class CQOr extends CQElement implements ExportForm.DefaultSelectSettable {
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
	public void setDefaultSelects() {
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

		final QPNode or = OrNode.of(Arrays.asList(nodes), dateAction);

		if (createExists()) {
			final ExistsAggregator existsAggregator = new ExistsAggregator(or.collectRequiredTables());
			existsAggregator.setReference(or);
			plan.registerAggregator(existsAggregator);
		}

		return or;
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
			case MERGE, LOGICAL -> DateAggregationAction.MERGE;
			case INTERSECT -> DateAggregationAction.INTERSECT;
		};
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		List<ResultInfo> resultInfos = new ArrayList<>();
		for (CQElement c : children) {
			resultInfos.addAll(c.getResultInfos());
		}

		if (createExists()) {
			resultInfos.add(new FixedLabelResultInfo(ResultType.Primitive.BOOLEAN, Set.of()) {
				@Override
				public String userColumnName(PrintSettings printSettings) {
					return userLabel(printSettings.getLocale());
				}

				@Override
				public String defaultColumnName(PrintSettings printSettings) {
					return defaultLabel(printSettings.getLocale());
				}
			});
		}

		return resultInfos;
	}

	@Override
	public String userLabel(Locale locale) {
		// Prefer the user label
		if (getLabel() != null) {
			return getLabel();
		}
		CQElementC10n localized = C10nCache.getLocalized(CQElementC10n.class, locale);
		return QueryUtils.createUserMultiLabel(children, " " + localized.or() + " ", " " + localized.exists(), locale);
	}

	@Override
	public String defaultLabel(Locale locale) {
		// This forces the default label on children even if there was a user label
		CQElementC10n localized = C10nCache.getLocalized(CQElementC10n.class, locale);
		return QueryUtils.createDefaultMultiLabel(children, " " + localized.or() + " ", " " + localized.exists(), locale);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);

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
				current = current.union(next);
			}
		}

		return current;
	}
}
