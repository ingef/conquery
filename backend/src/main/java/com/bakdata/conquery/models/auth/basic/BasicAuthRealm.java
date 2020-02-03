package com.bakdata.conquery.models.auth.basic;


import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.CredentialType;
import com.bakdata.conquery.models.auth.PasswordCredential;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.AuthServlet.AuthResourceProvider;
import com.bakdata.conquery.models.auth.web.TokenResource;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.jersey.DropwizardResourceConfig;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import jetbrains.exodus.env.TransactionalExecutable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.realm.AuthenticatingRealm;

@Slf4j
public class BasicAuthRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm, UserManageable, AuthResourceProvider{
	private static final String OAUTH_ACCESS_TOKEN_PARAM = "access_token";
	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JWTToken.class;
	private static final String STORENAME =  "BasicCredentialStore";
	private static final String PREFIX =  "Bearer";
	private static final int EXPIRATION_PERIOD = 12; //Hours
	
	private final XodusConfig passwordStoreConfig;
	private final String storeName;
	
	@JsonIgnore
	private Environment passwordEnvironment;
	@JsonIgnore
	private Store passwordStore;
	
	@JsonIgnore
	private Algorithm tokenSignAlgorithm;
	@JsonIgnore
	private JWTVerifier oauthTokenVerifier;
	@JsonIgnore
	private MasterMetaStorage storage;
	
		 
	public BasicAuthRealm(MasterMetaStorage storage, BasicAuthConfig config) {
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.storage = storage;
		this.storeName = config.getStoreName();
		this.passwordStoreConfig = config.getPasswordStoreConfig();
		
		tokenSignAlgorithm = Algorithm.HMAC256(config.getTokenSecret());
		oauthTokenVerifier = JWT.require(tokenSignAlgorithm)
			.withIssuer(getName())
			.build();
	}
	
	@Override
	protected void onInit() {
		super.onInit();
		File passwordStoreFile = new File(ConqueryConfig.getInstance().getStorage().getDirectory(),storeName);
		passwordEnvironment = Environments.newInstance(passwordStoreFile, passwordStoreConfig.createConfig());
		passwordStore = passwordEnvironment.computeInTransaction(new TransactionalComputable<Store>(){
			@Override
			public Store compute(Transaction txn) {
				return passwordEnvironment.openStore(STORENAME, StoreConfig.WITHOUT_DUPLICATES, txn);
			};
		});
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			// Incompatible token
			return null;
		}
		DecodedJWT decodedToken = null;
		try {
			decodedToken = oauthTokenVerifier.verify((String)token.getCredentials());
		}
		catch (TokenExpiredException e) {
			throw new ExpiredCredentialsException(e);
		}
		catch (SignatureVerificationException|AlgorithmMismatchException|InvalidClaimException e) {
			throw new IncorrectCredentialsException(e);
		}
		catch (JWTVerificationException e) {
			throw new AuthenticationException(e);
		}
		
		String username = decodedToken.getSubject();
		
		UserId userId = new UserId(username);
		User user = storage.getUser(userId);
		// try to construct a new User if none could be found in the storage
		if(user == null) {
			log.warn("Provided credentials were valid, but a corresponding user was not found in the System. You need to add a user to the system with the id: {}", userId);
			return null;
		}

