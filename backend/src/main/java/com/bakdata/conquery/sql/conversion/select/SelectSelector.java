package com.bakdata.conquery.sql.conversion.select;

import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import lombok.Getter;

/**
 * Determines if a specific {@link SelectConverter SelectConverter} is responsible
 * for a specific type of {@link com.bakdata.conquery.models.datasets.concepts.select.Select Select}.
 */
public class SelectSelector<S extends Select> {

	@Getter
	private final Class<S> selectClass;

	public SelectSelector(Class<S> selectClass) {
		this.selectClass = selectClass;
	}

	public Optional<S> select(Select select) {
		return selectClass.isInstance(select)
			   ? Optional.of(selectClass.cast(select))
			   : Optional.empty();
	}

}
