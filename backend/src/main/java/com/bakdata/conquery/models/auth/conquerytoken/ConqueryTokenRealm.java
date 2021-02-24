package com.bakdata.conquery.models.auth.conquerytoken;

import javax.ws.rs.container.ContainerRequestContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;

@Slf4j
public class ConqueryTokenRealm extends ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;

	private final MetaStorage storage;
	
	@Setter
	private JWTConfig jwtConfig = new JWTConfig();
	
	
	public ConqueryTokenRealm(MetaStorage storage) {
		this.storage = storage;
		setAuthenticationTokenClass(TOKEN_CLASS);
		setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
	}

	@Override
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			log.trace("Incompatible token. Expected {}, got {}", TOKEN_CLASS, token.getClass());
			return null;
		}
		log.trace("Token has expected format: {}\tWas: {} ", TOKEN_CLASS, token.getClass());
		DecodedJWT decodedToken = null;
		try {
			decodedToken = jwtConfig.getTokenVerifier(this).verify((String) token.getCredentials());
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
		catch (Exception e) {
			log.trace("Unable to decode token", e);
			throw new AuthenticationException(e);
		}
		log.trace("Received valid token.");

		String username = decodedToken.getSubject();

		UserId userId = UserId.Parser.INSTANCE.parse(username);
		User user = storage.getUser(userId);
		// try to construct a new User if none could be found in the storage
		if (user == null) {
			log.warn(
				"Provided credentials were valid, but a corresponding user was not found in the System. You need to add a user to the system with the id: {}",
				userId);
			return null;
		}

		return new ConqueryAuthenticationInfo(userId, token, this, true);
	}
	

	
	public String createTokenForUser(UserId userId) {
		if(storage.getUser(userId) == null) {
			throw new IllegalArgumentException("Cannot create a JWT for unknown user with id: " + userId);
		}
		return JWTokenHandler.createToken(userId.toString(), jwtConfig.getJwtDuration(), getName(), jwtConfig.getTokenSignAlgorithm());
	}
	
	public static class JWTConfig{
		@Getter
		@Setter
		private Duration jwtDuration = Duration.hours(8);
		
		@JsonIgnore
		@Getter
		private Algorithm tokenSignAlgorithm = Algorithm.HMAC256(JWTokenHandler.generateTokenSecret());
		@JsonIgnore
		private JWTVerifier tokenVerifier;
		
		@JsonIgnore
		public JWTVerifier getTokenVerifier(ConqueryAuthenticationRealm realm) {
			if(tokenVerifier == null) {
				tokenVerifier = JWT.require(tokenSignAlgorithm).withIssuer(realm.getName()).build();
			}
			return tokenVerifier;
		}
		

	}

}
