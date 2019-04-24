package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.validation.Validator;

import org.apache.commons.io.FileUtils;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.util.DebugMode;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.jakewharton.byteunits.BinaryByteUnit;

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
		
		Collection<Preprocessor> jobs = findPreprocessingJobs(config, bootstrap.getValidatorFactory().getValidator());
		jobs.removeIf(Predicate.not(Preprocessor::requiresProcessing));
		
		long totalSize = jobs.stream().mapToLong(Preprocessor::getTotalCsvSize).sum();
		
		log.info("Required to preprocess {} in total", BinaryByteUnit.format(totalSize));
		
		ProgressBar totalProgress = new ProgressBar(totalSize, System.out);

		for(Preprocessor job:jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(job.getDescriptor().toString());
				try {
					job.preprocess(totalProgress);
				}
				catch(Exception e) {
					log.error("Failed to preprocess "+LogUtil.printPath(job.getDescriptor().getInputFile().getDescriptionFile()), e);
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);
	}
	
	public static List<Preprocessor> findPreprocessingJobs(ConqueryConfig config, Validator validator) throws IOException, JSONException {
		List<Preprocessor> l = new ArrayList<>();
		for(PreprocessingDirectories directories:config.getPreprocessor().getDirectories()) {
			File in = directories.getDescriptions().getAbsoluteFile();
			for(File descriptionFile:in.listFiles()) {
				if(descriptionFile.getName().endsWith(ConqueryConstants.EXTENSION_DESCRIPTION)) {
					InputFile file = InputFile.fromDescriptionFile(descriptionFile, directories);
					try {
						ImportDescriptor descr = file.readDescriptor(validator);
						descr.setInputFile(file);
						l.add(new Preprocessor(config, descr));
					} catch(Exception e) {
						log.error("Failed to process "+LogUtil.printPath(descriptionFile), e);
					}
				}
			}
		}
		return l;
	}
}
