package com.bakdata.conquery.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.In;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.lang.Tuple;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import kotlin.jvm.functions.Function4;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Command allowing script based migration of databases. Especially useful for data that cannot be easily recreated after reimports, such as {@link com.bakdata.conquery.models.auth.entities.User}s and {@link com.bakdata.conquery.models.execution.ManagedExecution}s.
 * <p>
 * The supplied groovy scripts is expected to return a closure in the form of
 *
 * <code>
 * return {
 * String env, String store, String key, ObjectNode value -> return new Tuple(key,value)
 * }
 * </code>
 * <p>
 * The migration will call the returned method on all values in all stores, the returned {@link Tuple} will be used to insert the value into the store. The first value should contain a {@link String} as key, and and {@link ObjectNode} as value to be written into the store.
 * <p>
 * Returning null effectively deletes the processed value.
 * <p>
 * The command has four required parameters:
 * - `--in` root to the input storage, containing one or multiple environments. This storage is opened in read-only mode.
 * - `--out` root directory of the output storage where data will be written to. This storage will be truncated before usage.
 * - `--script` the above outline groovy script.
 */
@Slf4j
public class MigrateCommand extends ConqueryCommand {

	public MigrateCommand() {
		super("migrate", "Run a migration script on a store.");
	}

	@Override
	public void configure(Subparser subparser) {
		subparser
				.addArgument("--in")
				.help("Input storage directory.")
				.required(true)
				.type(Arguments.fileType().verifyIsDirectory().verifyCanRead());

		subparser
				.addArgument("--out")
				.help("Output storage directory.")
				.required(true)
				.type(Arguments.fileType());

		subparser
				.addArgument("--in-gzip")
				.help("If true, values are ungzipped before deserialization.")
				.setDefault(true)
				.type(Arguments.booleanType());

		subparser
				.addArgument("--out-gzip")
				.help("If true, values are gzipped before writing.")
				.setDefault(true)
				.type(Arguments.booleanType());

		subparser
				.addArgument("--script")
				.help("Migration Script returning a closure implementing MigrationScriptFactory. See supplementary example.groovy for details.\nSignature: String env, String store, String key, ObjectNode value -> return new Tuple(key,value)")
				.required(true)
				.type(Arguments.fileType().verifyCanRead().verifyCanExecute());
	}

