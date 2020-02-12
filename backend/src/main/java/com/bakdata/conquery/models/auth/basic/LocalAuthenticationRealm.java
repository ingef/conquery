package com.bakdata.conquery.models.auth.basic;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.stores.IStoreInfo;
import com.bakdata.conquery.io.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.PasswordHasher.HashedEntry;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.AdminServlet.AuthAdminResourceProvider;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthAdminUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthApiUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import io.dropwizard.jersey.DropwizardResourceConfig;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * This realm stores credentials in a local database ({@link XodusStore}). Upon
 * successful authentication using username and password the authenticated user
 * is given a signed JWT for further authentication over following requests. The
 * realm offers a basic user management, which is decoupled form the
 * authorization related user information that is saved in the
 * {@link MasterMetaStorage}. So adding or removing a user in this realm does
 * not change the {@link MasterMetaStorage}. {@link Conquery} interacts with
 * this realm using the Shiro frame work. However, endusers can interface it
 * through specific endpoints that are registerd by this realm.
 */
@Slf4j
public class LocalAuthenticationRealm extends ConqueryAuthenticationRealm implements UserManageable, AuthApiUnprotectedResourceProvider, AuthAdminUnprotectedResourceProvider, AuthAdminResourceProvider {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JwtToken.class;
	private int jwtDuration; // Hours

	private final XodusConfig passwordStoreConfig;
	private final String storeName;

	@JsonIgnore
	private Environment passwordEnvironment;
	@JsonIgnore
	private XodusStore passwordStore;

	@JsonIgnore
	private Algorithm tokenSignAlgorithm;
	@JsonIgnore
	private JWTVerifier oauthTokenVerifier;
	@JsonIgnore
	private MasterMetaStorage storage;

	@RequiredArgsConstructor
	@Getter
	private static class StoreInfo implements IStoreInfo {

		private final String xodusName;
		// Not used
		private final Class<?> keyType = String.class;
		// Not used
		private final Class<?> valueType = HashedEntry.class;

	}

	//////////////////// INITIALIZATION ////////////////////

	public LocalAuthenticationRealm(MasterMetaStorage storage, LocalAuthenticationConfig config) {
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.setCredentialsMatcher(new SkippingCredentialsMatcher());
		this.storage = storage;
		this.storeName = config.getStoreName();
		this.passwordStoreConfig = config.getPasswordStoreConfig();
		this.jwtDuration = config.getJwtDuration();
		
		String tokenSecret = generateTokenSecret();

		tokenSignAlgorithm = Algorithm.HMAC256(tokenSecret);
		oauthTokenVerifier = JWT.require(tokenSignAlgorithm).withIssuer(getName()).build();
	}
	
	/**
	 * Generate a random default token.
	 * @return The token as a {@link String}
	 */
	private static String generateTokenSecret() {
		Random rand = new SecureRandom();
		byte[] buffer = new byte[32];
		rand.nextBytes(buffer);
		return buffer.toString();
	}

	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File passwordStoreFile = new File(ConqueryConfig.getInstance().getStorage().getDirectory(), storeName);
		passwordEnvironment = Environments.newInstance(passwordStoreFile, passwordStoreConfig.createConfig());
		passwordStore = new XodusStore(passwordEnvironment, new StoreInfo("passwords"));
	}

	//////////////////// AUTHENTICATION ////////////////////

	//////////////////// FOR JWT
	@Override
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			// Incompatible token
			return null;
		}
		DecodedJWT decodedToken = null;
		try {
			decodedToken = oauthTokenVerifier.verify((String) token.getCredentials());
		}
		catch (TokenExpiredException e) {
			log.trace("The provided token is expired.");
			throw new ExpiredCredentialsException(e);
		}
		catch (SignatureVerificationException | InvalidClaimException e) {
			log.trace("The provided token was not successfully verified against its signature or claims.");
			throw new IncorrectCredentialsException(e);
		}
		catch (JWTVerificationException e) {
			log.trace("The provided token could not be verified.");
			throw new AuthenticationException(e);
		}

		String username = decodedToken.getSubject();

		UserId userId = new UserId(username);
		User user = storage.getUser(userId);
		// try to construct a new User if none could be found in the storage
		if (user == null) {
			log.warn(
				"Provided credentials were valid, but a corresponding user was not found in the System. You need to add a user to the system with the id: {}",
				userId);
			return null;
		}

		return new ConqueryAuthenticationInfo(userId, token, this);
	}

	//////////////////// FOR USERNAME/PASSWORD

	public String checkCredentialsAndCreateJWT(String username, char[] password) {
		// Check the password which is afterwards cleared
		if (!CredentialChecker.validUsernamePassword(username, password, passwordStore)) {
			throw new AuthenticationException("Provided username or password was not valid.");
		}
		// The username is in this case the email
		return TokenHandler.createToken(username, jwtDuration, getName(), tokenSignAlgorithm);
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
		return credentials.stream()
			.filter(PasswordCredential.class::isInstance)
			.map(PasswordCredential.class::cast)
			.collect(MoreCollectors.toOptional());
	}

	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		return TokenHandler.extractToken(request);
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

	//////////////////// RESOURCE REGISTRATION ////////////////////

	@Override
	public void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig) {
		jerseyConfig.register(new TokenResource(this));
		jerseyConfig.register(LoginResource.class);
	}

	@Override
	public void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig) {
		jerseyConfig.register(new TokenResource(this));
	}

	@Override
	public void registerAuthenticationAdminResources(DropwizardResourceConfig jerseyConfig) {
		LocalAuthenticationRealm thisRealm = this;
		jerseyConfig.register(new AbstractBinder() {

			@Override
			protected void configure() {
				this.bind(new UserAuthenticationManagementProcessor(thisRealm, storage)).to(UserAuthenticationManagementProcessor.class);
			}

		});
		jerseyConfig.register(UserAuthenticationManagementResource.class);
	}
}
