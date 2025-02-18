package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_TABLE", base=NamespacedMessage.class) @Slf4j
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
@Data
public class UpdateTable extends WorkerMessage {

	private final Table table;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received update of Table {}", table.getId());
		synchronized (context.getStorage()) {
			context.addTable(table);
		}
	}
}
