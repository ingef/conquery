package com.bakdata.conquery.sql.conversion.select;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Converter;
import org.jooq.Field;

/**
 * Converts a {@link com.bakdata.conquery.models.datasets.concepts.select.Select} to a field for a SQL SELECT statement.
 *
 * @param <S> The type of Select this converter is responsible for.
 */
public interface SelectConverter<S extends Select> extends Converter<S, Field<?>> {

}
