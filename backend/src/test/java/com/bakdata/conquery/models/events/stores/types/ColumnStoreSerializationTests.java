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
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.primitive.ByteArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.DecimalArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.IntArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.primitive.LongArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeCompound;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.events.stores.specific.DecimalTypeScaled;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeNumber;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypePrefixSuffix;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeSingleton;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ColumnStoreSerializationTests {

	private static final Set<Class<? extends ColumnStore>> EXCLUDING = Set.of(DateRangeTypeCompound.class);

	private static final CentralRegistry CENTRAL_REGISTRY = new CentralRegistry();
	private static final Dictionary DICTIONARY = new MapDictionary(Dataset.PLACEHOLDER, "dictionary");

	@BeforeAll
	public static void setupRegistry() {
		CENTRAL_REGISTRY.register(Dataset.PLACEHOLDER);
		CENTRAL_REGISTRY.register(DICTIONARY);
	}

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
						Sets.difference(
								(Set) CPSTypeIdResolver.listImplementations(ColumnStore.class),
								EXCLUDING
						)
				)
				.doesNotContainAnyElementsOf(EXCLUDING);
	}

	public static List<ColumnStore> createCTypes() {

		return Arrays.asList(
				new DecimalTypeScaled(13, IntArrayStore.create(10)),
				new MoneyIntStore(IntArrayStore.create(10)),
				new StringTypeDictionary(IntArrayStore.create(10), DICTIONARY),
				new StringTypeEncoded(new StringTypeDictionary(IntArrayStore.create(10), DICTIONARY), Encoding.Base16LowerCase),
				new StringTypePrefixSuffix(new StringTypeEncoded(new StringTypeDictionary(IntArrayStore.create(10), DICTIONARY), Encoding.Base16LowerCase), "a", "b"),

				new StringTypeNumber(new IntegerRange(0, 7), ByteArrayStore.create(10)),
				new StringTypeSingleton("a", BitSetStore.create(10)),
				new DateRangeTypeDateRange(IntegerDateStore.create(10), IntegerDateStore.create(10)),
				new DateRangeTypeQuarter(LongArrayStore.create(10)),
				new IntegerDateStore(LongArrayStore.create(10)),

				DecimalArrayStore.create(10),
				LongArrayStore.create(10),
				IntArrayStore.create(10),
				ByteArrayStore.create(10),
				ShortArrayStore.create(10),
				FloatArrayStore.create(10),
				DoubleArrayStore.create(10),
				BitSetStore.create(10),
				EmptyStore.INSTANCE,
				new RebasingStore(10, 10, IntArrayStore.create(10))
		);
	}

	@ParameterizedTest
	@MethodSource("createCTypes")
	public void testSerialization(ColumnStore type) throws IOException, JSONException {
		SerializationTestUtil
				.forType(ColumnStore.class)
				.registry(CENTRAL_REGISTRY)
				.test(type);
	}
}
