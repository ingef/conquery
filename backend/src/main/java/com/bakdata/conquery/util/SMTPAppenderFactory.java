package com.bakdata.conquery.util;

import java.net.URI;

import ch.qos.logback.access.net.SMTPAppender;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonTypeName("SMTP")
@Data
public class SMTPAppenderFactory extends AbstractAppenderFactory<IAccessEvent> {

	private String from;
	private String[] to;
	private URI server;
	private String subject;


	@Override
	public Appender<IAccessEvent> build(LoggerContext context, String applicationName, LayoutFactory<IAccessEvent> layoutFactory, LevelFilterFactory<IAccessEvent> levelFilterFactory, AsyncAppenderFactory<IAccessEvent> asyncAppenderFactory) {
		final SMTPAppender appender = new SMTPAppender();
		appender.setName("smtp-appender");

		appender.setFrom(getFrom());
		appender.setSmtpHost(server.getHost());
		appender.setSMTPPort(server.getPort());

		if (!Strings.isNullOrEmpty(getServer().getUserInfo())) {
			final String[] userInfo = getServer().getUserInfo().split(":");
			final String userName = userInfo[0];
			final String password = userInfo.length > 1 ? userInfo[1] : null;

			appender.setPassword(password);
			appender.setUsername(userName);
		}

		appender.setSubject(getSubject());

		for (String target : getTo()) {
			appender.addTo(target);
		}

		appender.setContext(context);
		appender.setAsynchronousSending(true);

		appender.setLayout(buildLayout(context, layoutFactory));
		appender.addFilter(levelFilterFactory.build(threshold));
		getFilterFactories().forEach(f -> appender.addFilter(f.build()));

		appender.start();
		return wrapAsync(appender, asyncAppenderFactory);
	}
}
