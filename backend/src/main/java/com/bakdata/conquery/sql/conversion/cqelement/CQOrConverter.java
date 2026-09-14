package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.LogicalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.conversion.model.QueryStepComposer;

/**
 * Adapts a resolved legacy disjunction to connector-owned logical query-step compilation.
 *
 * <p>TODO Remove this adapter once backend query resolution produces connector query nodes directly.</p>
 */
public class CQOrConverter implements NodeConverter<CQOr> {

	@Override
	public Class<CQOr> getConversionClass() {
		return CQOr.class;
	}

	@Override
	public ConversionContext convert(CQOr orNode, ConversionContext context) {
		List<QueryStep> children = QueryStepComposer.convertChildren(orNode.getChildren(), context);
		QueryStep joined = LogicalQueryStepCompiler.compile(
				children,
				JoinMode.FULL_OUTER,
				orNode.getDateAction(),
				orNode.getCreateExists().orElse(false),
				EntitySchemaAdapter.from(context.getIdColumns()),
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
		return context.withQueryStep(joined);
	}
}
