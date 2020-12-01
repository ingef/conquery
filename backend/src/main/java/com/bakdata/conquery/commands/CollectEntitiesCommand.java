package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.jobs.SimpleJob.Executable;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.github.powerlibraries.io.Out;
import com.google.common.collect.Sets;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
public class CollectEntitiesCommand extends ConqueryCommand {

	private ConcurrentMap<File, Set<String>> entities = new ConcurrentHashMap<>();
	private boolean verbose = false;
	
	public CollectEntitiesCommand() {
		super("collectEntities", "Collect all entities from the given preprocessing directories.");
	}
	
	@Override
	public void configure(Subparser subparser) {
		subparser
			.addArgument("-verbose")
			.help("creates not only a file for all entities but for eqach cqpp")
			.action(Arguments.storeTrue());
		super.configure(subparser);
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		verbose = Boolean.TRUE.equals(namespace.getBoolean("-verbose"));

		ExecutorService pool = Executors.newFixedThreadPool(config.getPreprocessor().getNThreads());
		
		Collection<EntityExtractor> jobs = findPreprocessedJobs(config);
		

		for(EntityExtractor job:jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(LogUtil.printPath(job.getFile()));
				try {
					job.execute();
					log.info("Merged {}", LogUtil.printPath(job.getFile()));
				}
				catch(Exception e) {
					log.error("Failed to preprocess "+LogUtil.printPath(job.getFile()), e);
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);
		
		log.info("finished collecting ids, writing...");
		for(Entry<File, Set<String>> e : entities.entrySet()) {
			log.info("{} entities into {}", e.getValue().size(), e.getKey());
			Out
				.file(e.getKey())
				.withUTF8()
				.writeLines(e.getValue().stream().sorted().distinct().iterator());
		}
		
	}
	
	public List<EntityExtractor> findPreprocessedJobs(ConqueryConfig config) throws IOException, JSONException {
		List<EntityExtractor> l = new ArrayList<>();
		for(PreprocessingDirectories directories:config.getPreprocessor().getDirectories()) {
			File in = directories.getPreprocessedOutputDir().getAbsoluteFile();
			for(File preprocessedFile:in.listFiles()) {
				if(preprocessedFile.getName().endsWith(ConqueryConstants.EXTENSION_PREPROCESSED)) {
					try {
						l.add(new EntityExtractor(preprocessedFile));
					} catch(Exception e) {
						log.error("Failed to process "+LogUtil.printPath(preprocessedFile), e);
					}
				}
			}
		}
		return l;
	}
	
	@RequiredArgsConstructor @Getter 
	public class EntityExtractor implements Executable {
		
		private final File file;
		
		@Override
		public void execute() throws Exception {
//			try (HCFile hcFile = new HCFile(file, false)) {
//				PreprocessedHeader header;
//				try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(hcFile.readHeader())) {
//					header = Jackson.BINARY_MAPPER.readerFor(PreprocessedHeader.class).readValue(in);
//
//					log.info("Reading {}", header.getName());
//
//					log.debug("\tparsing dictionaries");
//					header.getPrimaryColumn().getType().readHeader(in);
//					StringType primType = (StringType) header.getPrimaryColumn().getType();
//
//					add(primType, new File(file.getParentFile(), "all_entities.csv"));
//					if(verbose) {
//						add(primType, new File(file.getParentFile(), file.getName()+".entities.csv"));
//					}
//				}
//			}
		}

		private void add(StringType primType, File file) {
			Set<String> list = entities.computeIfAbsent(file, f->Sets.newConcurrentHashSet());
			primType.forEach(list::add);
		}
		
	}
}
