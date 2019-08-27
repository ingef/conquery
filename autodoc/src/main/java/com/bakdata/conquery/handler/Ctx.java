package com.bakdata.conquery.handler;

import io.github.classgraph.FieldInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Getter
@Wither @AllArgsConstructor @NoArgsConstructor
public class Ctx {
	private FieldInfo field;
	private boolean idOf;
	private boolean generic;
}
