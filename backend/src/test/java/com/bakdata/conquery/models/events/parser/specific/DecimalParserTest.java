package com.bakdata.conquery.models.events.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.events.stores.specific.DecimalTypeScaled;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DecimalParserTest {

	@Test
	public void test(){
		final DecimalParser parser = new DecimalParser(new ParserConfig());

		parser.addLine(BigDecimal.valueOf(10,1000));
		parser.addLine(BigDecimal.valueOf(30,1000));

		assertThat(parser.decideType()).isInstanceOf(DecimalTypeScaled.class);

		parser.addLine(BigDecimal.valueOf(2, 62));

		assertThat(parser.decideType()).isInstanceOf(DecimalStore.class);
	}

}