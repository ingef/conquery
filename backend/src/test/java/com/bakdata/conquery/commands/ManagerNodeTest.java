package com.bakdata.conquery.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import io.dropwizard.core.setup.Environment;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ManagerNodeTest {

	private final DatasetRegistry<Namespace> registry = mock(DatasetRegistry.class);
	private final MetaStorage metaStorage = mock(MetaStorage.class);
	private final Environment environment = mock(Environment.class);

	@Test
	void loadNamespacesLoadsEveryDiscoveredNamespace() {
		NamespaceStorage firstStorage = createStorage("dataset_first");
		NamespaceStorage secondStorage = createStorage("dataset_second");

		assertThatCode(() -> ManagerNode.loadNamespaces(List.of(firstStorage, secondStorage), registry, metaStorage, environment))
				.doesNotThrowAnyException();

		verify(registry).createNamespace(firstStorage, metaStorage, environment);
		verify(registry).createNamespace(secondStorage, metaStorage, environment);
	}

	@Test
	void loadNamespacesReportsOriginalFailureAndAbortsStartup() {
		NamespaceStorage storage = createStorage("dataset_broken");
		IllegalArgumentException originalFailure = new IllegalArgumentException("Unknown SQL data source");
		doThrow(originalFailure).when(registry).createNamespace(storage, metaStorage, environment);

		ListAppender<ILoggingEvent> appender = attachLogAppender();
		try {
			Throwable thrown = catchThrowable(() -> ManagerNode.loadNamespaces(List.of(storage), registry, metaStorage, environment));

			assertThat(thrown)
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Failed to load 1 of 1 persisted dataset namespaces: [dataset_broken]")
					.hasCause(originalFailure);
			assertThat(appender.list)
					.singleElement()
					.satisfies(event -> {
						assertThat(event.getLevel()).isEqualTo(Level.ERROR);
						assertThat(event.getFormattedMessage()).isEqualTo(
								"Failed to load persisted dataset namespace [storage=dataset_broken]. "
								+ "The dataset is unavailable; application startup will be aborted."
						);
						assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalArgumentException.class.getName());
						assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Unknown SQL data source");
					});
		}
		finally {
			detachLogAppender(appender);
		}
	}

	@Test
	void loadNamespacesReportsAllFailures() {
		NamespaceStorage firstStorage = createStorage("dataset_first");
		NamespaceStorage workingStorage = createStorage("dataset_working");
		NamespaceStorage secondStorage = createStorage("dataset_second");
		IllegalStateException firstFailure = new IllegalStateException("First failure");
		IllegalArgumentException secondFailure = new IllegalArgumentException("Second failure");
		doThrow(firstFailure).when(registry).createNamespace(firstStorage, metaStorage, environment);
		doThrow(secondFailure).when(registry).createNamespace(secondStorage, metaStorage, environment);

		ListAppender<ILoggingEvent> appender = attachLogAppender();
		try {
			Throwable thrown = catchThrowable(
					() -> ManagerNode.loadNamespaces(List.of(firstStorage, workingStorage, secondStorage), registry, metaStorage, environment)
			);

			assertThat(thrown)
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Failed to load 2 of 3 persisted dataset namespaces: [dataset_first, dataset_second]")
					.hasCause(firstFailure);
			assertThat(thrown.getSuppressed()).containsExactly(secondFailure);
			assertThat(appender.list)
					.extracting(ILoggingEvent::getFormattedMessage)
					.containsExactly(
							"Failed to load persisted dataset namespace [storage=dataset_first]. "
							+ "The dataset is unavailable; application startup will be aborted.",
							"Failed to load persisted dataset namespace [storage=dataset_second]. "
							+ "The dataset is unavailable; application startup will be aborted."
					);
			verify(registry).createNamespace(workingStorage, metaStorage, environment);
		}
		finally {
			detachLogAppender(appender);
		}
	}

	private NamespaceStorage createStorage(String pathName) {
		NamespaceStorage storage = mock(NamespaceStorage.class);
		when(storage.getPathName()).thenReturn(pathName);
		return storage;
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(ManagerNode.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(ManagerNode.class);
		logger.detachAppender(appender);
		appender.stop();
	}
}
