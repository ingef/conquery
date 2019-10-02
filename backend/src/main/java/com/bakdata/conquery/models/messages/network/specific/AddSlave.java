package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.io.xodus.WorkerStorageImpl;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Slave;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.annotation.JsonCreator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.UUID;

/**
 * Dummy message that is sent to Master, to authenticate the connection as a Slave connection.
 * This helps to avoids retaining connections from non-slaves.
 */
@CPSType(id="ADD_SLAVE", base=NetworkMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Slf4j
public class AddSlave extends MasterMessage {

	@Override
	public void react(NetworkMessageContext.Master context) throws Exception {
		//TODO test if this slave is already registered and send a warning message?
		context.getNamespaces().getSlaves().put(context.getRemoteAddress(), new SlaveInformation(new NetworkSession(context.getSession().getSession())));

		log.info("Slave {} registered.", context.getRemoteAddress());
	}
}
