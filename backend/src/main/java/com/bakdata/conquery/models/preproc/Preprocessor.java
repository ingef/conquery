package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.google.common.io.CountingInputStream;
import com.univocity.parsers.csv.CsvParser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@UtilityClass
public class Preprocessor {
	private static final int PROGRESS_UPDATE_INTERVAL = 4096;

	/**
	 * Result of preprocessing one import descriptor.
	 */
	public record Result(PreprocessingJob job, Exception failure) {
		public boolean isSuccess() {
			return failure == null;
		}
	}

	private record InputConsumer(JobContext job, int inputIndex, TableInputDescriptor input, File sourceFile) {
	}

	private record SourceReference(int inputIndex, Path source) {
	}

	private static final class PreparedInputConsumer {
		private final JobContext job;
		private final GroovyPredicate filter;
		private final OutputDescription.Output primaryOutput;
		private final List<OutputDescription.Output> outputs;
		private final OutputRow outputRow;
		private final String location;

		private PreparedInputConsumer(InputConsumer consumer, String[] headers, ConqueryConfig config) {
			job = consumer.job();
			final TableInputDescriptor input = consumer.input();
			final Object2IntArrayMap<String> headerMap = TableInputDescriptor.buildHeaderMap(headers);
			final DateReader dateReader = config.getLocale().getDateReader();

			filter = input.createFilter(headers);
			primaryOutput = input.getPrimary().createForHeaders(headerMap, dateReader, config);
			outputs = new ArrayList<>(input.getOutput().length);
			for (OutputDescription output : input.getOutput()) {
				outputs.add(output.createForHeaders(headerMap, dateReader, config));
			}
			outputRow = new OutputRow(outputs.size());
			location = job.location(consumer.inputIndex(), consumer.sourceFile());
		}

		private void process(String[] row, ConqueryConfig config) {
			ConqueryMDC.setLocation(location);

			// Script failures are fatal for this descriptor, as they were in the job-centric implementation.
			if (filter != null && !filter.filterRow(row)) {
				return;
			}

			try {
				final String primaryId = (String) Objects.requireNonNull(
						primaryOutput.createOutput(row, job.result.getPrimaryColumn(), job.lineId),
						"primaryId may not be null"
				);

				final String primary = job.result.addPrimary(primaryId);
				applyOutputs(outputs, job.result.getColumns(), row, job.lineId, outputRow);
				job.result.addRow(primary, outputRow);
			}
			catch (OutputDescription.OutputException exception) {
				job.recordRowError(exception.getCause().getClass(), exception, exception.getSource(), row, config);
			}
			catch (Exception exception) {
				job.recordRowError(exception.getClass(), exception, null, row, config);
			}
			finally {
				job.lineId++;
			}
		}
	}

	private static final class JobContext {
		private final PreprocessingJob job;
		private final File preprocessedFile;
		private final File temporaryFile;
		private final Preprocessed result;
		private final Object2IntMap<Class<? extends Throwable>> exceptions = new Object2IntArrayMap<>();
		private long lineId;
		private int errors;
		private Exception failure;

		private JobContext(PreprocessingJob job, ConqueryConfig config) throws IOException {
			this.job = job;
			preprocessedFile = job.getPreprocessedFile();
			temporaryFile = new File(preprocessedFile.getParentFile(), preprocessedFile.getName() + ".tmp");
			temporaryFile.deleteOnExit();
			exceptions.defaultReturnValue(0);

			if (!Files.isWritable(temporaryFile.getParentFile().toPath())) {
				throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(temporaryFile.getParentFile()));
			}
			if (!Files.isWritable(preprocessedFile.toPath().getParent())) {
				throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(preprocessedFile.toPath().getParent()));
			}
			if (preprocessedFile.exists()) {
				FileUtils.forceDelete(preprocessedFile);
			}