		return new ConqueryAuthenticationInfo(userId, token, this);
	}
	
	public String checkCredentialsAndCreateJWT(String username, char[] password) {
		if(!validUsernamePassword(username, password)) {
			throw new AuthenticationException("Provided username or password was not valid.");
		}
		return createToken(username);
	}
	
	private String createToken(String username) {
		Date issueDate = new Date();
		Date expDate = DateUtils.addHours(issueDate, EXPIRATION_PERIOD);
		String token = JWT.create()
			.withIssuer(getName())
			.withSubject(username)
			.withIssuedAt(issueDate)
			.withExpiresAt(expDate)
			.sign(tokenSignAlgorithm);
		return token;
	}

	private boolean validUsernamePassword(String username, char[] providedCredentials) {
		// Get rid of Strings
		String storedCredentials = passwordEnvironment.computeInReadonlyTransaction(new TransactionalComputable<String>() {
			
			@Override
			public String compute(jetbrains.exodus.env.Transaction txn) {
				return StringBinding.entryToString(
					passwordStore.get(txn, StringBinding.stringToEntry(username))
					);
			};
			
		});
		
		if(storedCredentials == null) {
			throw new IncorrectCredentialsException();
		}
		
		return isCredentialValid(new String(providedCredentials), storedCredentials);
	}
	
	public void addUser(User user, List<CredentialType> credentials, boolean overrideOld) {
		Optional<PasswordCredential> optPassword = credentials.stream().filter(PasswordCredential.class::isInstance).map(PasswordCredential.class::cast).collect(MoreCollectors.toOptional());
		if(!optPassword.isPresent()) {
			log.trace("No password credential provided. Not adding {} to {}", user.getName(), getName());
			return;
		}
		
		passwordEnvironment.executeInExclusiveTransaction(new TransactionalExecutable() {
			
			@Override
			public void execute(Transaction txn) {
				ArrayByteIterable usernameByteIt = StringBinding.stringToEntry(user.getName());
				ArrayByteIterable passwordByteIt = StringBinding.stringToEntry(optPassword.get().getPassword());
				if(overrideOld) {
					if(passwordStore.put(txn, usernameByteIt, passwordByteIt)) {
						log.info("Added/overrided {} successfully to the authentication store.", user.getName());
						
					}
					
				}
				else if(passwordStore.add(txn, usernameByteIt, passwordByteIt)) {
					log.info("Added {} successfully to the authentication store.", user.getName());
				} else {
					log.info("The user {} was not added to the authentication store. Entry already existed", user.getName());
				}
			}
			
		});
	}

	private static boolean isCredentialValid(String providedCredentials, String storedCredentials) {
		return providedCredentials.equals(storedCredentials);
	}
	

	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		AuthenticationToken tokenHeader = extractTokenFromHeader(request);
		AuthenticationToken tokenQuery = extractTokenFromQuery(request);
		if(tokenHeader == null && tokenQuery == null) {
			// No token could be parsed
			return null;
		} else if (tokenHeader != null && tokenQuery != null) {
			log.warn("There were tokens in the request header and query string provided, which is forbidden. See: https://tools.ietf.org/html/rfc6750#section-2");
			return null;
		} else if (tokenHeader != null) {
			log.trace("Extraced the request header token");
			return tokenHeader;
		}
		log.trace("Extraced the query string token");
		return tokenQuery;
	}

	/**
	 * Code obtained from the Dropwizard project {@link OAuthCredentialAuthFilter}.
	 * 
	 * Parses a value of the `Authorization` header in the form of `Bearer a892bf3e284da9bb40648ab10`.
	 *
	 * @param header the value of the `Authorization` header
	 * @return a token
	 */
	private static AuthenticationToken extractTokenFromHeader(ContainerRequestContext request) {

        final String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);


        
		if (header == null) {
            return null;
        }


        final int space = header.indexOf(' ');
        if (space <= 0) {
            return null;
        }

        final String method = header.substring(0, space);
        if (!PREFIX.equalsIgnoreCase(method)) {
            return null;
        }

		return new JWTToken(header.substring(space + 1));
	}
	
	@Nullable
	private static JWTToken extractTokenFromQuery(ContainerRequestContext request) {
		// If Authorization header is not used, check query parameter where token can be
		// passed as well		
		String credentials = request.getUriInfo().getQueryParameters().getFirst(OAUTH_ACCESS_TOKEN_PARAM);
		if(credentials != null) {
			return new JWTToken(credentials);
		}
		return null;
	}

	@SuppressWarnings("serial")
	@AllArgsConstructor
	private static class JWTToken implements AuthenticationToken{
		private String token;

		@Override
		public Object getPrincipal() {
			throw new UnsupportedOperationException("No principal availibale for this token type");
		}

		@Override
		public Object getCredentials() {
			return token;
		}
	}
		
	
	/**
	 *  Obtained from https://stackoverflow.com/questions/5513144/converting-char-to-byte
	 */
	private byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	@Override
	public void registerResources(DropwizardResourceConfig jerseyConfig) {
		jerseyConfig.register(new TokenResource(this));
	}
}
