package com.bakdata.conquery.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.In;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.lang.Tuple;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.DataSize;
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

@Slf4j
public class MigrateCommand extends Command {


	public MigrateCommand() {
		super("migrate", "Run a migration script on a store.");
	}

	@Override
	public void configure(Subparser subparser) {
		subparser
				.addArgument("--in")
				.help("Input store.")
				.required(true)
				.type(Arguments.fileType().verifyIsDirectory().verifyCanRead());

		subparser
				.addArgument("--out")
				.help("Output store.")
				.required(true)
				.type(Arguments.fileType());

		subparser
				.addArgument("--script")
				.help("Migration Script.")
				.required(true)
				.type(Arguments.fileType().verifyCanRead());

		subparser
				.addArgument("--logsize")
				.help("Log-size of stores.")
				.required(true);
	}

	@Override
	public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {

		final File inStoreDirectory = namespace.get("in");
		final File outStoreDirectory = namespace.get("out");

		final long logsize = DataSize.parse(namespace.get("logsize")).toKilobytes();


		final File[] environments = inStoreDirectory.listFiles(File::isDirectory);

		if (environments == null) {
			log.error("In Store is empty");
			return;
		}

		CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(MigrationScriptFactory.class.getName());
		GroovyShell groovy = new GroovyShell(config);

		MigrationScriptFactory factory = (MigrationScriptFactory) groovy.parse(In.file((File) namespace.get("script")).readAll());

		final Function4<String, String, String, ObjectNode, Tuple> migrator = factory.run();

		final ObjectMapper mapper = Jackson.BINARY_MAPPER;

		Arrays.stream(environments)
			  .parallel()
			  .forEach(xenv ->
					   {
						   final File environmentDirectory = new File(outStoreDirectory, xenv.getName());
						   environmentDirectory.mkdirs();
						   processEnvironment(xenv, logsize, environmentDirectory, migrator, mapper);
					   });

	}


	public static abstract class MigrationScriptFactory extends Script {

		/**
		 * Environment -> Store -> Key -> Value -> (Key, Value)
		 */
		@Override
		public abstract Function4<String, String, String, ObjectNode, Tuple> run();
	}

	private void processEnvironment(File inStoreDirectory, long logSize, File outStoreDirectory, Function4<String, String, String, ObjectNode, Tuple> migrator, ObjectMapper mapper) {
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

			doMigrate(inEnvironment, inStore, outEnvironment, outStore, migrator, mapper);
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

	private void doMigrate(Environment inEnvironment, Store inStore, Environment outEnvironment, Store outStore, Function4<String, String, String, ObjectNode, Tuple> migrator, ObjectMapper mapper) {


		final Transaction readTx = inEnvironment.beginReadonlyTransaction();
		final Transaction writeTx = outEnvironment.beginExclusiveTransaction();

		final long count = inStore.count(readTx);
		long processed = 0;

		log.info("Contains {} Entries", count);

		try (final Cursor cursor = inStore.openCursor(readTx)) {

			while (cursor.getNext()) {

				final String key = mapper.readValue(cursor.getKey().getBytesUnsafe(), String.class);

				final ObjectNode node = mapper.readValue(cursor.getValue().getBytesUnsafe(), ObjectNode.class);

				final Tuple
						migrated =
						migrator.invoke(inEnvironment.getLocation(), inStore.getName(), key, node);

				if (migrated == null) {
					log.debug("Deleting key `{}`", key);
					continue;
				}

				ByteIterable keyIter = new ArrayByteIterable(((String) migrated.get(0)).getBytes(StandardCharsets.UTF_8));
				final ByteIterable valueIter = new ArrayByteIterable(mapper.writeValueAsBytes(migrated.get(1)));

				if (log.isTraceEnabled()) {
					log.trace("Mapped `{}` to \n{}", new String(keyIter.getBytesUnsafe()), new String(valueIter.getBytesUnsafe()));
				}
				else {
					log.debug("Mapped `{}`", new String(keyIter.getBytesUnsafe()));
				}

				outStore.putRight(writeTx, keyIter, valueIter);

				if (++processed % (1 + (count / 10)) == 0) {
					log.info("Processed {} / {} ({}%)", processed, count, Math.round(100f * (float) processed / (float) count));
				}
			}
		}
		catch (IOException e) {
			//TODO handle for mapper
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
