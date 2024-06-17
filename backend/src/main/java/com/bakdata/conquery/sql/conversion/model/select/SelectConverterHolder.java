package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import lombok.RequiredArgsConstructor;

/**
 * Glue-class to connect a concrete {@link Select} class with a {@link SelectConverter}.
 *
 * @param <S> The type of {@link Select} this holder's {@link SelectConverter} can handle.
 */
@RequiredArgsConstructor
public class SelectConverterHolder<S extends Select> {

	private final S select;
	private final SelectConverter<S> converter;

	public ConnectorSqlSelects connectorSelect(SelectContext<Connector, ConnectorSqlTables> selectContext) {
		return converter.connectorSelect(select, selectContext);
	}

	public ConceptSqlSelects conceptSelect(SelectContext<TreeConcept, ConceptSqlTables> selectContext) {
		return converter.conceptSelect(select, selectContext);
	}

}
