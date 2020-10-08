package com.bakdata.conquery.apiv1;


import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class IdLabel<I extends IId<?>> implements Comparable<IdLabel<I>> {
	@NotEmpty
	private final I id;
	@NotEmpty
	private final String label;
	
	@Override
	public int compareTo(IdLabel<I> o) {
		return id.toString().compareTo(o.id.toString());
	}
}
