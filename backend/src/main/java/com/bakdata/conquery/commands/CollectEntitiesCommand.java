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
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.jobs.SimpleJob.Executable;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.types.specific.StringType;
import com.bakdata.conquery.util.DebugMode;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.fasterxml.jackson.core.JsonParser;
import com.github.powerlibraries.io.Out;
import com.google.common.collect.Sets;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
public class CollectEntitiesCommand extends ConfiguredCommand<ConqueryConfig> {

	private ConcurrentMap<File, Set<String>> entities = new ConcurrentHashMap<>();
	
	public CollectEntitiesCommand() {
		super("collectEntities", "Collect all entities from the given preprocessing directories.");
	}

	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig config) throws Exception {
		if(config.getDebugMode() != null) {
			DebugMode.setActive(config.getDebugMode());
		}
		config.initializeDatePatterns();
		
		ExecutorService pool = Executors.newFixedThreadPool(config.getPreprocessor().getThreads());
		
		Collection<EntityExtractor> jobs = findPreprocessedJobs(config);
		
		ProgressBar totalProgress = new ProgressBar(jobs.size(), System.out);

		for(EntityExtractor job:jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(LogUtil.printPath(job.getFile()));
				try {
					job.execute();
					totalProgress.addCurrentValue(1L);
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
			log.info("Writing {}", e.getKey());
			Out
				.file(new File(e.getKey(), "entities.csv"))
				.withUTF8()
				.writeLines(e.getValue().stream().sorted().distinct().iterator());
		}
		
	}
	
	public List<EntityExtractor> findPreprocessedJobs(ConqueryConfig config) throws IOException, JSONException {
		List<EntityExtractor> l = new ArrayList<>();
		for(PreprocessingDirectories directories:config.getPreprocessor().getDirectories()) {
			File in = directories.getPreprocessedOutput().getAbsoluteFile();
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
	
	@RequiredArgsConstructor 
	public class EntityExtractor implements Executable {
		@Getter
		private final File file;
		
		@Override
		public void execute() throws Exception {
			try (HCFile hcFile = new HCFile(file, false)) {
				PPHeader header;
				try (JsonParser in = Jackson.BINARY_MAPPER.getFactory().createParser(hcFile.readHeader())) {
					header = Jackson.BINARY_MAPPER.readerFor(PPHeader.class).readValue(in);

					log.info("Reading {}", header.getName());

					log.debug("\tparsing dictionaries");
					header.getPrimaryColumn().getType().readHeader(in);
					Dictionary dict = ((StringType) header.getPrimaryColumn().getType()).getDictionary();
					Set<String> list = entities.computeIfAbsent(file.getParentFile(), f->Sets.newConcurrentHashSet());
					dict.forEach(list::add);
				}
			}
		}
		
	}
}
