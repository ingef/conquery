package com.bakdata.conquery.models.types.parser;

import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.specific.DateTypeVarInt;
import com.bakdata.conquery.models.types.specific.VarIntType;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Decision<MAJOR_JAVA_TYPE, JAVA_TYPE, CTYPE extends CType<MAJOR_JAVA_TYPE, JAVA_TYPE>> {
	private final Transformer<MAJOR_JAVA_TYPE, JAVA_TYPE> transformer;
	private final CTYPE type;
}