			log.info("PREPROCESSING START in {}", job);
			result = new Preprocessed(config, job);
		}

		private boolean isActive() {
			return failure == null;
		}

		private String location() {
			return job.toString();
		}

		private String location(int inputIndex, File sourceFile) {
			return "%s:%s[%d/%s]".formatted(job.getDescriptor(), job.getDescriptor().getTable(), inputIndex, sourceFile.getName());
		}

		private void fail(Exception exception) {
			if (failure == null) {
				failure = exception;
			}
		}

		private void recordRowError(
				Class<? extends Throwable> exceptionClass,
				Exception exception,
				OutputDescription source,
				String[] row,
				ConqueryConfig config
		) {
			exceptions.put(exceptionClass, exceptions.getInt(exceptionClass) + 1);
			errors++;

			if (log.isTraceEnabled() || errors < config.getPreprocessor().getMaximumPrintedErrors()) {
				if (source == null) {
					log.warn("Failed to parse line: {} content: {}", lineId, row, exception);
				}
				else {
					log.warn("Failed to parse `{}` from line: {} content: {}", source, lineId, row, exception.getCause());
				}
			}
			else if (errors == config.getPreprocessor().getMaximumPrintedErrors()) {
				log.warn("More erroneous lines occurred. Only the first {} were printed.", config.getPreprocessor().getMaximumPrintedErrors());
			}
		}

		private void finish(ConqueryConfig config, int buckets) throws IOException {
			ConqueryMDC.setLocation(location());
			logErrors();
			result.write(temporaryFile, buckets);

			if (errors > 0) {
				log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%.2f", 100d * errors / lineId), errors, lineId);
			}
			if ((double) errors / (double) lineId > config.getPreprocessor().getFaultyLineThreshold()) {
				throw new RuntimeException("Too many faulty lines.");
			}

			FileUtils.moveFile(temporaryFile, preprocessedFile);
			log.info("PREPROCESSING DONE in {}", job);
		}

		private void logErrors() {
			if (errors > 0) {
				log.warn("File `{}` contained {} faulty lines of ~{} total.", job, errors, lineId);
			}
			if (log.isWarnEnabled()) {
				exceptions.forEach((clazz, count) -> log.warn("Got {} `{}`", count, clazz.getSimpleName()));
			}
		}
	}

	/**
	 * Create version of file-name with tag.
	 */
	public static File getTaggedVersion(File file, String tag, String extension) {
		if (Strings.isNullOrEmpty(tag)) {
			return file;
		}

		return new File(file.getParentFile(), file.getName().replaceAll(Pattern.quote(extension) + "$", String.format(".%s%s", tag, extension)));
	}

	/**
	 * Estimate the physical source bytes read by {@link #preprocess(Collection, ExecutorService, ProgressBar, ConqueryConfig, int, int)}.
	 */
	public static long estimateTotalCsvSizeBytes(Collection<PreprocessingJob> jobs, int maximumFanOut) {
		return partition(jobs, maximumFanOut).stream()
				.mapToLong(Preprocessor::estimateBatchSize)
				.sum();
	}

	/**
	 * Apply one descriptor's transformations and write its CQPP output.
	 */
	public static void preprocess(PreprocessingJob job, ProgressBar totalProgress, ConqueryConfig config, int buckets) throws IOException {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			final Result result = preprocess(List.of(job), executor, totalProgress, config, buckets, 1).getFirst();
			if (result.failure() instanceof IOException ioException) {
				throw ioException;
			}
			if (result.failure() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (result.failure() != null) {
				throw new IOException("Failed to preprocess " + job, result.failure());
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while preprocessing " + job, exception);
		}
		finally {
			executor.shutdownNow();
		}
	}

	/**
	 * Preprocess jobs in source-centric, bounded batches.
	 * <p>
	 * Inputs with the same ordinal and resolved source are read once per batch. Ordinal rounds preserve the input order of every descriptor, while the batch size
	 * bounds the number of live result accumulators. A source with more consumers than {@code maximumFanOut} is read once for each bounded batch.
	 */
	public static List<Result> preprocess(
			Collection<PreprocessingJob> jobs,
			ExecutorService executor,
			ProgressBar totalProgress,
			ConqueryConfig config,
			int buckets,
			int maximumFanOut
	) throws InterruptedException {
		final List<Result> results = new ArrayList<>(jobs.size());
		for (List<PreprocessingJob> batch : partition(jobs, maximumFanOut)) {
			results.addAll(preprocessBatch(batch, executor, totalProgress, config, buckets));
		}
		return results;
	}

	private static List<Result> preprocessBatch(
			List<PreprocessingJob> jobs,
			ExecutorService executor,
			ProgressBar totalProgress,
			ConqueryConfig config,
			int buckets
	) throws InterruptedException {
		final List<JobContext> contexts = new ArrayList<>(jobs.size());
		final List<Result> earlyFailures = new ArrayList<>();
		for (PreprocessingJob job : jobs) {
			try {
				contexts.add(new JobContext(job, config));
			}
			catch (Exception exception) {
				earlyFailures.add(new Result(job, exception));
			}
		}

		final int maximumInputs = contexts.stream()
				.mapToInt(context -> context.job.getDescriptor().getInputs().length)
				.max()
				.orElse(0);
		for (int inputIndex = 0; inputIndex < maximumInputs; inputIndex++) {
			final Map<Path, List<InputConsumer>> consumersBySource = groupBySource(contexts, inputIndex);
			final List<Future<?>> sourceTasks = new ArrayList<>(consumersBySource.size());
			for (List<InputConsumer> consumers : consumersBySource.values()) {
				sourceTasks.add(executor.submit(() -> processSource(consumers, totalProgress, config)));
			}
			await(sourceTasks);
		}

		final List<Future<?>> finalizationTasks = new ArrayList<>();
		for (JobContext context : contexts) {
			if (!context.isActive()) {
				continue;
			}
			finalizationTasks.add(executor.submit(() -> {
				context.result.acquireOwnership();
				try {
					context.finish(config, buckets);
				}
				catch (Exception exception) {
					context.fail(exception);
				}
				finally {
					context.result.releaseOwnership();
					ConqueryMDC.clearLocation();
				}
			}));
		}
		await(finalizationTasks);

		final List<Result> results = new ArrayList<>(jobs.size());
		results.addAll(earlyFailures);
		contexts.stream().map(context -> new Result(context.job, context.failure)).forEach(results::add);
		return results;
	}

	private static Map<Path, List<InputConsumer>> groupBySource(List<JobContext> contexts, int inputIndex) {
		final Map<Path, List<InputConsumer>> consumersBySource = new LinkedHashMap<>();
		for (JobContext context : contexts) {
			if (!context.isActive() || inputIndex >= context.job.getDescriptor().getInputs().length) {
				continue;
			}

			final TableInputDescriptor input = context.job.getDescriptor().getInputs()[inputIndex];
			final File sourceFile = resolveSourceFile(input.getSourceFile(), context.job.getCsvDirectory(), context.job.getTag());
			final Path source = sourceFile.toPath().toAbsolutePath().normalize();
			consumersBySource.computeIfAbsent(source, ignored -> new ArrayList<>())
					.add(new InputConsumer(context, inputIndex, input, sourceFile));
		}
		return consumersBySource;
	}

	private static void processSource(List<InputConsumer> consumers, ProgressBar totalProgress, ConqueryConfig config) {
		final File sourceFile = consumers.getFirst().sourceFile();
		final List<JobContext> acquired = new ArrayList<>(consumers.size());
		for (InputConsumer consumer : consumers) {
			try {
				consumer.job().result.acquireOwnership();
				acquired.add(consumer.job());
			}
			catch (Exception exception) {
				consumer.job().fail(exception);
			}
		}

		CsvParser parser = null;
		try {
			if (!(sourceFile.exists() && sourceFile.canRead())) {
				throw new FileNotFoundException(sourceFile.getAbsolutePath());
			}

			try (CountingInputStream countingInput = new CountingInputStream(new FileInputStream(sourceFile))) {
				final CSVConfig csvSettings = config.getCsv();
				parser = csvSettings.withParseHeaders(true).withSkipHeader(false).createParser();
				parser.beginParsing(FileUtil.isGZipped(sourceFile) ? new GZIPInputStream(countingInput) : countingInput, csvSettings.getEncoding());

				final String[] headers = parser.getContext().parsedHeaders();
				final List<PreparedInputConsumer> prepared = prepareConsumers(consumers, headers, config);
				long progress = 0;
				int rowsSinceProgressUpdate = 0;
				String[] row;
				while ((row = parser.parseNext()) != null) {
					rowsSinceProgressUpdate++;
					if (rowsSinceProgressUpdate == PROGRESS_UPDATE_INTERVAL) {
						progress = reportProgress(totalProgress, countingInput, progress);
						rowsSinceProgressUpdate = 0;
					}

					for (PreparedInputConsumer consumer : prepared) {
						if (!consumer.job.isActive()) {
							continue;
						}
						try {
							consumer.process(row.clone(), config);
						}
						catch (Exception exception) {
							consumer.job.fail(exception);
						}
					}
				}
				reportProgress(totalProgress, countingInput, progress);
			}
		}
		catch (Exception exception) {
			for (InputConsumer consumer : consumers) {
				consumer.job().fail(exception);
			}
		}
		finally {
			try {
				try {
					if (parser != null) {
						parser.stopParsing();
					}
				}
				catch (Exception exception) {
					for (InputConsumer consumer : consumers) {
						consumer.job().fail(exception);
					}
				}
			}
			finally {
				for (JobContext context : acquired) {
					context.result.releaseOwnership();
				}
				ConqueryMDC.clearLocation();
			}
		}
	}

	private static List<PreparedInputConsumer> prepareConsumers(List<InputConsumer> consumers, String[] headers, ConqueryConfig config) {
		final List<PreparedInputConsumer> prepared = new ArrayList<>(consumers.size());
		for (InputConsumer consumer : consumers) {
			if (!consumer.job().isActive()) {
				continue;
			}
			try {
				prepared.add(new PreparedInputConsumer(consumer, headers, config));
			}
			catch (Exception exception) {
				consumer.job().fail(exception);
			}
		}
		return prepared;
	}

	private static void await(List<Future<?>> tasks) throws InterruptedException {
		for (Future<?> task : tasks) {
			try {
				task.get();
			}
			catch (ExecutionException exception) {
				throw new IllegalStateException("Preprocessing task failed without recording its error", exception.getCause());
			}
		}
	}

	private static List<List<PreprocessingJob>> partition(Collection<PreprocessingJob> jobs, int maximumFanOut) {
		if (maximumFanOut < 1) {
			throw new IllegalArgumentException("maximumFanOut must be positive");
		}

		final List<PreprocessingJob> remaining = new ArrayList<>(jobs);
		final List<List<PreprocessingJob>> batches = new ArrayList<>((remaining.size() + maximumFanOut - 1) / maximumFanOut);
		while (!remaining.isEmpty()) {
			final List<PreprocessingJob> batch = new ArrayList<>(maximumFanOut);
			batch.add(remaining.removeFirst());
			final Set<SourceReference> sharedSources = new HashSet<>(sourceReferences(batch.getFirst()));

			while (batch.size() < maximumFanOut && !remaining.isEmpty()) {
				int bestIndex = 0;
				long bestSharedSources = -1;
				for (int index = 0; index < remaining.size(); index++) {
					final long sharedSourceCount = sourceReferences(remaining.get(index)).stream().filter(sharedSources::contains).count();
					if (sharedSourceCount > bestSharedSources) {
						bestIndex = index;
						bestSharedSources = sharedSourceCount;
					}
				}

				final PreprocessingJob selected = remaining.remove(bestIndex);
				batch.add(selected);
				sharedSources.addAll(sourceReferences(selected));
			}
			batches.add(batch);
		}
		return batches;
	}

	private static Set<SourceReference> sourceReferences(PreprocessingJob job) {
		final Set<SourceReference> sources = new HashSet<>();
		for (int inputIndex = 0; inputIndex < job.getDescriptor().getInputs().length; inputIndex++) {
			final TableInputDescriptor input = job.getDescriptor().getInputs()[inputIndex];
			final Path source = resolveSourceFile(input.getSourceFile(), job.getCsvDirectory(), job.getTag()).toPath().toAbsolutePath().normalize();
			sources.add(new SourceReference(inputIndex, source));
		}
		return sources;
	}

	private static long estimateBatchSize(List<PreprocessingJob> batch) {
		final int maximumInputs = batch.stream()
				.mapToInt(job -> job.getDescriptor().getInputs().length)
				.max()
				.orElse(0);
		long totalSize = 0;
		for (int inputIndex = 0; inputIndex < maximumInputs; inputIndex++) {
			final int currentInput = inputIndex;
			final Set<Path> sources = batch.stream()
					.filter(job -> currentInput < job.getDescriptor().getInputs().length)
					.map(job -> resolveSourceFile(
							job.getDescriptor().getInputs()[currentInput].getSourceFile(),
							job.getCsvDirectory(),
							job.getTag()
					).toPath().toAbsolutePath().normalize())
					.collect(Collectors.toSet());
			totalSize += sources.stream().mapToLong(source -> source.toFile().length()).sum();
		}
		return totalSize;
	}

	/**
	 * Apply each output for a single row into the reusable output buffer.
	 */
	private static void applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, long lineId, OutputRow outRow)
			throws ParsingException, OutputDescription.OutputException {
		for (int index = 0; index < outputs.size(); index++) {
			final OutputDescription.Output out = outputs.get(index);

			try {
				final Parser parser = columns[index].getParser();
				out.createOutput(row, parser, lineId, outRow, index);
			}
			catch (Exception exception) {
				outRow.clear();
				throw new OutputDescription.OutputException(out.getDescription(), exception);
			}
		}
	}

	private static long reportProgress(ProgressBar totalProgress, CountingInputStream countingInput, long previousProgress) {
		final long currentProgress = countingInput.getCount();
		final long progress = currentProgress - previousProgress;
		if (progress > 0) {
			totalProgress.addCurrentValue(progress);
		}
		return currentProgress;
	}

	/**
	 * Resolve a source file with tag appended if present, in csvDirectory.
	 */
	public static File resolveSourceFile(String fileName, Path csvDirectory, Optional<String> tag) {
		if (tag.isEmpty()) {
			return csvDirectory.resolve(fileName).toFile();
		}

		String name = fileName;
		final String suffix;

		if (name.endsWith(".csv.gz")) {
			name = name.substring(0, name.length() - ".csv.gz".length());
			suffix = ".csv.gz";
		}
		else if (name.endsWith(".csv")) {
			name = name.substring(0, name.length() - ".csv".length());
			suffix = ".csv";
		}
		else {
			throw new IllegalArgumentException("Unknown suffix for file " + name);
		}

		return csvDirectory.resolve(name + "." + tag.get() + suffix).toFile();
	}
}