	@Override
	protected void run(io.dropwizard.setup.Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception {

		log.debug("Attrs: {}", namespace.getAttrs());

		final File inStoreDirectory = namespace.get("in");
		final File outStoreDirectory = namespace.get("out");

		final boolean inGzip = namespace.get("in-gzip");
		final boolean outGzip = namespace.get("out-gzip");


		final long logsize = ((XodusStoreFactory) configuration.getStorage()).getXodus().getLogFileSize().toKilobytes();


		final File[] environments = inStoreDirectory.listFiles(File::isDirectory);

		if (environments == null) {
			log.error("In Store is empty");
			return;
		}

		// Create Groovy Shell and parse script
		final CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(MigrationScriptFactory.class.getName());
		final GroovyShell groovy = new GroovyShell(config);

		final MigrationScriptFactory factory = (MigrationScriptFactory) groovy.parse(In.file((File) namespace.get("script")).readAll());

		final Function4<String, String, JsonNode, JsonNode, Tuple> migrator = factory.run();

		final ObjectMapper mapper = Jackson.BINARY_MAPPER;

		Arrays.stream(environments)
			  .parallel()
			  .forEach(xenv ->
					   {
						   final File environmentDirectory = new File(outStoreDirectory, xenv.getName());
						   environmentDirectory.mkdirs();

						   processEnvironment(xenv, logsize, environmentDirectory, migrator, mapper, inGzip, outGzip);
					   });

	}

	private void processEnvironment(File inStoreDirectory, long logSize, File outStoreDirectory, Function4<String, String, JsonNode, JsonNode, Tuple> migrator, ObjectMapper mapper, boolean inGzip, boolean outGzip) {
		final jetbrains.exodus.env.Environment inEnvironment = Environments.newInstance(
				inStoreDirectory,
				new EnvironmentConfig().setLogFileSize(logSize)
									   .setEnvIsReadonly(true)
									   .setEnvCompactOnOpen(false)
									   .setEnvCloseForcedly(true)
									   .setGcEnabled(false)
		);

		// we dump first, then enable GC.
		final jetbrains.exodus.env.Environment outEnvironment = Environments.newInstance(
				outStoreDirectory,
				new EnvironmentConfig().setLogFileSize(logSize)
									   .setGcEnabled(false)
		);


		final List<String> stores = inEnvironment.computeInReadonlyTransaction(inEnvironment::getAllStoreNames);

		log.info("Environment {} contains {} Stores {}", inStoreDirectory, stores.size(), stores);
		// Clear it before writing
		outEnvironment.clear();

		for (String store : stores) {
			final Store inStore =
					inEnvironment.computeInExclusiveTransaction(tx -> inEnvironment.openStore(store, StoreConfig.USE_EXISTING, tx, false));

			if (inStore == null) {
				log.error("{} does not exist, aborting.", store);
				continue;
			}

			final Store outStore =
					outEnvironment.computeInExclusiveTransaction(tx -> outEnvironment.openStore(store, StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, tx, true));

			if (outEnvironment.computeInReadonlyTransaction(outStore::count) > 0) {
				log.warn("Store is not empty, aborting.");
				continue;
			}

			migrateStore(inStore, outStore, migrator, mapper, inGzip, outGzip);
			log.info("Done writing {}.", store);
		}

		// enable and run gc.

		outEnvironment.getEnvironmentConfig().setGcEnabled(true);
		log.info("Starting GC.");
		outEnvironment.gc();

		outEnvironment.close();
		log.info("GC Done.");

		inEnvironment.close();
	}

	private void migrateStore(Store inStore, Store outStore, Function4<String, String, JsonNode, JsonNode, Tuple> migrator, ObjectMapper mapper, boolean inGzip, boolean outGzip) {

		final Environment inEnvironment = inStore.getEnvironment();
		final Environment outEnvironment = outStore.getEnvironment();

		ConqueryMDC.setLocation(inEnvironment.getLocation() + "\t" + inStore.getName());

		final Transaction readTx = inEnvironment.beginReadonlyTransaction();
		final Transaction writeTx = outEnvironment.beginExclusiveTransaction();

		final long count = inStore.count(readTx);
		long processed = 0;

		log.info("Contains {} Entries", count);

		try (final Cursor cursor = inStore.openCursor(readTx)) {

			while (cursor.getNext()) {

				// Everything is mapped with Smile so even the keys.
				final JsonNode key = read(mapper, cursor.getKey(), inGzip);

				final JsonNode value = read(mapper, cursor.getValue(), inGzip);

				// Apply the migrator, it will return new key and value
				final Tuple<?> migrated = migrator.invoke(inEnvironment.getLocation(), inStore.getName(), key, value);

				// => Effectively delete the object
				if (migrated == null) {
					log.debug("Deleting key `{}`", key);
					continue;
				}

				// Serialize the values and write them into new Store.
				final byte[] keyWritten = write(mapper, ((JsonNode) migrated.get(0)), outGzip);

				final byte[] valueWritten = write(mapper, ((JsonNode) migrated.get(1)), outGzip);

				if (log.isTraceEnabled()) {
					log.trace("Mapped `{}` to \n{}", new String(keyWritten), new String(valueWritten));
				}
				else if	(log.isDebugEnabled()) {
					log.debug("Mapped `{}`", new String(keyWritten));
				}

				outStore.put(writeTx, new ArrayByteIterable(keyWritten), new ArrayByteIterable(valueWritten));

				if (++processed % (1 + (count / 10)) == 0) {
					log.info("Processed {} / {} ({}%)", processed, count, Math.round(100f * (float) processed / (float) count));
				}
			}
		}
		catch (JsonMappingException e) {
			log.error("Failed to Map", e);
		}
		catch (IOException e) {
			log.error("Failed in IO", e);
		}
		finally {
			readTx.abort();
		}

		log.info("Processed {} / {} ({}%)", processed, count, 100);

		if (!writeTx.commit()) {
			log.error("Failed to commit Tx for {}", outEnvironment);
		}
	}

	private JsonNode read(ObjectMapper mapper, ByteIterable cursor, boolean gzip) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(cursor.getBytesUnsafe());

		if (gzip) {
			inputStream = new GZIPInputStream(inputStream);
		}

		return mapper.readTree(inputStream);
	}

	@SneakyThrows
	@NotNull
	private byte[] write(ObjectMapper mapper, JsonNode value, boolean gzip) throws JsonProcessingException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (OutputStream outputStream = gzip ? new GZIPOutputStream(baos) : baos) {
			mapper.writeValue(outputStream, value);
		}

		return baos.toByteArray();
	}

	/**
	 * Class defining the interface for the Groovy-Script.
	 */
	public abstract static class MigrationScriptFactory extends Script {

		/**
		 * Environment -> Store -> Key -> Value -> (Key, Value)
		 */
		@Override
		public abstract Function4<String, String, JsonNode, JsonNode, Tuple> run();
	}
}
