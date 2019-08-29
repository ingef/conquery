package com.bakdata.conquery.handler;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Joiner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SimpleWriter implements Closeable {
	private final Writer writer;

	@Override
	public void close() throws IOException {
		writer.close();
	}

	public void heading(String str) throws IOException {
		line("\n# "+str);
	}
	
	public void subHeading(String str) throws IOException {
		line("\n---\n\n## "+str);
	}
	
	public void subSubHeading(String str) throws IOException {
		line("\n### "+str);
	}
	
	public void tableHeader(String... header) throws IOException {
		line("\n| "+Joiner.on(" | ").join(header)+" |");
		line("| "+Stream.generate(()->"---").limit(header.length).collect(Collectors.joining(" | "))+" |");
	}
	public void table(String... values) throws IOException {
		line("| "+Joiner.on(" | ").join(values)+" | ");
	}
	
	public void paragraph(String str) throws IOException {
		line(str+"\n");
	}

	public void line(String str) throws IOException {
		writer.write(str+"\n");
		log.trace(str);
	}
}
