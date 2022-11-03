package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@CPSType(id="REMOVE_IMPORT", base= NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)  @ToString
@Slf4j
public class RemoveImportJob extends WorkerMessage {

	@NsIdRef
	private final Import imp;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Deleting Import[{}]", imp);

		context.removeImport(imp);
	}
}
