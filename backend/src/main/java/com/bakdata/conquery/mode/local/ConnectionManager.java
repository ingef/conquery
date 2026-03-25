package com.bakdata.conquery.mode.local;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.datasets.Dataset;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.jooq.DSLContext;

@Data
public class ConnectionManager {
	@Getter(AccessLevel.NONE)
	private final Map<String, ManagedConnection> connections = new HashMap<>();

	public void addConnection(String name, ManagedConnection connection) {
		connections.put(name, connection);
	}

	public DSLContext connect(Dataset dataset) {
		return getConnection(dataset).connect();
	}

	public ManagedConnection getConnection(Dataset dataset) {
		return Objects.requireNonNull(connections.get(dataset.getDataSource()), () -> "No connection available for %s".formatted(dataset));
	}

}
