package com.bakdata.conquery.models.messages.network.specific;

import java.io.File;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.io.xodus.WorkerStorageImpl;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Slave;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.annotation.JsonCreator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="ADD_WORKER", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class AddWorker extends SlaveMessage.Slow {

	private final Dataset dataset;
	
	@Override
	public void react(Slave context) throws Exception {
		log.info("creating a new worker for {}", dataset);
		ConqueryConfig config = context.getConfig();
		File dir = createWorkerName(config);
		WorkerInformation info = new WorkerInformation();
		info.setDataset(dataset.getId());
		info.setIncludedBuckets(new IntArrayList());
		info.setName(dir.getName());
		WorkerStorage workerStorage = new WorkerStorageImpl(context.getValidator(), config.getStorage(), dir);
		workerStorage.loadData();
		workerStorage.updateDataset(dataset);
		Worker worker = new Worker(
			info,
			context.getJobManager(),
			workerStorage,
			new QueryExecutor(config)
		);
		worker.setSession(context.getRawSession());
		workerStorage.setWorker(info);
		context.getWorkers().add(worker);
		context.send(new RegisterWorker(worker.getInfo()));
	}

	private File createWorkerName(ConqueryConfig config) {
		String name = "worker_"+dataset.getId()+"_"+UUID.randomUUID();
		return new File(config.getStorage().getDirectory(), name);
	}
}
