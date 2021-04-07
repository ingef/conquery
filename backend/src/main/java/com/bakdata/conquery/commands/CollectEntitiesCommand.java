package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.jobs.SimpleJob.Executable;
import com.bakdata.conquery.models.preproc.Preprocessed;
import com.bakdata.conquery.models.preproc.PreprocessedDictionaries;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.github.powerlibraries.io.Out;
import com.google.common.collect.Sets;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
public class CollectEntitiesCommand extends Command {

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

		subparser
				.addArgument("--file")
				.nargs("+")
				.help("List of CQPP to process");

	}


	@Override
	public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
		verbose = Boolean.TRUE.equals(namespace.getBoolean("-verbose"));
		Collection<EntityExtractor> jobs = findPreprocessedJobs(namespace.<File>getList("file"));

		ExecutorService pool = Executors.newCachedThreadPool();

		for (EntityExtractor job : jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(LogUtil.printPath(job.getFile()));
				try {
					job.execute();
					log.info("Merged {}", LogUtil.printPath(job.getFile()));
				}
				catch (Exception e) {
					log.error("Failed to preprocess " + LogUtil.printPath(job.getFile()), e);
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);

		log.info("finished collecting ids, writing...");
		for (Entry<File, Set<String>> e : entities.entrySet()) {
			log.info("{} entities into {}", e.getValue().size(), e.getKey());
			Out
					.file(e.getKey())
					.withUTF8()
					.writeLines(e.getValue().stream().sorted().distinct().iterator());
		}

	}

	public List<EntityExtractor> findPreprocessedJobs(List<File> files) throws IOException, JSONException {
		List<EntityExtractor> l = new ArrayList<>();
		for (File preprocessedFile : files) {
			if (!preprocessedFile.getName().endsWith(ConqueryConstants.EXTENSION_PREPROCESSED)) {
				continue;
			}

			l.add(new EntityExtractor(preprocessedFile));
		}
		return l;
	}

	@RequiredArgsConstructor
	@Getter
	public class EntityExtractor implements Executable {

		private final File file;

		@Override
		public void execute() throws Exception {
			try (final JsonParser parser = Preprocessed.createParser(file, Map.of(Dataset.PLACEHOLDER.getId(), Dataset.PLACEHOLDER))) {

				final PreprocessedHeader header = parser.readValueAs(PreprocessedHeader.class);
				log.info("Reading {}", header.getName());

				final PreprocessedDictionaries dictionaries = parser.readValueAs(PreprocessedDictionaries.class);

				final EncodedDictionary primaryDictionary = new EncodedDictionary(dictionaries.getPrimaryDictionary(), StringTypeEncoded.Encoding.UTF8);

				add(primaryDictionary, new File(file.getParentFile(), "all_entities.csv"));
				if (verbose) {
					add(primaryDictionary, new File(file.getParentFile(), file.getName() + ".entities.csv"));
				}
			}
		}

		private void add(EncodedDictionary primDict, File file) {
			Set<String> list = entities.computeIfAbsent(file, f -> Sets.newConcurrentHashSet());
			for (int id = 0; id < primDict.getSize(); id++) {
				list.add(primDict.getElement(id));
			}
		}
	}
}
