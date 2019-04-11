package com.bakdata.conquery.commands;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.util.DebugMode;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
public class PreprocessorCommand extends ConfiguredCommand<ConqueryConfig> {

	public PreprocessorCommand() {
		super("preprocess", "Preprocesses all the files in the given input directories. This has to be done only if the model or the files changed.");
	}

	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig config) throws Exception {
		if(config.getDebugMode() != null) {
			DebugMode.setActive(config.getDebugMode());
		}
		config.initializeDatePatterns();
		
		ExecutorService pool = Executors.newFixedThreadPool(config.getPreprocessor().getThreads());
		for(PreprocessingDirectories directories:config.getPreprocessor().getDirectories()) {
			Preprocessor preprocessor = new Preprocessor();
			Collection<ImportDescriptor> descriptors = preprocessor.findInitialDescriptors(directories, bootstrap.getValidatorFactory().getValidator());
			for(ImportDescriptor d:descriptors) {
				pool.submit(() -> {
					ConqueryMDC.setLocation(d.toString());
					try {
						preprocessor.preprocess(d, config);
					}
					catch(Exception e) {
						log.error("Failed to preprocess "+LogUtil.printPath(d.getInputFile().getDescriptionFile()), e);
					}
				});
			}
		}
		

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);
	}
}
