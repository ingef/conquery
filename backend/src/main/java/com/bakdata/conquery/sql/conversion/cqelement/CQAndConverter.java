package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;
import java.util.stream.Collectors;

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

		List<QueryStep> queriesToJoin = node.getChildren().stream()
											.map(child -> context.getNodeConverterService().convert(child, context))
											.flatMap(resultContext -> resultContext.getQuerySteps().stream())
											.toList();

		QueryStep andQueryStep = QueryStep.builder()
										  .cteName(this.constructAndQueryStepLabel(queriesToJoin))
										  .selects(new MergedSelects(queriesToJoin))
										  .fromTable(this.constructJoinedTable(queriesToJoin))
										  .conditions(List.of())
										  .predecessors(queriesToJoin)
										  .build();

		return context.withQuerySteps(List.of(andQueryStep));
	}

	private String constructAndQueryStepLabel(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
							.map(QueryStep::getCteName)
							.collect(Collectors.joining("_")) + "_AND";
	}

	private TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin) {

		Table<Record> joinedQuery = this.getIntitialJoinTable(queriesToJoin);

		for (int i = 1; i < queriesToJoin.size(); i++) {

			QueryStep leftPartQS = queriesToJoin.get(i - 1);
			QueryStep rightPartQS = queriesToJoin.get(i);

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
