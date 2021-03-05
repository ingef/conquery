package com.bakdata.conquery.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PreprocessingJob;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.jakewharton.byteunits.BinaryByteUnit;
import io.dropwizard.setup.Environment;
import lombok.SneakyThrows;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.BooleanArgumentType;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.impl.type.StringArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
@FieldNameConstants
public class PreprocessorCommand extends ConqueryCommand {

	private final List<String> failed = Collections.synchronizedList(new ArrayList<>());
	private final List<String> success = Collections.synchronizedList(new ArrayList<>());
	private final List<String> missing = Collections.synchronizedList(new ArrayList<>());
	private ExecutorService pool;
	private boolean isFailFast = false;
	private boolean isStrict = true;

	public PreprocessorCommand() {
		this(null);
	}

	public PreprocessorCommand(ExecutorService pool) {
		super("preprocess", "Preprocesses all the files in the given input directories. This has to be done only if the model or the files changed.");
		this.pool = pool;
	}

	@SneakyThrows
	public static boolean requiresProcessing(PreprocessingJob preprocessingJob) {
		ConqueryMDC.setLocation(preprocessingJob.toString());
		if (preprocessingJob.getPreprocessedFile().exists()) {

			log.info("EXISTS ALREADY");

			int currentHash = preprocessingJob.getDescriptor()
											  .calculateValidityHash(preprocessingJob.getCsvDirectory(), preprocessingJob.getTag());


			try (HCFile outFile = new HCFile(preprocessingJob.getPreprocessedFile(), false);
				 InputStream is = outFile.readHeader()) {

				PreprocessedHeader header = Jackson.BINARY_MAPPER.readValue(is, PreprocessedHeader.class);
				log.debug("Old hash = `{}`, New hash = `{}`", header.getValidityHash(), currentHash);
				if (header.getValidityHash() == currentHash) {

					log.info("\tHASH STILL VALID");
					return false;
				}
				log.info("\tHASH OUTDATED");
			}
			catch (Exception e) {
				log.error("\tHEADER READING FAILED", e);
				return false;
			}
		}
		else {
			log.info("DOES NOT EXIST");
		}

		return true;
	}

	/**
	 * Configure CLI based configuration for preprocessor.
	 */
	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		final ArgumentGroup group = subparser.addArgumentGroup("Preprocessing CLI Config")
											 .description("Optional arguments to do a single import step by hand. Overrides json configuration.");

		group.addArgument("--in")
			 .required(true)
			 .type(new FileArgumentType().verifyIsDirectory().verifyCanRead())
			 .help("Directory containing the input files (in csv or gzipped csv format).");

		group.addArgument("--out")
			 .required(false)
			 .type(new FileArgumentType().verifyIsDirectory().verifyCanCreate().verifyCanWrite())
			 .help("Directory to write the output cqpp files to.");

		group.addArgument("--desc")
			 .required(true)
			 .type(new FileArgumentType().verifyCanRead())
			 .nargs("*")
			 .help("Directory containing the import description files (*.import.json).");

		group.addArgument("--tag")
			 .required(false)
			 .type(new StringArgumentType())
			 .nargs("*")
			 .help("Optional tags for input and output files: Will change input files from `filename.csv.gz` to `filename.$tag.csv.gz` and output files from `filename.cqpp` to `filename.$tag.cqpp`. Tag will also override the import-id to tag.");

		group.addArgument("--fast-fail")
			 .action(Arguments.storeTrue())
			 .help("Stop preprocessing and exit with failure if an error occures that prevents the generation of a cqpp.");

		group.addArgument("--strict")
			 .type(new BooleanArgumentType())
			 .setDefault(true)
			 .help("Fail immediately when files are missing.");

	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		if (pool == null) {
			pool = Executors.newFixedThreadPool(config.getPreprocessor().getNThreads());
		}

		// Tag if present is appended to input-file csvs, output-file cqpp and used as id of cqpps

		isFailFast = namespace.get("fast-fail") != null && namespace.getBoolean("fast-fail");
		isStrict = namespace.getBoolean("strict");

		final List<String> tags = namespace.getList("tag");

		final File inDir = namespace.get("in");
		final File outDir = namespace.get("out");
		final List<File> descriptionFiles = namespace.<File>getList("desc");


		log.info("Preprocessing from command line config.");

		final Collection<PreprocessingJob> jobs = new ArrayList<>();

