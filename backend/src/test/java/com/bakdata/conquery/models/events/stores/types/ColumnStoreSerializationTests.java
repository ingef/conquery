package com.bakdata.conquery.models.events.stores.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
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
import com.bakdata.conquery.models.events.stores.specific.CompoundDateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.DirectDateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.events.stores.specific.QuarterDateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingIntegerStore;
import com.bakdata.conquery.models.events.stores.specific.ScaledDecimalStore;
import com.bakdata.conquery.models.events.stores.specific.string.DictionaryStore;
import com.bakdata.conquery.models.events.stores.specific.string.EncodedStringStore;
import com.bakdata.conquery.models.events.stores.specific.string.EncodedStringStore.Encoding;
import com.bakdata.conquery.models.events.stores.specific.string.NumberStringStore;
import com.bakdata.conquery.models.events.stores.specific.string.PrefixSuffixStringStore;
import com.bakdata.conquery.models.events.stores.specific.string.SingletonStringStore;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.dropwizard.jersey.validation.Validators;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ColumnStoreSerializationTests {

	/**
	 * Set of {@link ColumnStore}-Types that cannot be tested because it needs more inputs than just one class.
	 * For {@link CompoundDateRangeStore} a manual test is done in {@link com.bakdata.conquery.models.SerializationTests}
	 */
	private static final Set<Class<? extends ColumnStore>> EXCLUDING = Set.of(CompoundDateRangeStore.class);

	private static final CentralRegistry CENTRAL_REGISTRY = new CentralRegistry();
	private static final Dictionary DICTIONARY = new MapDictionary(Dataset.PLACEHOLDER, "dictionary");

	private static ObjectMapper shardInternalMapper;

	@BeforeAll
	public static void setupRegistry() {
		CENTRAL_REGISTRY.register(Dataset.PLACEHOLDER);
		CENTRAL_REGISTRY.register(DICTIONARY);


		// Prepare shard node internal mapper
		final ShardNode shardNode = mock(ShardNode.class);
		when(shardNode.getConfig()).thenReturn(new ConqueryConfig());
		when(shardNode.getValidator()).thenReturn(Validators.newValidator());

		when(shardNode.createInternalObjectMapper(any())).thenCallRealMethod();
		shardInternalMapper = shardNode.createInternalObjectMapper(View.Persistence.Shard.class);
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
				new ScaledDecimalStore(13, IntArrayStore.create(10)),
				new MoneyIntStore(IntArrayStore.create(10)),
				new DictionaryStore(IntArrayStore.create(10), DICTIONARY),
				new EncodedStringStore(new DictionaryStore(IntArrayStore.create(10), DICTIONARY), Encoding.Base16LowerCase),
				new PrefixSuffixStringStore(new EncodedStringStore(new DictionaryStore(IntArrayStore.create(10), DICTIONARY), Encoding.Base16LowerCase), "a", "b"),

				new NumberStringStore(new Range.IntegerRange(0, 7), ByteArrayStore.create(10)),
				new SingletonStringStore("a", BitSetStore.create(10)),
				new DirectDateRangeStore(IntegerDateStore.create(10), IntegerDateStore.create(10)),
				new QuarterDateRangeStore(LongArrayStore.create(10)),
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
				new RebasingIntegerStore(10, 10, IntArrayStore.create(10))
		);
	}

	@ParameterizedTest
	@MethodSource("createCTypes")
	public void testSerialization(ColumnStore type) throws IOException, JSONException {

		SerializationTestUtil
				.forType(ColumnStore.class)
				.objectMappers(shardInternalMapper)
				.registry(CENTRAL_REGISTRY)
				.test(type);
	}
}
