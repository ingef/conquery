package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.codehaus.groovy.control.CompilerConfiguration;

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
				.addArgument("--script")
				.help("Migration Script returning a closure implementing MigrationScriptFactory. See supplementary example.groovy for details.\nSignature: String env, String store, String key, ObjectNode value -> return new Tuple(key,value)")
				.required(true)
				.type(Arguments.fileType().verifyCanRead().verifyCanExecute());
	}

	@Override
	protected void run(io.dropwizard.setup.Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception {

		final File inStoreDirectory = namespace.get("in");
		final File outStoreDirectory = namespace.get("out");

		final long logsize = ((XodusStoreFactory) configuration.getStorage()).getXodus().getLogFileSize().toKilobytes();


		final File[] environments = inStoreDirectory.listFiles(File::isDirectory);

		if (environments == null) {
			log.error("In Store is empty");
			return;
		}

		// Create Groovy Shell and parse script
		CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(MigrationScriptFactory.class.getName());
		GroovyShell groovy = new GroovyShell(config);

		MigrationScriptFactory factory = (MigrationScriptFactory) groovy.parse(In.file((File) namespace.get("script")).readAll());

		final Function4<String, String, String, JsonNode, Tuple> migrator = factory.run();

		final ObjectMapper mapper = Jackson.BINARY_MAPPER;

		final ObjectReader keyReader = mapper.readerFor(String.class);
		final ObjectReader valueReader = mapper.readerFor(JsonNode.class);
		final ObjectWriter keyWriter = mapper.writerFor(String.class);
		final ObjectWriter valueWriter = mapper.writerFor(JsonNode.class);


		Arrays.stream(environments)
			  .parallel()
			  .forEach(xenv ->
					   {
						   final File environmentDirectory = new File(outStoreDirectory, xenv.getName());
						   environmentDirectory.mkdirs();

						   processEnvironment(xenv, logsize, environmentDirectory, migrator, keyReader, valueReader, keyWriter, valueWriter);
					   });

	}


	/**
	 * Class defining the interface for the Groovy-Script.
	 */
	public static abstract class MigrationScriptFactory extends Script {

		/**
		 * Environment -> Store -> Key -> Value -> (Key, Value)
		 */
		@Override
		public abstract Function4<String, String, String, JsonNode, Tuple> run();
	}

	private void processEnvironment(File inStoreDirectory, long logSize, File outStoreDirectory, Function4<String, String, String, JsonNode, Tuple> migrator, ObjectReader keyReader, ObjectReader valueReader, ObjectWriter keyWriter, ObjectWriter valueWriter) {
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

			doMigrate(inStore, outStore, migrator, keyReader, valueReader, keyWriter, valueWriter);
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

	private void doMigrate(Store inStore, Store outStore, Function4<String, String, String, JsonNode, Tuple> migrator, ObjectReader keyReader, ObjectReader valueReader, ObjectWriter keyWriter, ObjectWriter valueWriter) {

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
				final String key = keyReader.readValue(cursor.getKey().getBytesUnsafe());

				final JsonNode node = valueReader.readValue(cursor.getValue().getBytesUnsafe());

				// Apply the migrator, it will return new key and value
				final Tuple<?> migrated =
						migrator.invoke(inEnvironment.getLocation(), inStore.getName(), key, node);

				// => Effectively delete the object
				if (migrated == null) {
					log.debug("Deleting key `{}`", key);
					continue;
				}

				// Serialize the values and write them into new Store.
				final ByteIterable keyIter = new ArrayByteIterable(keyWriter.writeValueAsBytes(migrated.get(0)));

				final ByteIterable valueIter = new ArrayByteIterable(valueWriter.writeValueAsBytes(migrated.get(1)));

				if (log.isTraceEnabled()) {
					log.trace("Mapped `{}` to \n{}", new String(keyIter.getBytesUnsafe()), new String(valueIter.getBytesUnsafe()));
				}
				else {
					log.debug("Mapped `{}`", new String(keyIter.getBytesUnsafe()));
				}

				outStore.put(writeTx, keyIter, valueIter);

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

}
