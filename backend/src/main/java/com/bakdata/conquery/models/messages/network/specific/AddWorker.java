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
		File dir = createWorkerName(context);
		WorkerInformation info = new WorkerInformation();
		info.setDataset(dataset.getId());
		info.setIncludedBuckets(new IntArrayList());
		info.setName(dir.getName());
		WorkerStorage workerStorage = new WorkerStorageImpl(context.getValidator(), config.getStorage(), dir);
		workerStorage.loadData();
		workerStorage.updateDataset(dataset);


		Worker worker = context.getWorkers().createWorker(info, workerStorage);

		worker.setSession(context.getRawSession());
		workerStorage.setWorker(info);
		context.send(new RegisterWorker(worker.getInfo()));
	}

	private File createWorkerName(Slave context) {
		String name = "worker_"+dataset.getName()+"_"+UUID.randomUUID().toString();
		return new File(context.getConfig().getStorage().getDirectory(), name);
	}
}
