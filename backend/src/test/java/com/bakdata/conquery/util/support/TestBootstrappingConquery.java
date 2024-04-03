package com.bakdata.conquery.util.support;

import com.bakdata.conquery.Conquery;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.logging.DropwizardLayout;

public class TestBootstrappingConquery extends Conquery {

	@Override
	protected void bootstrapLogging() {
		BootstrapLogging.bootstrap(bootstrapLogLevel(), (ctx, timeZone) -> {
			final DropwizardLayout dropwizardLayout = new DropwizardLayout(ctx, timeZone);
			DropwizardLayout layout = dropwizardLayout;
			layout.setPattern(TestLoggingFactory.LOG_PATTERN);
			return layout;
		});
	}
}
