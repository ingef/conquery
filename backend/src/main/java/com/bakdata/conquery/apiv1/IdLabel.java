package com.bakdata.conquery.apiv1;


import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.AId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Container class for the frontend to provide a tuple of id and a corresponding label.
 *
 * @param <I> The id type to use
 */
@Getter
@RequiredArgsConstructor
public class IdLabel<I extends AId<?>> implements Comparable<IdLabel<I>> {
	@NotEmpty
	private final I id;
	@NotEmpty
	private final String label;

	@Override
	public int compareTo(IdLabel<I> o) {
		return id.toString().compareTo(o.id.toString());
	}
}
