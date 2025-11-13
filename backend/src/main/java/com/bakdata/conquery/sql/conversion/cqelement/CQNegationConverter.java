package com.bakdata.conquery.sql.conversion.cqelement;

import com.bakdata.conquery.apiv1.query.concept.specific.CQNegation;
import com.bakdata.conquery.sql.conversion.NodeConverter;

public class CQNegationConverter implements NodeConverter<CQNegation> {

    @Override
    public Class<CQNegation> getConversionClass() {
        return CQNegation.class;
    }


    @Override
    public ConversionContext convert(CQNegation negationNode, ConversionContext context) {
        return context.getNodeConversions()
                .convert(negationNode.getChild(), context.withNegation(true))
                .withNegation(false);
    }

}
