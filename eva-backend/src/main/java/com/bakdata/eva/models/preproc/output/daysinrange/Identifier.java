package com.bakdata.eva.models.preproc.output.daysinrange;

import java.util.List;

import lombok.Data;

@Data
public class Identifier {

	private final List<Object> values;

	public Object getValue(int index) {
		return values.get(index);
	}
}
