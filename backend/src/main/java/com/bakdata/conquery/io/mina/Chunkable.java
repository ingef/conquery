package com.bakdata.conquery.io.mina;

import java.io.OutputStream;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Getter @Slf4j
public class Chunkable {

	private final UUID id;
	private final ObjectWriter writer;
	private final Object message;
	
	public void writeMessage(OutputStream out) {
		try(OutputStream os = out) {
			writer.writeValue(os, message);
		} catch(Exception e) {
			log.error("Failed to write message "+id+": "+message, e);
		}
	}
}
