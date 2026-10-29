package com.bakdata.conquery.models.preproc.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import org.junit.jupiter.api.Test;

class RealParserTest {

	@Test
	public void ulpInRange() {
		final ConqueryConfig config = new ConqueryConfig();
		config.getPreprocessor().getParsers().setMinPrecision(Math.ulp(10));

		final RealParser realParser = new RealParser(config);

		realParser.addDouble(1d);
		realParser.addDouble(2d);
		realParser.addDouble(-3d);

		// ULP is symmetric on the 0 axis and monotonic, so larger values have a larger ULP

		assertThat(realParser.decideType(realParser.getLines())).isInstanceOf(FloatArrayStore.class);

		realParser.addDouble(3000d);

		assertThat(realParser.decideType(realParser.getLines())).isInstanceOf(DoubleArrayStore.class);
	}

}
