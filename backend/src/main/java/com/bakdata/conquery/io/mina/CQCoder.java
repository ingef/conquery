package com.bakdata.conquery.io.mina;

import com.bakdata.conquery.io.mina.ChunkReader.ChunkedMessage;

public interface CQCoder<OUT> {

	public OUT decode(ChunkedMessage message) throws Exception;

	public Chunkable encode(OUT message) throws Exception;
}
