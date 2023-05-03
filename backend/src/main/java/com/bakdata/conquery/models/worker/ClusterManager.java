package com.bakdata.conquery.models.worker;

import io.dropwizard.lifecycle.Managed;
import org.apache.mina.core.session.IoSession;

public interface ClusterManager extends Managed {

	void sessionOpened(IoSession session);

	void sessionClosed(IoSession session);

	void exceptionCaught(IoSession session, Throwable cause);

	void messageReceived(IoSession session, Object message);

	@Override
	void start() throws Exception;

	@Override
	void stop() throws Exception;
}
