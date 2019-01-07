package com.bakdata.conquery.models.config;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Ints;

import io.dropwizard.util.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MinaConfig implements IoSessionConfig {

	/** The minimum size of the buffer used to read incoming data */
	private int minReadBufferSize = 64;
	
	/** The default size of the buffer used to read incoming data */
	private int readBufferSize = 8192;

	/** The maximum size of the buffer used to read incoming data */
	private int maxReadBufferSize = Ints.checkedCast(Size.megabytes(500).toBytes());

	/** The delay before we notify a session that it has been idle on read. Default to infinite */
	private int readerIdleTime;

	/** The delay before we notify a session that it has been idle on write. Default to infinite */
	private int writerIdleTime;

	/**
	 * The delay before we notify a session that it has been idle on read and write.
	 * Default to infinite
	 **/
	private int bothIdleTime;

	/** The delay to wait for a write operation to complete before bailing out */
	private int writeTimeout = 0;

	/** A flag set to true when weallow the application to do a session.read(). Default to false */
	private boolean useReadOperation;

	private int throughputCalculationInterval = 3;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAll(IoSessionConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("config");
		}

		setReadBufferSize(config.getReadBufferSize());
		setMinReadBufferSize(config.getMinReadBufferSize());
		setMaxReadBufferSize(config.getMaxReadBufferSize());
		setIdleTime(IdleStatus.BOTH_IDLE, config.getIdleTime(IdleStatus.BOTH_IDLE));
		setIdleTime(IdleStatus.READER_IDLE, config.getIdleTime(IdleStatus.READER_IDLE));
		setIdleTime(IdleStatus.WRITER_IDLE, config.getIdleTime(IdleStatus.WRITER_IDLE));
		setWriteTimeout(config.getWriteTimeout());
		setUseReadOperation(config.isUseReadOperation());
		setThroughputCalculationInterval(config.getThroughputCalculationInterval());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getIdleTime(IdleStatus status) {
		if (status == IdleStatus.BOTH_IDLE) {
			return bothIdleTime;
		}

		if (status == IdleStatus.READER_IDLE) {
			return readerIdleTime;
		}

		if (status == IdleStatus.WRITER_IDLE) {
			return writerIdleTime;
		}

		throw new IllegalArgumentException("Unknown idle status: " + status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getIdleTimeInMillis(IdleStatus status) {
		return getIdleTime(status) * 1000L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIdleTime(IdleStatus status, int idleTime) {
		if (idleTime < 0) {
			throw new IllegalArgumentException("Illegal idle time: " + idleTime);
		}

		if (status == IdleStatus.BOTH_IDLE) {
			bothIdleTime = idleTime;
		} else if (status == IdleStatus.READER_IDLE) {
			readerIdleTime = idleTime;
		} else if (status == IdleStatus.WRITER_IDLE) {
			writerIdleTime = idleTime;
		} else {
			throw new IllegalArgumentException("Unknown idle status: " + status);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override @JsonIgnore
	public final long getBothIdleTimeInMillis() {
		return getIdleTimeInMillis(IdleStatus.BOTH_IDLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override @JsonIgnore
	public final long getReaderIdleTimeInMillis() {
		return getIdleTimeInMillis(IdleStatus.READER_IDLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override @JsonIgnore
	public final long getWriterIdleTimeInMillis() {
		return getIdleTimeInMillis(IdleStatus.WRITER_IDLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override @JsonIgnore
	public long getWriteTimeoutInMillis() {
		return writeTimeout * 1000L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override @JsonIgnore
	public long getThroughputCalculationIntervalInMillis() {
		return throughputCalculationInterval * 1000L;
	}
}