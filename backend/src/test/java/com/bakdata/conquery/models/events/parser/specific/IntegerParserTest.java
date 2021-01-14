package com.bakdata.conquery.models.events.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
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
				Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE * 2L, direct(LongStore.class)),

				// IntegerStore
				Arguments.of(Integer.MIN_VALUE, (long) Integer.MAX_VALUE - 1L, direct(IntegerStore.class)),
				Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + (long) Integer.MAX_VALUE - 1L, rebased(IntegerStore.class)),

				// ByteStore
				Arguments.of(Byte.MIN_VALUE, Byte.MAX_VALUE - 1L, direct(ByteStore.class)),
				Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 255 - 1L, rebased(ByteStore.class)),
				Arguments.of(Short.MAX_VALUE, Short.MAX_VALUE + 255 - 1L, rebased(ByteStore.class)),

				// ShortStore
				Arguments.of(Short.MIN_VALUE, Short.MAX_VALUE - 1L, direct(ShortStore.class)),
				Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE + ((long) Short.MAX_VALUE - (long) Short.MIN_VALUE) - 1L, rebased(ShortStore.class)),
				Arguments.of(Short.MAX_VALUE, Short.MAX_VALUE + (Short.MAX_VALUE - Short.MIN_VALUE) - 1L, rebased(ShortStore.class))
		);
	}

	public static Consumer<ColumnStore<?>> direct(Class<?> clazz) {
		return store -> assertThat(store).isInstanceOf(clazz);
	}

	public static Consumer<ColumnStore<?>> rebased(Class<?> clazz) {
		return store -> {
			assertThat(store)
					.asInstanceOf(new InstanceOfAssertFactory<>(RebasingStore.class, new ObjectAssertFactory<>()))
					.extracting(RebasingStore::getStore)
					.isInstanceOf(clazz);
		};
	}


	@ParameterizedTest
	@MethodSource("arguments")
	public void test(long min, long max, Consumer<ColumnStore<Long>> test) {
		final IntegerParser parser = new IntegerParser();
		parser.setMinValue(min);
		parser.setMaxValue(max);

		assertThat(parser.decideType())
				.satisfies(test);
	}
}