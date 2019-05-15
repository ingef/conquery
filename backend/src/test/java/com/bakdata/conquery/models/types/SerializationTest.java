package com.bakdata.conquery.models.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.types.specific.BooleanType;
import com.bakdata.conquery.models.types.specific.DateRangeType;
import com.bakdata.conquery.models.types.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.types.specific.DateType;
import com.bakdata.conquery.models.types.specific.DecimalType;
import com.bakdata.conquery.models.types.specific.DecimalTypeScaled;
import com.bakdata.conquery.models.types.specific.IntegerType;
import com.bakdata.conquery.models.types.specific.IntegerTypeByte;
import com.bakdata.conquery.models.types.specific.IntegerTypeInteger;
import com.bakdata.conquery.models.types.specific.IntegerTypeShort;
import com.bakdata.conquery.models.types.specific.MoneyType;
import com.bakdata.conquery.models.types.specific.MoneyTypeByte;
import com.bakdata.conquery.models.types.specific.MoneyTypeInteger;
import com.bakdata.conquery.models.types.specific.MoneyTypeShort;
import com.bakdata.conquery.models.types.specific.RealType;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded.Encoding;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class SerializationTest {

	@SuppressWarnings("rawtypes")
	public static List<CType<?,?>> createCTypes() {
		return Arrays.asList(
			new DecimalTypeScaled(13, 2, 2, new IntegerTypeInteger()),
			new MoneyTypeShort(),
			new IntegerTypeShort(),
			new IntegerTypeByte(),
			new MoneyTypeInteger(),
			new DecimalType(),
			new StringTypeEncoded(Encoding.Base16LowerCase, 13,2),
			new BooleanType(),
			new MoneyTypeByte(),
			new RealType(),
			new DateType(),
			new StringType(),
			new IntegerType(),
			new MoneyType(),
			new IntegerTypeInteger(),
			new DateRangeType(),
			new DateRangeTypeQuarter()
		);
	}
	
	@Test @SuppressWarnings({ "unchecked", "rawtypes" })
	public void testAllTypesCovered() {
		assertThat(
			createCTypes()
				.stream()
				.map(Object::getClass)
				.collect(Collectors.toSet())
		)
		.containsAll(
			(Set)CPSTypeIdResolver.listImplementations(CType.class)
		);
	}

	@ParameterizedTest @MethodSource("createCTypes")
	public void testSerialization(CType<?,?> type) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		SerializationTestUtil.testSerialization(type, CType.class, Dictionary.class);
	}
}
