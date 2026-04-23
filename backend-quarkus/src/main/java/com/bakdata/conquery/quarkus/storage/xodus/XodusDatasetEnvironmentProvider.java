package com.bakdata.conquery.quarkus.storage.xodus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusDatasetEnvironmentProvider {

	private static final String DATASET_PREFIX = "dataset_";

	@ConfigProperty(name = "conquery.storage.xodus.path", defaultValue = "storage/quarkus-meta")
	String xodusPath;

	private final Map<String, Environment> environmentsByDatasetId = new ConcurrentHashMap<>();

	public Environment getOrCreateEnvironment(String datasetId) {
		return environmentsByDatasetId.computeIfAbsent(datasetId, ignored -> Environments.newInstance(datasetPath(datasetId).toFile()));
	}

	public Optional<Environment> findEnvironment(String datasetId) {
		Environment existing = environmentsByDatasetId.get(datasetId);
		if (existing != null) {
			return Optional.of(existing);
		}
		Path path = datasetPath(datasetId);
		if (!Files.isDirectory(path)) {
			return Optional.empty();
		}
		return Optional.of(getOrCreateEnvironment(datasetId));
	}

	public List<String> listDatasetIds() {
		Path base = datasetBasePath();
		if (!Files.isDirectory(base)) {
			return List.of();
		}
		try {
			List<String> ids = new ArrayList<>();
			try (var stream = Files.list(base)) {
				stream.filter(Files::isDirectory)
					  .map(path -> path.getFileName().toString())
					  .filter(name -> name.startsWith(DATASET_PREFIX))
					  .map(name -> name.substring(DATASET_PREFIX.length()))
					  .forEach(ids::add);
			}
			return ids;
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to list dataset environments in " + base, e);
		}
	}

	public void removeEnvironment(String datasetId) {
		Environment environment = environmentsByDatasetId.remove(datasetId);
		if (environment != null) {
			environment.close();
		}
		deleteRecursively(datasetPath(datasetId));
	}

	@PreDestroy
	void closeAll() {
		environmentsByDatasetId.values().forEach(Environment::close);
		environmentsByDatasetId.clear();
	}

	private Path datasetBasePath() {
		return Path.of(xodusPath, "datasets");
	}

	private Path datasetPath(String datasetId) {
		return datasetBasePath().resolve(DATASET_PREFIX + datasetId);
	}

	private void deleteRecursively(Path path) {
		if (!Files.exists(path)) {
			return;
		}
		try (var walk = Files.walk(path)) {
			walk.sorted((left, right) -> right.getNameCount() - left.getNameCount())
				.forEach(current -> {
					try {
						Files.deleteIfExists(current);
					}
					catch (IOException e) {
						throw new IllegalStateException("Failed to delete " + current, e);
					}
				});
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to walk for deletion " + path, e);
		}
	}
}
