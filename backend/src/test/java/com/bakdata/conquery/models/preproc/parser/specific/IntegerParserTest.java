package com.bakdata.conquery.models.preproc.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.ByteArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.IntArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.LongArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingIntegerStore;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ObjectAssertFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class IntegerParserTest {

	public static Stream<Arguments> arguments() {
		return Stream.of(
				// Long is everything not match-able to other sizes.
				Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE * 2L, direct(LongArrayStore.class)),

				// IntegerStore
				Arguments.of(Integer.MIN_VALUE, (long) Integer.MAX_VALUE - 1L, direct(IntArrayStore.class)),
				Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + (long) Integer.MAX_VALUE - 1L, rebased(IntArrayStore.class)),

				// ByteArrayStore
				Arguments.of(Byte.MIN_VALUE, Byte.MAX_VALUE - 1L, direct(ByteArrayStore.class)),
				Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 255 - 1L, rebased(ByteArrayStore.class)),
				Arguments.of(Short.MAX_VALUE, Short.MAX_VALUE + 255 - 1L, rebased(ByteArrayStore.class)),

				// ShortArrayStore
				Arguments.of(Short.MIN_VALUE, Short.MAX_VALUE - 1L, direct(ShortArrayStore.class)),
				Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE + ((long) Short.MAX_VALUE - (long) Short.MIN_VALUE) - 1L, rebased(ShortArrayStore.class)),
				Arguments.of(Short.MAX_VALUE, Short.MAX_VALUE + (Short.MAX_VALUE - Short.MIN_VALUE) - 1L, rebased(ShortArrayStore.class))
		);
	}

	public static Consumer<ColumnStore> direct(Class<?> clazz) {
		return store -> assertThat(store).isInstanceOf(clazz);
	}

	public static Consumer<ColumnStore> rebased(Class<?> clazz) {
		return store -> {
			assertThat(store)
					.asInstanceOf(new InstanceOfAssertFactory<>(RebasingIntegerStore.class, new ObjectAssertFactory<>()))
					.extracting(RebasingIntegerStore::getStore)
					.isInstanceOf(clazz);
		};
	}


	@ParameterizedTest
	@MethodSource("arguments")
	public void test(long min, long max, Consumer<IntegerStore> test) {
		final IntegerParser parser = new IntegerParser(new ConqueryConfig());
		parser.setMinValue(min);
		parser.setMaxValue(max);

		assertThat(parser.decideType())
				.satisfies(test);
	}
}