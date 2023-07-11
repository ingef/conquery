package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;

public class CQNegationConverter implements NodeConverter<CQNegation> {

    @Override
    public Class<CQNegation> getConversionClass() {
        return CQNegation.class;
    }

    @Override
    public ConversionContext convert(CQNegation negationNode, ConversionContext context) {
        return this.convertChildWithNegationActive(negationNode.getChild(), context);
    }

    private ConversionContext convertChildWithNegationActive(CQElement child, ConversionContext context) {
        return context.getNodeConverterService()
                .convert(child, context.withNegation(true))
                .withNegation(false);
    }

}
