package com.bakdata.conquery.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ConqueryEscapeTest {

	@ParameterizedTest
	@CsvSource(value= {
			"hallo,hallo",
			"test,test",
			"test7,test7",
			"7test,7test",
			"$,%24",
			"$24,%2424",
			"PO!\"$%&/()=ß,PO%21%22%24%25%26%2F%28%29%3D%C3%9F",
			"😈,%F0%9F%98%88",
			"aa.aa,aa%2eaa",
			"a_a,a_a",
			"a-a,a-a"
	})
	public void testEscaping(String in, String expectedEscaped) {
		String escaped1 = ConqueryEscape.escape(in);
		assertThat(escaped1).isEqualTo(expectedEscaped);

		String escaped2 = ConqueryEscape.escape(escaped1);
		String unescaped2 = ConqueryEscape.unescape(escaped2);
		assertThat(unescaped2).isEqualTo(escaped1);
		
		String unescaped1 = ConqueryEscape.unescape(unescaped2);
		assertThat(unescaped1).isEqualTo(in);
	}
}
