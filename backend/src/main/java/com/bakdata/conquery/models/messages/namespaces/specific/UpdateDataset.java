package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_DATASET", base=NamespacedMessage.class) @Slf4j
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString(callSuper=true)
public class UpdateDataset extends WorkerMessage {

	private Dataset dataset;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received update of dataset {}", dataset.getId());
		synchronized (context.getStorage()) {
			context.updateDataset(dataset);
		}
	}
}
