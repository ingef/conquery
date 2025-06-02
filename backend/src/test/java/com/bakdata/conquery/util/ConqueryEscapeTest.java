package com.bakdata.conquery.util;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

public class ConqueryEscapeTest {

	@ParameterizedTest
	@CsvSource(value = {
			"hallo,hallo",
			"test,test",
			"test7,test7",
			"7test,7test",
			"$,$24",
			"$24,$2424",
			"PO!\"$%&/()=ß,PO$21$22$24$25$26$2f$28$29$3d$c3$9f",
			"😈,$f0$9f$98$88",
			"aa.aa,aa$2eaa",
			"a_a,a_a",
			"a-a,a-a",
			"a/a,a$2fa",
			"a a,a$20a",
			"`az{,$60az$7b", // Range border characters
			"@AZ[,$40AZ$5b",
			"/09:,$2f09$3a",
			"test@example.org,test$40example$2eorg"
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

	/**
	 * Test that a warning is written if we encounter a string to decode that contains decoded character.
	 * Here '@' raises a warning because it should have been '$40'
	 */
	@Test
	@SuppressWarnings("unused") // _unescaped
	public void testEscapingWarningUnescaped() {
		Logger conqueryEscapeLog = (Logger) LoggerFactory.getLogger(ConqueryEscape.class);

		// Add appender to capture warning
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		conqueryEscapeLog.addAppender(listAppender);

		try {
			String _unescaped = ConqueryEscape.unescape("test@example$2eorg");

			assertThat(listAppender.list.getFirst())
					.extracting(
							ILoggingEvent::getLevel,
							ILoggingEvent::getFormattedMessage
					)
					.containsExactly(
							ch.qos.logback.classic.Level.WARN,
							"Unescaped character '64' at 4 in 'test@example$2eorg'"
					);
		}
		finally {
			// Cleanup appender
			conqueryEscapeLog.detachAppender(listAppender);
			listAppender.stop();
		}
	}
}
