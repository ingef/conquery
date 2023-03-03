package com.bakdata.conquery.models.messages.network.specific;

import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.ManagerNodeNetworkContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id = "FORWARD_TO_NAMESPACE", base = NetworkMessage.class)
@RequiredArgsConstructor
@Getter
public class ForwardToNamespace extends MessageToManagerNode implements SlowMessage {

	private final DatasetId datasetId;
	private final NamespaceMessage message;

	@Override
	public void react(ManagerNodeNetworkContext context) throws Exception {
		Namespace ns = Objects.requireNonNull(context.getDatasetRegistry().get(datasetId), () -> String.format("Missing dataset `%s`", datasetId));
		ConqueryMDC.setLocation(ns.getStorage().getDataset().toString());
		message.react(ns);
	}

	@Override
	public ProgressReporter getProgressReporter() {
		return ((SlowMessage) message).getProgressReporter();
	}

	@Override
	public void setProgressReporter(ProgressReporter progressReporter) {
		((SlowMessage) message).setProgressReporter(progressReporter);
	}

	@Override
	public String toString() {
		return message.toString() + " for dataset " + datasetId;
	}
}
