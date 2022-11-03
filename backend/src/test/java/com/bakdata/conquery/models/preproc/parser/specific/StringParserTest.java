package com.bakdata.conquery.models.preproc.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;


class StringParserTest {
	@Test
	public void isOnlyDigits() {
		assertThat(StringParser.isOnlyDigits("01")).isFalse();
	}

}