package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;

/**
 * Interface for converters that implement the translation of a ConQuery query to an SQL query.
 *
 * <p>
 * A ConQuery is a graph that has a {@link com.bakdata.conquery.apiv1.query.QueryDescription} as its root.
 * The children of the root are of type {@link com.bakdata.conquery.apiv1.query.CQElement}.
 *
 * @param <V> type of the node to convert
 */
public interface NodeConverter<V extends Visitable> extends Converter<V, ConversionContext, ConversionContext> {

}
