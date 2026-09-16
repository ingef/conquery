package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.sql.compiler.ir.AllEntitiesQueryStepCompiler;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;

/**
 * Adapts the legacy all-entities query node to connector-owned compilation.
 *
 * <p>TODO Remove this adapter once backend query resolution produces the connector's all-entities node directly.</p>
 */
public class CQYesConverter implements NodeConverter<CQYes> {

	@Override
	public Class<? extends CQYes> getConversionClass() {
		return CQYes.class;
	}

	@Override
	public ConversionContext convert(CQYes cqYes, ConversionContext context) {
		return context.withQueryStep(AllEntitiesQueryStepCompiler.compile(
				new AllEntitiesNode(),
				EntitySchemaAdapter.from(context.getIdColumns()),
				context.getCompilerDialect()
		));
	}
}
