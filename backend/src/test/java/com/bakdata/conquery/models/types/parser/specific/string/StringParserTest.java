package com.bakdata.conquery.models.types.parser.specific.string;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.specific.AStringType;
import org.junit.jupiter.api.Test;

class StringParserTest {
	@Test
	public void testOverlappingPrefixSuffix() throws ParsingException {
		final StringParser parser = new StringParser(new ParserConfig());

		int abb_bba = parser.parseValue("abb_bba");
		int abbbba = parser.parseValue("abbbba");
		int abbba = parser.parseValue("abbba");
		int abba = parser.parseValue("abba");

		assertThat(parser.getPrefix()).isEqualTo("abb");
		assertThat(parser.getSuffix()).isEqualTo("bba");

		final AStringType<?> integerDecision = (AStringType<?>) parser.decideType().getType();

		assertThat(integerDecision.getElement(abb_bba)).isEqualTo("abb_bba");
		assertThat(integerDecision.getElement(abbbba)).isEqualTo("abbbba");
		assertThat(integerDecision.getElement(abbba)).isEqualTo("abbba");
		assertThat(integerDecision.getElement(abba)).isEqualTo("abba");

	}

}