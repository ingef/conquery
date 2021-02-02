package com.bakdata.conquery.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.jakewharton.byteunits.BinaryByteUnit;
import io.dropwizard.setup.Environment;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.impl.type.StringArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

@Slf4j
@FieldNameConstants
public class PreprocessorCommand extends ConqueryCommand {

	private ExecutorService pool;
	private final List<String> failed = Collections.synchronizedList(new ArrayList<>());
	private final List<String> success = Collections.synchronizedList(new ArrayList<>());
	private final List<String> missing = Collections.synchronizedList(new ArrayList<>());
	private final List<String> skipped = Collections.synchronizedList(new ArrayList<>());
	private boolean isFailFast = false;

	public PreprocessorCommand() {
		this(null);
	}

	public PreprocessorCommand(ExecutorService pool) {
		super("preprocess", "Preprocesses all the files in the given input directories. This has to be done only if the model or the files changed.");
		this.pool = pool;
	}

	/**
	 * Configure CLI based configuration for preprocessor.
	 */
	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		final ArgumentGroup	group =	subparser.addArgumentGroup("Preprocessing CLI Config")
												.description("Optional arguments to do a single import step by hand. Overrides json configuration.");

		group.addArgument("--in")
				.required(false)
				.type(new FileArgumentType().verifyIsDirectory().verifyCanRead())
				.help("Directory containing the input files (in csv or gzipped csv format).");

		group.addArgument("--out")
				.required(false)
				.type(new FileArgumentType().verifyIsDirectory().verifyCanCreate().verifyCanWrite())
				.help("Directory to write the output cqpp files to.");

		group.addArgument("--desc")
				.required(false)
				.type(new FileArgumentType().verifyIsDirectory().verifyCanRead())
				.help("Directory containing the import description files (*.import.json).");

		group.addArgument("--tag")
				.required(false)
				.type(new StringArgumentType())
				.nargs("*")
				.help("Optional tags for input and output files: Will change input files from `filename.csv.gz` to `filename.$tag.csv.gz` and output files from `filename.cqpp` to `filename.$tag.cqpp`. Tag will also override the import-id to tag.");

		group.addArgument("--fast-fail")
	        	.action(Arguments.storeTrue())
	        	.help("Stop preprocessing and exit with failure if an error occures that prevents the generation of a cqpp.");
		
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		if (pool == null) {
			pool = Executors.newFixedThreadPool(config.getPreprocessor().getNThreads());
		}

		final Collection<TableImportDescriptor> descriptors = new ArrayList<>();

		// Tag if present is appended to input-file csvs, output-file cqpp and used as id of cqpps

		isFailFast = namespace.get("fast-fail") == null ? false : namespace.get("fast-fail");

		final List<String> tags = namespace.getList("tag") != null ? namespace.getList("tag") : Collections.singletonList(null);

		if (namespace.get("in") != null && namespace.get("desc") != null && namespace.get("out") != null) {
			log.info("Preprocessing from command line config.");

			for (String tag : tags) {
				descriptors.addAll(findPreprocessingDescriptions(environment.getValidator(), new PreprocessingDirectories[]{
						new PreprocessingDirectories(namespace.get("in"), namespace.get("desc"), namespace.get("out"))
				}, tag));
			}

		}
		else {
			for (String tag : tags) {
				log.info("Preprocessing from config.json");
				descriptors.addAll(findPreprocessingDescriptions(environment.getValidator(), config.getPreprocessor().getDirectories(), tag));
			}
		}


		descriptors.removeIf(Predicate.not(Preprocessor::requiresProcessing));

		long totalSize = descriptors.stream().mapToLong(Preprocessor::getTotalCsvSize).sum();

		log.info("Required to preprocess {} in total", BinaryByteUnit.format(totalSize));

		ProgressBar totalProgress = new ProgressBar(totalSize, System.out);

		for (TableImportDescriptor descriptor : descriptors) {
			pool.submit(() -> {
				ConqueryMDC.setLocation(descriptor.toString());
				try {
					Preprocessor.preprocess(descriptor, totalProgress, ConqueryConfig.getInstance());
					success.add(descriptor.toString());
				}
				catch (FileNotFoundException e) {
					log.warn("Did not find file `{}` for preprocessing.", e.getMessage());
					missing.add(descriptor.toString());
				}
				catch (Exception e) {
					log.error("Failed to preprocess " + LogUtil.printPath(descriptor.getInputFile().getDescriptionFile()), e);
					if (isFailFast) {
						System.exit(1);
					}
					failed.add(descriptor.toString());
				}
			});
		}

		pool.shutdown();
		pool.awaitTermination(24, TimeUnit.HOURS);

		ConqueryMDC.clearLocation();
		
		if(!skipped.isEmpty()) {			
			log.info("Skipped {} Imports because not all source files existed for a tag", skipped.size());
			skipped.forEach(desc -> log.trace("\tSkipped Preprocessing for {}", desc));
		}

		if(!success.isEmpty()){
			log.info("Successfully Preprocess {} Jobs:", success.size());
			success.forEach(desc -> log.info("\tSucceeded Preprocessing for {}", desc));
		}
		
		if(!missing.isEmpty()){
			log.warn("Did not find {} Files",missing.size());
			missing.forEach(desc -> log.warn("\tDid not find file for {}", desc));
		}

		if (!failed.isEmpty()) {
			log.error("Failed {} Preprocessing Jobs:", failed.size());
			failed.forEach(desc -> log.error("\tFailed Preprocessing for {}", desc));
			System.exit(1); 
		}
	}

	public List<TableImportDescriptor> findPreprocessingDescriptions(Validator validator, PreprocessingDirectories[] directories, String tag)
			throws IOException {
		List<TableImportDescriptor> out = new ArrayList<>();
		for (PreprocessingDirectories description : directories) {

			File inDir = description.getDescriptionsDir().getAbsoluteFile();
			final File[] files = inDir.isFile() ?
								 new File[]{inDir} :
								 inDir.listFiles(((dir, name) -> name.endsWith(ConqueryConstants.EXTENSION_DESCRIPTION)));

			for (File descriptionFile : files) {

				tryExtractDescriptor(validator, tag, description, descriptionFile).ifPresent(out::add);
			}
		}
		return out;
	}

	private Optional<TableImportDescriptor> tryExtractDescriptor(Validator validator, String tag, PreprocessingDirectories description, File descriptionFile) throws IOException {
		InputFile file = InputFile.fromDescriptionFile(descriptionFile, description, tag);
		try {
			TableImportDescriptor descr = file.readDescriptor(validator, tag);
			// Test if all Inputs exist
			for(TableInputDescriptor inputs : descr.getInputs()) {
				if(!inputs.getSourceFile().exists()) {
					log.trace("Skipping import {} because source file {} does not exists.", descriptionFile, inputs.getSourceFile());
					skipped.add(new StringBuilder().append(descriptionFile).append(" with tag ").append(tag).toString());
					return Optional.empty();
				}
			}
			
			descr.setInputFile(file);

			// Override name to tag if present
			if (!Strings.isNullOrEmpty(tag)) {
				descr.setName(tag);
			}

			return Optional.of(descr);
		}
		catch (Exception e) {
			log.error("Failed to process " + LogUtil.printPath(descriptionFile), e);
			if(isFailFast) {
				System.exit(1);
			}
			failed.add(file.getDescriptionFile().toString());
		}
		return Optional.empty();
	}
}
