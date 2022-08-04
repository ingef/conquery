package com.bakdata.conquery;

import com.google.common.base.CharMatcher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MatcherTest {
	@Test
	void test() {

		log.info("result {}", CharMatcher.whitespace().trimAndCollapseFrom("hekd dskf d    dkfjdk", ' '));
	}
}
