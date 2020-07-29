package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Namespaces extends NamespaceCollection implements Closeable {

	private ConcurrentMap<DatasetId, Namespace> datasets = new ConcurrentHashMap<>();
	@NotNull
	@Getter
	@Setter
	private IdMap<WorkerId, WorkerInformation> workers = new IdMap<>();
	@Getter
	@JsonIgnore
	private transient ConcurrentMap<SocketAddress, SlaveInformation> slaves = new ConcurrentHashMap<>();
	@Getter @Setter @JsonIgnore
	private transient MasterMetaStorage metaStorage;

	public void add(Namespace ns) {
		datasets.put(ns.getStorage().getDataset().getId(), ns);
		ns.setNamespaces(this);
	}

	public Namespace get(DatasetId dataset) {
		return datasets.get(dataset);
	}
	
	public void removeNamespace(DatasetId id) {
		Namespace removed = datasets.remove(id);

		if(removed != null) {
			metaStorage.getCentralRegistry().remove(id);

			workers.keySet().removeIf(w->w.getDataset().equals(id));
			try {
				// remove all associated data.
				removed.getStorage().remove();
			}
			catch(Exception e) {
				log.error("Failed to delete storage "+removed, e);
			}
		}
	}

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException {
		if (!datasets.containsKey(dataset)) {
			throw new NoSuchElementException(String.format("Did not find Dataset[%s] in [%s]", dataset, datasets.keySet()));
		}

		return datasets.get(dataset).getStorage().getCentralRegistry();
	}
	
	@Override
	public CentralRegistry getMetaRegistry() {
		return metaStorage.getCentralRegistry();
	}

	public synchronized void register(SlaveInformation slave, WorkerInformation info) {
		WorkerInformation old = workers.getOptional(info.getId()).orElse(null);
		if (old != null) {
			old.setIncludedBuckets(info.getIncludedBuckets());
			old.setConnectedSlave(slave);
		}
		else {
			info.setConnectedSlave(slave);
			workers.add(info);
		}

		Namespace ns = datasets.get(info.getDataset());
		if (ns == null) {
			throw new NoSuchElementException(
				"Trying to register a worker for unknown dataset '" + info.getDataset() + "'. I only know " + datasets.keySet());
		}
		ns.addWorker(info);
	}

	public List<Dataset> getAllDatasets() {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toList());
	}
	
	public <C extends Collection<Dataset>> C getAllDatasets(Supplier<C> collectionSupplier) {
		return datasets.values().stream().map(Namespace::getStorage).map(NamespaceStorage::getDataset).collect(Collectors.toCollection(collectionSupplier));
	}

	public Collection<Namespace> getNamespaces() {
		return datasets.values();
	}
	
	public void close() {
		for(Namespace namespace : datasets.values()) {
			try {
				namespace.close();				
			}catch (Exception e) {
				log.error("Unable to close namespace {}", namespace, e);
			}
		}
	}
}
