package com.bakdata.conquery.sql.conversion.select;

import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Field;

/**
 * Converts a {@link com.bakdata.conquery.models.datasets.concepts.select.Select} to a field for a SQL SELECT statement.
 *
 * @param <S> The type of Select this converter is responsible for.
 */
public abstract class SelectConverter<S extends Select> {

	private final SelectSelector<S> selector;

	protected SelectConverter(Class<S> selectClass) {
		this.selector = new SelectSelector<>(selectClass);
	}

	public Optional<Field<?>> convert(final Select selectNode, final ConversionContext context) {
		return this.selector.select(selectNode).map(select -> this.convertSelect(select, context));
	}

	protected abstract Field<?> convertSelect(final S select, final ConversionContext context);

}
