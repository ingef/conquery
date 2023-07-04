package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.MergedSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public ConversionContext convert(CQAnd node, ConversionContext context) {
		// if the AND node has a single child, the AND node is a noop
		// otherwise, the converted children need to be logically combined before we obtain the final query
		if (node.getChildren().size() == 1) {
			return context.getNodeConverterService().convert(node.getChildren().get(0), context);
		}

		ConversionContext childrenContext = context;
		for (CQElement child : node.getChildren()) {
			childrenContext = context.getNodeConverterService().convert(child, childrenContext);
		}

		List<QueryStep> queriesToJoin = childrenContext.getQuerySteps();
		QueryStep andQueryStep = QueryStep.builder()
										  .cteName(this.constructAndQueryStepLabel(queriesToJoin))
										  .selects(new MergedSelects(queriesToJoin))
										  .fromTable(this.constructJoinedTable(queriesToJoin))
										  .conditions(Collections.emptyList())
										  .predecessors(queriesToJoin)
										  .build();

		return context.withQueryStep(andQueryStep);
	}

	private String constructAndQueryStepLabel(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
							.map(QueryStep::getCteName)
							.collect(Collectors.joining("_AND_"));
	}

	private TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin) {

		Table<Record> joinedQuery = this.getIntitialJoinTable(queriesToJoin);

		for (int i = 0; i < queriesToJoin.size() - 1; i++) {

			QueryStep leftPartQS = queriesToJoin.get(i);
			QueryStep rightPartQS = queriesToJoin.get(i + 1);

			Field<Object> leftPartPrimaryColumn = leftPartQS.getQualifiedSelects().getPrimaryColumn();
			Field<Object> rightPartPrimaryColumn = rightPartQS.getQualifiedSelects().getPrimaryColumn();

			joinedQuery = joinedQuery
					.innerJoin(rightPartQS.getCteName())
					.on(leftPartPrimaryColumn.eq(rightPartPrimaryColumn));
		}

		return joinedQuery;
	}

	private Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return DSL.table(DSL.name(queriesToJoin.get(0).getCteName()));
	}


	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}
}
