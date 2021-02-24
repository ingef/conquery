package com.bakdata.conquery.models.auth.basic;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.io.storage.IStoreInfo;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.PasswordHasher.HashedEntry;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentClosedException;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This realm stores credentials in a local database ({@link XodusStore}). Upon
 * successful authentication using username and password the authenticated user
 * is given a signed JWT for further authentication over following requests. The
 * realm offers a basic user management, which is decoupled form the
 * authorization related user information that is saved in the
 * {@link MetaStorage}. So adding or removing a user in this realm does
 * not change the {@link MetaStorage}. {@link Conquery} interacts with
 * this realm using the Shiro frame work. However, endusers can interface it
 * through specific endpoints that are registerd by this realm.
 */
@Slf4j
public class LocalAuthenticationRealm extends ConqueryAuthenticationRealm implements UserManageable, UsernamePasswordChecker {

	private static final int ENVIRONMNENT_CLOSING_RETRYS = 2;
	private static final int ENVIRONMNENT_CLOSING_TIMEOUT = 2; // seconds
	// Get the path for the storage here so it is set when as soon the first class is instantiated (in the ManagerNode)
	// In the StandaloneCommand this directory is overriden multiple times before LocalAuthenticationRealm::onInit for the ShardNodes, so this is a problem.
	private final File storageDir;

	private final XodusConfig passwordStoreConfig;
	private final String storeName;

	@JsonIgnore
	private Environment passwordEnvironment;
	@JsonIgnore
	private XodusStore passwordStore;

	@JsonIgnore
	private final MetaStorage storage;
	@JsonIgnore
	private final ConqueryTokenRealm centralTokenRealm;

	@RequiredArgsConstructor
	@Getter
	private static class StoreInfo implements IStoreInfo {

		private final String name;
		// Not used
		private final Class<?> keyType = String.class;
		// Not used
		private final Class<?> valueType = HashedEntry.class;

	}

	//////////////////// INITIALIZATION ////////////////////

	public LocalAuthenticationRealm(MetaStorage storage,  ConqueryTokenRealm centralTokenRealm, String storeName, File storageDir, XodusConfig passwordStoreConfig) {
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.storage = storage;
		this.storeName = storeName;
		this.storageDir = storageDir;
		this.centralTokenRealm = centralTokenRealm;
		this.passwordStoreConfig = passwordStoreConfig;
	}

	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File passwordStoreFile = new File(storageDir, storeName);
		passwordEnvironment = Environments.newInstance(passwordStoreFile, passwordStoreConfig.createConfig());
		passwordStore = new XodusStore(passwordEnvironment, new StoreInfo("passwords"), new ArrayList<>(), (e) -> e.close(), (e) -> {});
	}

	//////////////////// AUTHENTICATION ////////////////////

	//////////////////// FOR JWT
	/**
	 *  Should not be called since the tokens are now handled by the ConqueryTokenRealm.
	 */
	@Override
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		throw new UnsupportedOperationException("Should not be called since the tokens are now handled by the ConqueryTokenRealm.");
	}

	//////////////////// FOR USERNAME/PASSWORD

	public String checkCredentialsAndCreateJWT(String username, char[] password) {
		// Check the password which is afterwards cleared
		if (!CredentialChecker.validUsernamePassword(username, password, passwordStore)) {
			throw new AuthenticationException("Provided username or password was not valid.");
		}
		// The username is in this case the email
		return centralTokenRealm.createTokenForUser(new UserId(username));
	}

	/**
	 * Converts the provided password to a Xodus compatible hash.
	 */
	private static ByteIterable passwordToHashedEntry(Optional<PasswordCredential> optPassword) {
		return HashedEntry.asByteIterable(PasswordHasher.generateHashedEntry(optPassword.get().getPassword()));
	}

	/**
	 * Checks the provided credentials for the realm-compatible
	 * {@link PasswordCredential}. However only one credential of this type is
	 * allowed to be provided.
	 *
	 * @param credentials
	 *            A list of possible credentials.
	 * @return The password credential.
	 */
	private static Optional<PasswordCredential> getTypePassword(List<CredentialType> credentials) {
		if(credentials == null) {
			return Optional.empty();
		}
		return credentials.stream()
			.filter(PasswordCredential.class::isInstance)
			.map(PasswordCredential.class::cast)
			.collect(MoreCollectors.toOptional());
	}

	//////////////////// USER MANAGEMENT ////////////////////

	@Override
	public boolean addUser(User user, List<CredentialType> credentials) {
		Optional<PasswordCredential> optPassword = getTypePassword(credentials);
		if (!optPassword.isPresent()) {
			log.trace("No password credential provided. Not adding {} to {}", user.getName(), getName());
			return false;
		}
		ArrayByteIterable usernameByteIt = StringBinding.stringToEntry(user.getId().getEmail());
		ByteIterable passwordByteIt = passwordToHashedEntry(optPassword);

		return passwordStore.add(usernameByteIt, passwordByteIt);
	}

	@Override
	public boolean updateUser(User user, List<CredentialType> credentials) {
		Optional<PasswordCredential> optPassword = getTypePassword(credentials);
		if (!optPassword.isPresent()) {
			log.trace("No password credential provided. Not adding {} to {}", user.getName(), getName());
			return false;
		}
		ArrayByteIterable usernameByteIt = StringBinding.stringToEntry(user.getId().getEmail());
		ByteIterable passwordByteIt = passwordToHashedEntry(optPassword);

		return passwordStore.update(usernameByteIt, passwordByteIt);

	}

	@Override
	public boolean removeUser(User user) {
		return passwordStore.remove(StringBinding.stringToEntry(user.getId().getEmail()));
	}

	@Override
	public List<UserId> getAllUsers() {
		List<String> listId = new ArrayList<>();
		// Iterate over the store entries by collecting all keys (UserIds/emails).
		// These must be turned from their binary format into Strings.
		passwordStore.forEach((k, v) -> listId.add(StringBinding.entryToString(k)));

		// Finally the Strings are turned into UserIds
		return listId.stream().map(UserId::new).collect(Collectors.toList());
	}


	
	//////////////////// LIFECYCLE MANAGEMENT ////////////////////
		
	@Override
	public void destroy() throws InterruptedException {
		for(int retries = 0; retries < ENVIRONMNENT_CLOSING_RETRYS; retries++) {			
			try {
				log.info("Closing the password environment.");
				passwordStore.close();
				return;
			}
			catch (EnvironmentClosedException e) {
				log.warn("Password environment was already closed, which is odd but mayby the stop() lifecycle event fired twice");
				return;
			}
			catch (ExodusException e) {
				if (retries == 0) {
					log.info("The environment is still working on some transactions. Retry");				
				}
				log.info("Waiting for {} seconds to retry.");
				Thread.sleep(ENVIRONMNENT_CLOSING_TIMEOUT*1000 /* milliseconds */);
				continue;
			}
		}
		// Close the environment with force
		log.info("Closing the environment forcefully");
		passwordEnvironment.getEnvironmentConfig().setEnvCloseForcedly(true);
		passwordEnvironment.close();
		return;

	}
}
