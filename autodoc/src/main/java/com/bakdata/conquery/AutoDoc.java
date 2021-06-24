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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutoDoc {
	
	public static void main(String[] args) throws IOException {
		new AutoDoc().start(new File(args.length==0?"./docs/":args[0]));
	}

	private ScanResult scan;
	
	public AutoDoc() {
		scan = new ClassGraph()
			.enableAllInfo()
			//reject some packages that contain large libraries
			.rejectPackages(
				"groovy",
				"org.codehaus.groovy",
				"org.apache",
				"org.eclipse",
				"com.google"
			)
			.scan();
	}
	
	public void start(File docs) throws IOException {
		docs.mkdirs();
		for(Group group : GROUPS) {
			File target = new File(docs, group.getName()+".md");
			try (var out = new SimpleWriter(
				Out.file(target).withUTF8().asWriter()
			)) {
				new GroupHandler(scan, group, out, docs.getCanonicalFile().getParentFile()).handle();
			}
			log.info("Written file {}", target.getCanonicalPath());
		}
	}
}
