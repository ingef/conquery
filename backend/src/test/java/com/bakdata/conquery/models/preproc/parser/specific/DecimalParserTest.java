package com.bakdata.conquery.models.preproc.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DecimalArrayStore;
import com.bakdata.conquery.models.events.stores.specific.ScaledDecimalStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DecimalParserTest {

	@Test
	public void test(){
		final DecimalParser parser = new DecimalParser(new ConqueryConfig());

		parser.addDecimal(BigDecimal.valueOf(10,1000));
		parser.addDecimal(BigDecimal.valueOf(30,1000));

		assertThat(parser.decideType(parser.getLines())).isInstanceOf(ScaledDecimalStore.class);

		parser.addDecimal(BigDecimal.valueOf(2, 62));

		assertThat(parser.decideType(parser.getLines())).isInstanceOf(DecimalArrayStore.class);
	}

}
