package com.bakdata.conquery.util;

import ch.qos.logback.access.net.SMTPAppender;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonTypeName("SMTP")
public class SMTPAppenderFactory extends AbstractAppenderFactory<IAccessEvent> {

	@Override
	public Appender<IAccessEvent> build(LoggerContext context, String applicationName, LayoutFactory<IAccessEvent> layoutFactory, LevelFilterFactory<IAccessEvent> levelFilterFactory, AsyncAppenderFactory<IAccessEvent> asyncAppenderFactory) {
		final SMTPAppender appender = new SMTPAppender();
		appender.setName("cq-smtp-appender");

		//TODO configure

		appender.setContext(context);

		appender.setLayout(buildLayout(context, layoutFactory));
		appender.addFilter(levelFilterFactory.build(threshold));
		getFilterFactories().forEach(f -> appender.addFilter(f.build()));

		appender.start();
		return wrapAsync(appender, asyncAppenderFactory);
	}
}
