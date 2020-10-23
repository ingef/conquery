//package com.bakdata.conquery.models.types;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
//import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
//import com.bakdata.conquery.models.common.Range.IntegerRange;
//import com.bakdata.conquery.models.dictionary.Dictionary;
//import com.bakdata.conquery.models.exceptions.JSONException;
//import com.bakdata.conquery.models.types.specific.BooleanTypeBoolean;
//import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypeDateRange;
//import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypePacked;
//import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypeQuarter;
//import com.bakdata.conquery.models.types.specific.date.DateTypeVarInt;
//import com.bakdata.conquery.models.types.specific.DecimalTypeBigDecimal;
//import com.bakdata.conquery.models.types.specific.DecimalTypeScaled;
//import com.bakdata.conquery.models.types.specific.integer.IntegerTypeLong;
//import com.bakdata.conquery.models.types.specific.integer.IntegerTypeVarInt;
//import com.bakdata.conquery.models.types.specific.MoneyTypeLong;
//import com.bakdata.conquery.models.types.specific.MoneyTypeVarInt;
//import com.bakdata.conquery.models.types.specific.RealTypeDouble;
//import com.bakdata.conquery.models.types.specific.RealTypeFloat;
//import com.bakdata.conquery.models.types.specific.string.StringTypeDictionary;
//import com.bakdata.conquery.models.types.specific.string.StringTypeEncoded;
//import com.bakdata.conquery.models.types.specific.string.StringTypeEncoded.Encoding;
//import com.bakdata.conquery.models.types.specific.string.StringTypeNumber;
//import com.bakdata.conquery.models.types.specific.string.StringTypePrefix;
//import com.bakdata.conquery.models.types.specific.string.StringTypeSingleton;
//import com.bakdata.conquery.models.types.specific.string.StringTypeSuffix;
//import com.bakdata.conquery.models.types.specific.VarIntTypeByte;
//import com.bakdata.conquery.models.types.specific.VarIntTypeInt;
//import com.bakdata.conquery.models.types.specific.VarIntTypeShort;
//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//
//public class SerializationTest {
//
//	public static List<CType<?,?>> createCTypes() {
//		return Arrays.asList(
//			new DecimalTypeScaled(13, new IntegerTypeLong(-1,1)),
//			new IntegerTypeVarInt(new VarIntTypeInt(-1, +1)),
//			new MoneyTypeLong(),
//			new DecimalTypeBigDecimal(),
//			new BooleanTypeBoolean(),
//			new MoneyTypeVarInt(new VarIntTypeInt(-1, +1)),
//			new RealTypeDouble(),
//			new DateTypeVarInt(new VarIntTypeInt(-1, +1)),
//			new StringTypeDictionary(new VarIntTypeInt(-1, +1)),
//			new StringTypeEncoded(new StringTypeDictionary(new VarIntTypeInt(-1, +1)),Encoding.Base16LowerCase),
//			new StringTypePrefix(new StringTypeEncoded(new StringTypeDictionary(new VarIntTypeInt(-1, +1)),Encoding.Base16LowerCase), "a"),
//			new StringTypeSuffix(new StringTypeEncoded(new StringTypeDictionary(new VarIntTypeInt(-1, +1)),Encoding.Base16LowerCase), "a"),
//			new StringTypeNumber(new IntegerRange(0,7), new VarIntTypeInt(0, 7)),
//			new StringTypeSingleton("a", Boolean.),
//			new IntegerTypeLong(-1,+1),
//			new DateRangeTypeDateRange(),
//			new DateRangeTypeQuarter(),
//			new DateRangeTypePacked(),
//			new DateTypeVarInt(new VarIntTypeInt(-1, +1)),
//			new VarIntTypeInt(-1, +1),
//			new VarIntTypeByte((byte)-1, (byte)+1),
//			new VarIntTypeShort((short)-1, (short)+1),
//			new RealTypeFloat(delegate)
//		);
//	}
//
//	@Test @SuppressWarnings({ "unchecked", "rawtypes" })
//	public void testAllTypesCovered() {
//		assertThat(
//			createCTypes()
//				.stream()
//				.map(Object::getClass)
//				.collect(Collectors.toSet())
//		)
//		.containsAll(
//			(Set)CPSTypeIdResolver.listImplementations(CType.class)
//		);
//	}
//
//	@ParameterizedTest @MethodSource("createCTypes")
//	public void testSerialization(CType<?,?> type) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
//		SerializationTestUtil
//			.forType(CType.class)
//			.ignoreClasses(Arrays.asList(Dictionary.class))
//			.test(type);
//	}
//}
