package com.bakdata.conquery;

import static com.bakdata.conquery.Constants.GROUPS;

import java.io.File;
import java.io.IOException;

import com.bakdata.conquery.handler.GroupHandler;
import com.bakdata.conquery.handler.SimpleWriter;
import com.bakdata.conquery.model.Group;
import com.github.powerlibraries.io.Out;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;


public class AutoDoc {
	
	public static void main(String[] args) throws IOException {
		new AutoDoc().start();
	}

	private ScanResult scan;
	
	public AutoDoc() {
		scan = new ClassGraph()
			.enableAllInfo()
			//blacklist some packages that contain large libraries
			.blacklistPackages(
				"groovy",
				"org.codehaus.groovy",
				"org.apache",
				"org.eclipse",
				"com.google"
			)
			.scan();
	}
	
	public void start() throws IOException {
		File docs = new File("../docs/");
		docs.mkdirs();
		for(Group group : GROUPS) {
			try (var out = new SimpleWriter(
				Out.file(docs, group.getName()+".md").withUTF8().asWriter()
			)) {
				new GroupHandler(scan, group, out).handle();
			}
		}
	}
}
