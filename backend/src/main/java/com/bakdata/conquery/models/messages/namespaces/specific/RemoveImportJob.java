package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Getter
@CPSType(id="REMOVE_IMPORT", base= NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)  @ToString
@Slf4j
public class RemoveImportJob extends WorkerMessage {

	private final ImportId imp;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Deleting Import[{}]", imp);

		context.removeImport(imp.resolve());
	}
}
