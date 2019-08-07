package com.bakdata.conquery.apiv1;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class IdLabel implements Comparable<IdLabel> {
	@NotEmpty
	private String label;
	@NotEmpty
	private String id;
	
	@Override
	public int compareTo(IdLabel o) {
		return id.compareTo(o.id);
	}
}
