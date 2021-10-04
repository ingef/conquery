package com.bakdata.conquery.models.auth.apitoken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Validator;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentClosedException;
import jetbrains.exodus.env.Environments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@RequiredArgsConstructor
public class TokenStorage implements Managed {

	private static final int ENVIRONMENT_CLOSING_RETRIES = 2;
	private static final int ENVIRONMENT_CLOSING_TIMEOUT = 2; // seconds

	private final Path storageDir;
	private final XodusConfig storeConfig;
	private final Validator validator;
	private final ObjectMapper objectMapper;

	private Environment environment;
	private Store<ApiTokenHash, ApiTokenData> dataStore;
	private Store<UUID, ApiTokenData.MetaData> metaDataStore;
	private ArrayList<XodusStore> openStoresInEnv = new ArrayList<>();

	@Override
	public void start(){
		String storeName = "api-token";
		File tokenStore = new File(storageDir.toFile(), storeName);
		environment = Environments.newInstance(tokenStore, storeConfig.createConfig());

		final XodusStore data = new XodusStore(
				environment,
				"DATA",
				this::closeStoreHook,
				this::removeStoreHook
		);
		dataStore = StoreMappings.cached(new SerializingStore<>(
				data,
				validator,
				objectMapper,
				ApiTokenHash.class,
				ApiTokenData.class,
				true,
				false,
				null
		));
		openStoresInEnv.add(data);

		final XodusStore meta = new XodusStore(
				environment,
				"META",
				this::closeStoreHook,
				this::removeStoreHook
		);
		metaDataStore = StoreMappings.cached(new SerializingStore<>(
				meta,
				validator,
				objectMapper,
				UUID.class,
				ApiTokenData.MetaData.class,
				true,
				false,
				null
		));
		openStoresInEnv.add(meta);
	}



	private void removeStoreHook(XodusStore store) {
		openStoresInEnv.remove(store);

		if (!openStoresInEnv.isEmpty()){
			return;
		}

		final Environment environment = store.getEnvironment();
		log.info("Removed last XodusStore in Environment. Removing Environment as well: {}", environment.getLocation());

		final List<String> xodusStores= environment.computeInReadonlyTransaction(environment::getAllStoreNames);

		if (!xodusStores.isEmpty()){
			throw new IllegalStateException("Cannot delete environment, because it still contains these stores:" + xodusStores);
		}

		environment.close();

		try {
			FileUtil.deleteRecursive(Path.of(environment.getLocation()));
		}
		catch (IOException e) {
			log.error("Cannot delete directory of removed Environment[{}]", environment.getLocation(), e);
		}
	}

	private void closeStoreHook(XodusStore store) {
		openStoresInEnv.remove(store);
		final Environment environment = store.getEnvironment();
		if (!openStoresInEnv.isEmpty()){
			return;
		}
		if (!environment.isOpen()) {
			return;
		}
		environment.close();
	}



	@Override
	public void stop() throws Exception {
		if (!environment.isOpen()) {
			return;
		}
		for(int retries = 0; retries < ENVIRONMENT_CLOSING_RETRIES; retries++) {
			try {
				log.info("Closing the environment.");
				environment.close();
				return;
			}
			catch (EnvironmentClosedException e) {
				log.warn("Environment was already closed, which is odd but mayby the stop() lifecycle event fired twice");
				return;
			}
			catch (ExodusException e) {
				if (retries == 0) {
					log.info("The environment is still working on some transactions. Retry");
				}
				log.info("Waiting for {} seconds to retry.", ENVIRONMENT_CLOSING_TIMEOUT);
				Thread.sleep(ENVIRONMENT_CLOSING_TIMEOUT * 1000 /* milliseconds */);
			}
		}
		// Close the environment with force
		log.info("Closing the environment forcefully");
		environment.getEnvironmentConfig().setEnvCloseForcedly(true);
		environment.close();

	}

	public ApiTokenData get(ApiTokenHash tokenHash) {
		return dataStore.get(tokenHash);
	}

	public void updateMetaData(UUID id, ApiTokenData.MetaData metaData) {
		metaDataStore.update(id, metaData);
	}

	public void add(ApiTokenHash hash, ApiTokenData apiTokenData) {
		dataStore.add(hash, apiTokenData);
	}

	public Iterator<Pair<ApiTokenData, ApiTokenData.MetaData>> getAll() {
		return dataStore.getAll().stream().map(tokenData -> Pair.of(tokenData, metaDataStore.get(tokenData.getId()))).iterator();
	}

	public Optional<ApiTokenData> getByUUID(UUID tokenId) {
		// Find the corresponding token data and extract its hash
		for (ApiTokenData apiTokenData : dataStore.getAll()) {
			if (tokenId.equals(apiTokenData.getId())) {
				return Optional.of(apiTokenData);
			}
		}
		return Optional.empty();
	}

	public void deleteToken(ApiTokenData token) {


		final ApiTokenHash hash = token.getTokenHash();

		synchronized (this) {
			// This should never return null
			ApiTokenData data = dataStore.get(hash);
			if (data == null) {
				throw new IllegalStateException("Unable to retrieve token data for hash.");
			}


			dataStore.remove(hash);
			metaDataStore.remove(token.getId());
		}

		hash.clear();
	}
}
