package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.jakewharton.byteunits.BinaryByteUnit;

import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
public class PreprocessorCommand extends ConqueryCommand {

	private ExecutorService pool;

	public PreprocessorCommand() {
		this(null);
	}

	public PreprocessorCommand(ExecutorService pool) {
		super("preprocess", "Preprocesses all the files in the given input directories. This has to be done only if the model or the files changed.");
		this.pool = pool;
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		final ArgumentGroup group = subparser.addArgumentGroup("Preprocessing CLI Config");

		group.addArgument("--in").required(false)
			 .type(new FileArgumentType().verifyIsDirectory().verifyCanRead());

		group.addArgument("--out").required(false)
			 .type(new FileArgumentType().verifyIsDirectory().verifyCanCreate().verifyCanWrite());

		group.addArgument("--desc").required(false)
			 .type(new FileArgumentType().verifyIsDirectory().verifyCanRead());
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		if (pool == null) {
			pool = Executors.newFixedThreadPool(config.getPreprocessor().getThreads());
		}

		Collection<Preprocessor> jobs = null;

		if (namespace.get("in") != null && namespace.get("desc") != null && namespace.get("out") != null) {
			jobs = findPreprocessingJobs(config, environment.getValidator(), new PreprocessingDirectories[]{
					new PreprocessingDirectories(namespace.get("in"), namespace.get("desc"), namespace.get("out"))
			});
		}
		else
			jobs = findPreprocessingJobs(config, environment.getValidator(), config.getPreprocessor().getDirectories());


		jobs.removeIf(Predicate.not(Preprocessor::requiresProcessing));

		long totalSize = jobs.stream().mapToLong(Preprocessor::getTotalCsvSize).sum();

		log.info("Required to preprocess {} in total", BinaryByteUnit.format(totalSize));

		ProgressBar totalProgress = new ProgressBar(totalSize, System.out);

		for (Preprocessor job : jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(job.getDescriptor().toString());
				try {
					job.preprocess(totalProgress);
				} catch (Exception e) {
					log.error("Failed to preprocess " + LogUtil.printPath(job.getDescriptor().getInputFile().getDescriptionFile()), e);
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);
	}

	public static List<Preprocessor> findPreprocessingJobs(ConqueryConfig config, Validator validator, PreprocessingDirectories[] directories1) throws IOException, JSONException {
		List<Preprocessor> l = new ArrayList<>();
		for (PreprocessingDirectories directories : directories1) {
			File in = directories.getDescriptions().getAbsoluteFile();
			for (File descriptionFile : in.listFiles()) {
				if (descriptionFile.getName().endsWith(ConqueryConstants.EXTENSION_DESCRIPTION)) {
					InputFile file = InputFile.fromDescriptionFile(descriptionFile, directories);
					try {
						ImportDescriptor descr = file.readDescriptor(validator);
						descr.setInputFile(file);
						l.add(new Preprocessor(config, descr));
					} catch (Exception e) {
						log.error("Failed to process " + LogUtil.printPath(descriptionFile), e);
					}
				}
			}
		}
		return l;
	}
}
