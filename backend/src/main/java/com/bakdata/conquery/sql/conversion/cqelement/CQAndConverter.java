package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.LogicalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.conversion.model.QueryStepComposer;

/**
 * Adapts a resolved legacy conjunction to connector-owned logical query-step compilation.
 *
 * <p>TODO Remove this adapter once backend query resolution produces connector query nodes directly.</p>
 */
public class CQAndConverter implements NodeConverter<CQAnd> {

	@Override
	public Class<CQAnd> getConversionClass() {
		return CQAnd.class;
	}

	@Override
	public ConversionContext convert(CQAnd andNode, ConversionContext context) {
		List<QueryStep> children = QueryStepComposer.convertChildren(andNode.getChildren(), context);
		QueryStep joined = LogicalQueryStepCompiler.compile(
				children,
				JoinMode.INNER,
				andNode.getDateAction(),
				andNode.getCreateExists().orElse(false),
				EntitySchemaAdapter.from(context.getIdColumns()),
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
		return context.withQueryStep(joined);
	}
}
