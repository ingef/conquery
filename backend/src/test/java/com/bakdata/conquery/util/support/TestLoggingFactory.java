package com.bakdata.conquery.util.support;

import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jmx.JMXConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.util.StatusPrinter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.logback.InstrumentedAppender;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.LoggingFactory;
import io.dropwizard.logging.LoggingUtil;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.filter.ThresholdLevelFilterFactory;
import io.dropwizard.logging.layout.DropwizardLayoutFactory;
import io.dropwizard.logging.layout.LayoutFactory;

public class TestLoggingFactory implements LoggingFactory {

	public static final String LOG_PATTERN = "[%level] [%date{yyyy-MM-dd HH:mm:ss}]\t%logger{10}\t%mdc{location}\t%message%n";
	
	private static final ReentrantLock MBEAN_REGISTRATION_LOCK = new ReentrantLock();
	private static final ReentrantLock CHANGE_LOGGER_CONTEXT_LOCK = new ReentrantLock();

	@JsonIgnore
	private final LoggerContext loggerContext;

	@JsonIgnore
	private final PrintStream configurationErrorsStream;

	public TestLoggingFactory() {
		this(LoggingUtil.getLoggerContext(), System.err);
	}

	@VisibleForTesting
	TestLoggingFactory(LoggerContext loggerContext, PrintStream configurationErrorsStream) {
		this.loggerContext = requireNonNull(loggerContext);
		this.configurationErrorsStream = requireNonNull(configurationErrorsStream);
	}

	@Override
	public void configure(MetricRegistry metricRegistry, String name) {
		LoggingUtil.hijackJDKLogging();

		CHANGE_LOGGER_CONTEXT_LOCK.lock();
		final Logger root;
		try {
			root = configureLoggers();
		}
		finally {
			CHANGE_LOGGER_CONTEXT_LOCK.unlock();
		}

		final LevelFilterFactory<ILoggingEvent> levelFilterFactory = new ThresholdLevelFilterFactory();
		final AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory = new AsyncLoggingEventAppenderFactory();
		final LayoutFactory<ILoggingEvent> layoutFactory = new DropwizardLayoutFactory();

		ConsoleAppenderFactory<ILoggingEvent> consoleAppender = new ConsoleAppenderFactory<>();
		consoleAppender.setLogFormat(LOG_PATTERN);
		root.addAppender(consoleAppender.build(loggerContext, name, layoutFactory, levelFilterFactory, asyncAppenderFactory));

		StatusPrinter.setPrintStream(configurationErrorsStream);
		try {
			StatusPrinter.printIfErrorsOccured(loggerContext);
		}
		finally {
			StatusPrinter.setPrintStream(System.out);
		}

		final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		MBEAN_REGISTRATION_LOCK.lock();
		try {
			final ObjectName objectName = new ObjectName("io.dropwizard:type=Logging");
			if (!server.isRegistered(objectName)) {
				server.registerMBean(new JMXConfigurator(loggerContext, server, objectName), objectName);
			}
		}
		catch (MalformedObjectNameException | InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException e) {
			throw new RuntimeException(e);
		}
		finally {
			MBEAN_REGISTRATION_LOCK.unlock();
		}

		configureInstrumentation(root, metricRegistry);
	}

	@Override
	public void stop() {
		// Should acquire the lock to avoid concurrent listener changes
		CHANGE_LOGGER_CONTEXT_LOCK.lock();
		try {
			// We need to go through a list of appenders and locate the async ones,
			// as those could have messages left to write. Since there is no flushing
			// mechanism built into logback, we wait for a short period of time before
			// giving up that the appender will be completely flushed.
			final Logger logger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
			final ArrayList<Appender<ILoggingEvent>> appenders = Lists.newArrayList(logger.iteratorForAppenders());
			for (Appender<ILoggingEvent> appender : appenders) {
				if (appender instanceof AsyncAppender) {
					flushAppender((AsyncAppender) appender);
				}
			}
		}
		catch (InterruptedException ignored) {
			// If the thread waiting for the logs to be flushed is aborted then
			// user clearly wants the application to quit now, so stop trying
			// to flush any appenders
			Thread.currentThread().interrupt();
		}
		finally {
			CHANGE_LOGGER_CONTEXT_LOCK.unlock();
		}
	}

	@Override
	public void reset() {
	}

	private void flushAppender(AsyncAppender appender) throws InterruptedException {
		int timeWaiting = 0;
		while (timeWaiting < appender.getMaxFlushTime() && appender.getNumberOfElementsInQueue() > 0) {
			Thread.sleep(100);
			timeWaiting += 100;
		}

		if (appender.getNumberOfElementsInQueue() > 0) {
			// It may seem odd to log when we're trying to flush a logger that
			// isn't flushing, but the same warning is issued inside
			// appender.stop() if the appender isn't able to flush.
			appender.addWarn(appender.getNumberOfElementsInQueue() + " events may be discarded");
		}
	}

	private void configureInstrumentation(Logger root, MetricRegistry metricRegistry) {
		final InstrumentedAppender appender = new InstrumentedAppender(metricRegistry);
		appender.setContext(loggerContext);
		appender.start();
		root.addAppender(appender);
	}

	private Logger configureLoggers() {
		final Logger root = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		loggerContext.reset();

		final LevelChangePropagator propagator = new LevelChangePropagator();
		propagator.setContext(loggerContext);
		propagator.setResetJUL(true);

		loggerContext.addListener(propagator);

		root.setLevel(Level.WARN);

		loggerContext.getLogger("com.bakdata").setLevel(Level.DEBUG);
		loggerContext.getLogger("com.bakdata.conquery.io.storage.xodus.stores").setLevel(Level.DEBUG);

		return root;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).toString();
	}
}
