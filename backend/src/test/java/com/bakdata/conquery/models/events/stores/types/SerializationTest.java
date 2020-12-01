package com.bakdata.conquery.models.events.stores.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.Range.IntegerRange;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.DateStore;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.events.stores.base.DoubleStore;
import com.bakdata.conquery.models.events.stores.base.FloatStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.events.stores.specific.BooleanTypeBoolean;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.events.stores.specific.DecimalTypeBigDecimal;
import com.bakdata.conquery.models.events.stores.specific.DecimalTypeScaled;
import com.bakdata.conquery.models.events.stores.specific.MoneyType;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeNumber;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypePrefixSuffix;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeSingleton;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SerializationTest {

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testAllTypesCovered() {
		assertThat(
				createCTypes()
						.stream()
						.map(Object::getClass)
						.collect(Collectors.toSet())
		)
				.containsAll(
						(Set) CPSTypeIdResolver.listImplementations(ColumnStore.class)
				);
	}

	public static List<ColumnStore<?>> createCTypes() {
		final MapDictionary dictionary = new MapDictionary(new DatasetId("dataset"), "hi");
		return Arrays.asList(
				new DecimalTypeScaled(13, IntegerStore.create(10)),
				new MoneyType(IntegerStore.create(10)),
				new DecimalTypeBigDecimal(DecimalStore.create(10)),
				new BooleanTypeBoolean(BooleanStore.create(10)),
				new StringTypeDictionary(IntegerStore.create(10), dictionary, "hi"),
				new StringTypeEncoded(new StringTypeDictionary(IntegerStore.create(10), dictionary, "hi"), Encoding.Base16LowerCase),
				new StringTypePrefixSuffix(new StringTypeEncoded(new StringTypeDictionary(IntegerStore.create(10), dictionary, "hi"), Encoding.Base16LowerCase), "a", "b"),

				new StringTypeNumber(new IntegerRange(0, 7), ByteStore.create(10)),
				new StringTypeSingleton("a", BooleanStore.create(10)),
				new DateRangeTypeDateRange(LongStore.create(10), LongStore.create(10)),
				new DateRangeTypeQuarter(LongStore.create(10)),
				new DateStore(LongStore.create(10)),

				DecimalStore.create(10),
				LongStore.create(10),
				IntegerStore.create(10),
				ByteStore.create(10),
				ShortStore.create(10),
				FloatStore.create(10),
				DoubleStore.create(10),
				BooleanStore.create(10),
				new EmptyStore<>(MajorTypeId.DECIMAL),
				new RebasingStore(10,10,IntegerStore.create(10))
		);
	}

	@ParameterizedTest
	@MethodSource("createCTypes")
	public void testSerialization(ColumnStore type) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {
		SerializationTestUtil
				.forType(ColumnStore.class)
				.ignoreClasses(List.of(Dictionary.class))
				.test(type);
	}
}
