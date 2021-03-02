package com.bakdata.conquery.io.mina;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import lombok.Data;

@Data
public class ChunkedMessage {

	private final ByteArrayFeeder feeder;
	private final JsonParser parser;

}