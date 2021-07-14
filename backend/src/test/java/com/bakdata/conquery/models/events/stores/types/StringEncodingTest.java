package com.bakdata.conquery.models.events.stores.types;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;


@Slf4j
public class StringEncodingTest {

	@TestFactory
	public Stream<DynamicTest> testEncodings() {

		StringTypeEncoded.Encoding encoding = StringTypeEncoded.Encoding.Base64;

		return Stream.generate(() -> UUID.randomUUID().toString().replace("-", ""))
					 .map(uuid -> DynamicTest.dynamicTest(uuid, () -> {
						 byte[] decoded = encoding.encode(uuid);
						 String encoded = encoding.decode(decoded);

						 assertThat(encoded).isEqualTo(uuid);
						 assertThat(decoded.length).isLessThan(uuid.length());
					 }))
					 .limit(100);
	}

	@Test
	public void testHexStreamStringType() {
		StringParser parser = new StringParser(new ParserConfig());

		Stream.generate(() -> UUID.randomUUID().toString().replace("-", ""))
			  .map(String::toUpperCase)
			  .mapToInt(v -> {
				  try {
					  return parser.parse(v);
				  }
				  catch (ParsingException e) {
					  return 0; // We know that StringTypeVarInt is able to parse our strings.
				  }
			  })
			  .limit(100)
			  .forEach(parser::addLine);


		StringTypeEncoded subType = (StringTypeEncoded) parser.findBestType();

		assertThat(subType)
				.isInstanceOf(StringTypeEncoded.class);
		assertThat(subType.getEncoding()).isEqualByComparingTo(StringTypeEncoded.Encoding.Base16UpperCase);
	}
}