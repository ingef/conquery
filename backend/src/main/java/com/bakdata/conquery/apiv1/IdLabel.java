package com.bakdata.conquery.apiv1;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class IdLabel {
	@NotEmpty
	private String label;
	@NotEmpty
	private String id;
}
