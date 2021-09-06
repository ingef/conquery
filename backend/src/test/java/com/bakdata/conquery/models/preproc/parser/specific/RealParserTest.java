package com.bakdata.conquery.models.preproc.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import org.junit.jupiter.api.Test;

class RealParserTest {

	@Test
	public void ulpInRange() {
		final ConqueryConfig config = new ConqueryConfig();
		config.getPreprocessor().getParsers().setMinPrecision(Math.ulp(10));

		final RealParser realParser = new RealParser(config);

		realParser.registerValue(1d);
		realParser.registerValue(2d);
		realParser.registerValue(-3d);

		// ULP is symmetric on the 0 axis and monotonic, so larger values have a larger ULP

		assertThat(realParser.decideType()).isInstanceOf(FloatArrayStore.class);

		realParser.registerValue(3000d);

		assertThat(realParser.decideType()).isInstanceOf(DoubleArrayStore.class);
	}

}