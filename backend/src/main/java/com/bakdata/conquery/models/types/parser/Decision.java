package com.bakdata.conquery.models.types.parser;

import com.bakdata.conquery.models.types.CType;
import lombok.Data;

@Data
public class Decision<CTYPE extends CType<?, ?>> {
	private final CTYPE type;

	public Decision(CTYPE type) {
		this.type = type;
	}
}
