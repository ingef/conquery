package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;

public class RequestConsistency extends MessageToShardNode {
    @Override
    public void react(NetworkMessageContext.ShardNodeNetworkContext context) throws Exception {

        context.send(new ReportConsistency(worker.getInfo()));
    }
}
