package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="REMOVE_TABLE", base=NamespacedMessage.class) @Slf4j
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString(callSuper=true)
public class RemoveTable extends WorkerMessage {

	@NsIdRef
	private Table table;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received update of Table {}", table);
		synchronized (context.getStorage()) {
			context.removeTable(table);
		}
	}
}