		if (tags == null || tags.isEmpty()) {
			for (File desc : descriptionFiles) {
				final List<PreprocessingJob> descriptions =
						findPreprocessingDescriptions(desc, inDir, outDir, Optional.empty(), environment.getValidator());
				jobs.addAll(descriptions);
			}
		}
		else {
			for (String tag : tags) {
				for (File desc : descriptionFiles) {
					final List<PreprocessingJob> jobDescriptions =
							findPreprocessingDescriptions(desc, inDir, outDir, Optional.of(tag), environment.getValidator());

					jobs.addAll(jobDescriptions);
				}
			}
		}

		List<PreprocessingJob> missing = new ArrayList<>();

		for (PreprocessingJob job : jobs) {
			for (TableInputDescriptor input : job.getDescriptor().getInputs()) {
				final File sourceFile = Preprocessor.resolveSourceFile(input.getSourceFile(), job.getCsvDirectory(), job.getTag());
				if (!sourceFile.exists()) {
					log.error("Did not find file `{}` for Preprocessing[{}].", sourceFile, job);
					missing.add(job);
				}
			}
		}

		// This will halt preprocessing immediately.
		if (isStrict && !missing.isEmpty()) {
			log.error("FAILED Preprocessing, files are missing.");
			doFail();
		}

		jobs.removeIf(Predicate.not(PreprocessorCommand::requiresProcessing));

		final long totalSize = jobs.stream()
								   .mapToLong(PreprocessingJob::estimateTotalCsvSizeBytes)
								   .sum();

		log.info("Required to preprocess {} in total", BinaryByteUnit.format(totalSize));

		ProgressBar totalProgress = new ProgressBar(totalSize, System.out);

		for (PreprocessingJob job : jobs) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(job.toString());
				try {
					Preprocessor.preprocess(job, totalProgress, config);
					success.add(job.toString());
				}
				catch (FileNotFoundException e) {
					log.warn("Did not find file `{}` for preprocessing.", e.getMessage());
					addMissing(job);
				}
				catch (Exception e) {
					log.error("Failed to preprocess " + LogUtil.printPath(job.getDescriptionFile()), e);
					addFailed(job);
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);

		ConqueryMDC.clearLocation();


		if (!success.isEmpty()) {
			log.info("Successfully Preprocess {} Jobs:", success.size());
			success.forEach(desc -> log.info("\tSucceeded Preprocessing for {}", desc));
		}

		if (!missing.isEmpty()) {
			log.warn("Did not find {} Files", missing.size());
			missing.forEach(desc -> log.warn("\tDid not find file for {}", desc));
		}

		if (isFailed()) {
			log.error("Failed {} Preprocessing Jobs:", failed.size());
			failed.forEach(desc -> log.error("\tFailed Preprocessing for {}", desc));
			doFail();
		}
	}

	private void addMissing(PreprocessingJob job) {
		log.error("Files are missing for {}", job);
		if (isStrict) {
			addFailed(job);
		}
	}

	private void addFailed(PreprocessingJob job) {
		if (isFailFast) {
			doFail();
		}
		failed.add(job.toString());
	}

	public List<PreprocessingJob> findPreprocessingDescriptions(File descriptionFiles, File inDir, File outputDir, Optional<String> tag, Validator validator)
			throws IOException {
		List<PreprocessingJob> out = new ArrayList<>();

		final File[] files = descriptionFiles.isFile()
							 ? new File[]{descriptionFiles}
							 : descriptionFiles.listFiles(((dir, name) -> name.endsWith(ConqueryConstants.EXTENSION_DESCRIPTION)));

		if (files == null) {
			return Collections.emptyList();
		}

		for (File descriptionFile : files) {
			tryExtractDescriptor(validator, tag, descriptionFile, outputDir, inDir)
					.ifPresent(out::add);
		}
		return out;
	}

	private boolean isFailed() {
		return !failed.isEmpty();
	}

	private Optional<PreprocessingJob> tryExtractDescriptor(Validator validator, Optional<String> tag, File descriptionFile, File outputDir, File csvDir)
			throws IOException {
		try {
			final TableImportDescriptor
					descriptor =
					TableImportDescriptor.read(descriptionFile);

			validator.validate(validator);

			final PreprocessingJob preprocessingJob = new PreprocessingJob(csvDir.toPath(), descriptionFile, outputDir.toPath(), tag, descriptor);


			// Override name to tag if present
			tag.ifPresent(descriptor::setName);

			return Optional.of(preprocessingJob);
		}
		catch (Exception e) {
			log.error("Failed to process " + LogUtil.printPath(descriptionFile), e);
			if (isFailFast) {
				doFail();
			}
			failed.add(descriptionFile.toString());
		}
		return Optional.empty();
	}

	private void doFail() {
		System.exit(1);
	}

}
