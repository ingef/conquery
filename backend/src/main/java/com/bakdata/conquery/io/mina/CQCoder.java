package com.bakdata.conquery.io.mina;

public interface CQCoder<OUT> {

	OUT decode(ChunkedMessage message) throws Exception;

	Chunkable encode(OUT message) throws Exception;
}
