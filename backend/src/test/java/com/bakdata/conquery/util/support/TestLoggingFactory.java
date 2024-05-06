package com.bakdata.conquery.util.support;

import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.util.StatusPrinter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.LoggingFactory;
import io.dropwizard.logging.common.LoggingUtil;
import io.dropwizard.logging.common.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.common.filter.ThresholdLevelFilterFactory;
import io.dropwizard.logging.common.layout.DropwizardLayoutFactory;

public class TestLoggingFactory implements LoggingFactory {

	public static final String LOG_PATTERN = "[%level] [%date{yyyy-MM-dd HH:mm:ss}]\t%logger{10}\t%mdc{location}\t%message%n";

	private static final Condition CHANGE_LOGGER_CONTEXT_LOCK = new ReentrantLock().newCondition();

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


		final Logger root;

		synchronized (CHANGE_LOGGER_CONTEXT_LOCK) {
			root = configureLoggers();
		}

		final ConsoleAppenderFactory<ILoggingEvent> consoleAppender = new ConsoleAppenderFactory<>();
		consoleAppender.setLogFormat(LOG_PATTERN);

		root.addAppender(consoleAppender.build(loggerContext, name, new DropwizardLayoutFactory(), new ThresholdLevelFilterFactory(), new AsyncLoggingEventAppenderFactory()));

		StatusPrinter.setPrintStream(configurationErrorsStream);
		try {
			StatusPrinter.printIfErrorsOccured(loggerContext);
		}
		finally {
			StatusPrinter.setPrintStream(System.out);
		}

	}

	private Logger configureLoggers() {
		final Logger root = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		loggerContext.reset();

		final LevelChangePropagator propagator = new LevelChangePropagator();
		propagator.setContext(loggerContext);
		propagator.setResetJUL(true);

		loggerContext.addListener(propagator);

		root.setLevel(Level.INFO);

		loggerContext.getLogger("com.bakdata").setLevel(Level.DEBUG);

		return root;
	}

	@Override
	public void stop() {


		// Should acquire the lock to avoid concurrent listener changes
		synchronized (CHANGE_LOGGER_CONTEXT_LOCK) {

			// We need to go through a list of appenders and locate the async ones,
			// as those could have messages left to write. Since there is no flushing
			// mechanism built into logback, we wait for a short period of time before
			// giving up that the appender will be completely flushed.
			final Logger logger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

			logger.iteratorForAppenders().forEachRemaining(this::flushAppender);
		}

	}

	private void flushAppender(Appender appender) {
		if (!(appender instanceof AsyncAppender)) {
			return;
		}

		final AsyncAppender asyncAppender = (AsyncAppender) appender;
		try {
			int timeWaiting = 0;
			while (timeWaiting < asyncAppender.getMaxFlushTime() && asyncAppender.getNumberOfElementsInQueue() > 0) {
				Thread.sleep(100);
				timeWaiting += 100;
			}

			if (asyncAppender.getNumberOfElementsInQueue() > 0) {
				// It may seem odd to log when we're trying to flush a logger that
				// isn't flushing, but the same warning is issued inside
				// appender.stop() if the appender isn't able to flush.
				asyncAppender.addWarn(asyncAppender.getNumberOfElementsInQueue() + " events may be discarded");
			}
		}
		catch (InterruptedException ignored) {
			// If the thread waiting for the logs to be flushed is aborted then
			// user clearly wants the application to quit now, so stop trying
			// to flush any appenders
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void reset() {
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).toString();
	}
}
