package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="ADD_IMPORT", base=NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Slf4j
@Getter
public class AddImport extends WorkerMessage {

	private final Import imp;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received Import[{}], containing {} entries.", imp.getId(), imp.getNumberOfEntries());
		context.addImport(imp);
	}

}
