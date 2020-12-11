package com.bakdata.conquery.models.events.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ObjectAssertFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class IntegerParserTest {

	public static LongStream arguments(){
		return Stream.of(
				Long.MIN_VALUE,
				Integer.MIN_VALUE,
				Short.MIN_VALUE,
				Byte.MIN_VALUE,
				0,
				Integer.MAX_VALUE,
				Short.MAX_VALUE,
				Byte.MAX_VALUE
		)
					 .mapToLong(Number::longValue);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void byteRange(long root) {
		final int span = Byte.MAX_VALUE - Byte.MIN_VALUE - 1;
		final Class<ByteStore> clazz = ByteStore.class;
		assertChain(root, span, clazz);

		assertChain(root, span / 2, clazz);


		assertExcess(root, span, clazz);
	}


	@ParameterizedTest
	@MethodSource("arguments")
	public void shortRange(long root) {
		final int span = Short.MAX_VALUE - Short.MIN_VALUE - 1;
		final Class<?> clazz = ShortStore.class;

		assertChain(root, span, clazz);

		assertChain(root, span / 2, clazz);

		assertExcess(root, span, clazz);
	}


	@ParameterizedTest
	@MethodSource("arguments")
	public void intRange(long root) {
		final long span = (long) Integer.MAX_VALUE - Integer.MIN_VALUE - 1;
		final Class<?> clazz = IntegerStore.class;

		assertChain(root, span, clazz);

		assertChain(root, span / 2, clazz);

		assertExcess(root, span, clazz);
	}

	private void assertExcess(long root, long span, Class<?> clazz) {
		// barely exceeding the range to the right
		{
			final IntegerParser parser = new IntegerParser();
			parser.setMinValue(root);
			parser.setMaxValue(root + span + 1);


			assertThat(parser.decideType())
					.asInstanceOf(new InstanceOfAssertFactory<>(RebasingStore.class, new ObjectAssertFactory<>()))
					.extracting(RebasingStore::getStore)
					.isNotInstanceOf(clazz);
		}
	}

	public void assertChain(long root, long span, Class<?> clazz) {
		// Full range
		{
			final IntegerParser parser = new IntegerParser();
			parser.setMinValue(root);
			parser.setMaxValue(root + span - 1);

			assertThat(parser.decideType())
					.asInstanceOf(new InstanceOfAssertFactory<>(RebasingStore.class, new ObjectAssertFactory<>()))
					.extracting(RebasingStore::getStore)
					.isInstanceOf(clazz)
			;
		}
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void longRange(long root) {
		final long span = ((long) Integer.MAX_VALUE - Integer.MIN_VALUE)  * 2L;
		final Class<?> clazz = LongStore.class;

		assertChain(root, span, clazz);

		assertChain(root, span / 2, clazz);
	}
}