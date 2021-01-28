package com.bakdata.conquery.io.xodus;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.metrics.JobMetrics;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class of persistent storages to uniformly handle load of data and closing of storages.
 */
@Getter @Slf4j
public abstract class ConqueryStorageXodus implements ConqueryStorage {

	protected final Validator validator;
	protected final XodusStorageFactory config;
	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
	private final List<KeyIncludingStore<?,?>> stores = new ArrayList<>();
	
	private final Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores = MultimapBuilder.linkedHashKeys().arrayListValues().build();

	public ConqueryStorageXodus(Validator validator, XodusStorageFactory config) {
		this.validator = validator;
		this.config = config;
	}

	/**
	 * Stores can contain of multiple environments, whose themselves can consist of multiple stores.
	 * This method collects this information as a mapping that is used to load and later close the stores.
	 * The environments should be collected in the order the stores should be loaded.
	 */
	abstract protected void createStores(Multimap<Environment, KeyIncludingStore<?,?>> environmentToStores);

	/**
	 * Load all stores from disk.
	 */
	@Override
	public void loadData() {
		createStores(environmentToStores);
		for(Environment environment : environmentToStores.keySet()) {
			log.info("Loading storage {} from {}", this.getClass().getSimpleName(), environment.getLocation());
			
			try (final Timer.Context timer = JobMetrics.getStoreLoadingTimer()) {
				Stopwatch all = Stopwatch.createStarted();
				for (KeyIncludingStore<?, ?> store : environmentToStores.get(environment)) {
					store.loadData();
				}
				log.info("Loading of environment {} completed within {}", environment.getLocation(), all.stop());
			}
		}
	}
	

	@Override
	public void close() throws IOException {
		for(Environment environment : environmentToStores.keySet()) {
			log.info("Closing environment {}", environment.getLocation());
			environment.close();
			log.info("Closed environment {}", environment.getLocation());
		}
	}
	


	/**
	 * Clears the environment.
	 */
	@Override
	public void clear() {
		for(Environment environment : environmentToStores.keySet()) {
			log.info("Clearing environment {}", environment.getLocation());
			environment.clear();
			log.info("Cleared environment {}", environment.getLocation());
		}
	}

	@Override
	@SneakyThrows
	public void remove() {
		for(Environment environment : environmentToStores.keySet()) {
			log.info("Removing {}", environment.getLocation());
			Files.walkFileTree(Path.of(environment.getLocation()),
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							if (!dir.toFile().delete()) {
								throw new IOException("Could not delete " + dir);
							}
							return super.postVisitDirectory(dir, exc);
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (!file.toFile().delete()) {
								throw new IOException("Could not delete " + file);
							}
							return super.visitFile(file, attrs);
						}
					});

			Path.of(environment.getLocation()).toFile().delete();
		}
	}
}
