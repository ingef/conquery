package com.bakdata.conquery.mode.cluster;

import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.session.IoSession;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ClusterMetrics implements MetricSet {

	private final IoSession session;

	@Override
	public Map<String, Metric> getMetrics() {
		return Map.of(
				nameReadBytes(session), (Gauge<Long>) session::getReadBytes,
				nameWrittenBytes(session), (Gauge<Long>) session::getWrittenBytes,
				nameReadMessages(session), (Gauge<Long>) session::getReadMessages,
				nameWrittenMessages(session), (Gauge<Long>) session::getWrittenMessages,
				nameScheduledWriteMessages(session), (Gauge<Integer>) session::getScheduledWriteMessages,
				nameScheduledWriteBytes(session), (Gauge<Long>) session::getScheduledWriteBytes,
				nameLastIoTime(session), (Gauge<Long>) session::getLastIoTime,
				nameLastReadTime(session), (Gauge<Long>) session::getLastReadTime,
				nameLastWriteTime(session), (Gauge<Long>) session::getLastWriteTime
		);
	}


	@NotNull
	private static String nameLastWriteTime(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "lastWriteTime");
	}

	@NotNull
	private static String nameLastReadTime(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "lastReadTime");
	}

	@NotNull
	private static String nameLastIoTime(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "lastIoTime");
	}

	@NotNull
	private static String nameScheduledWriteBytes(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "scheduledWriteBytes");
	}

	@NotNull
	private static String nameScheduledWriteMessages(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "scheduledWriteMessages");
	}

	@NotNull
	private static String nameWrittenMessages(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "writtenMessages");
	}

	@NotNull
	private static String nameReadMessages(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "readMessages");
	}

	@NotNull
	private static String nameWrittenBytes(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "writtenBytes");
	}

	@NotNull
	private static String nameReadBytes(IoSession session) {
		return MetricRegistry.name("cluster", "session", session.getRemoteAddress().toString(), "readBytes");
	}

}
