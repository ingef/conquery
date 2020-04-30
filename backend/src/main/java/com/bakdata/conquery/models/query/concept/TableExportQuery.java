package com.bakdata.conquery.models.query.concept;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.filter.CQUnfilteredTable;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan.TableExportConnector;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * A TABLE_EXPORT creates a full export of the given tables. It ignores selects completely.
 */
@Slf4j
@Getter @Setter
@CPSType(id = "TABLE_EXPORT", base = QueryDescription.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class TableExportQuery extends IQuery {

	@Valid
	@NotNull @NonNull
	protected IQuery query;
	@NotNull
	private Range<LocalDate> dateRange = Range.all();
	@NotEmpty @Valid
	private List<CQUnfilteredTable> tables;

	@Override
	public TableExportQueryPlan createQueryPlan(QueryPlanContext context) {
		for(CQUnfilteredTable table : tables) {
			try {
				Concept<?> concept=context.getCentralRegistry().resolve(table.getId().getConcept());
				table.setResolvedConnector(concept.getConnectorByName(table.getId().getConnector()));
			}
			catch (NoSuchElementException exc){
				log.warn("Unable to resolve connector `{}` in dataset `{}`.",table.getId().getConnector(), table.getId().getDataset(), exc);
				continue;
			}
		}
		
		int totalColumns = 0;
		List<TableExportConnector> resolvedConnectors = new ArrayList<>();
		for(CQUnfilteredTable ut : tables) {
			resolvedConnectors.add(
				new TableExportConnector(
					ut.getResolvedConnector().getTable(),
					ut.getResolvedConnector().getValidityDateColumn(ut.selectedValidityDate()),
					totalColumns
				)
			);
			totalColumns+=ut.getResolvedConnector().getTable().getColumns().length;
		}
		
		return new TableExportQueryPlan(
			query.createQueryPlan(context),
			CDateRange.of(dateRange),
			resolvedConnectors,
			totalColumns
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public TableExportQuery resolve(QueryResolveContext context) {
		this.query = query.resolve(context);
		return this;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		//collector.getSettings().
		collector.add(new SimpleResultInfo("column", ResultType.CATEGORICAL));
		collector.add(new SimpleResultInfo("value", ResultType.STRING));
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}