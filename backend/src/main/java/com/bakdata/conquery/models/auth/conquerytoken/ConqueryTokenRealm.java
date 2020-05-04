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
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;

@Slf4j
public class ConqueryTokenRealm extends ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JwtToken.class;

	private MasterMetaStorage storage;
	
	private JWTConfig jwtConfig;
	
	@JsonIgnore
	private JWTVerifier oauthTokenVerifier;
	
	
	public ConqueryTokenRealm(MasterMetaStorage storage) {
		this.storage = storage;
		updateJWTConfig(new JWTConfig());
	}
	
	private void updateJWTConfig(JWTConfig jwtConfig){
		this.jwtConfig = jwtConfig;
		oauthTokenVerifier = JWT.require(jwtConfig.getTokenSignAlgorithm()).withIssuer(getName()).build();
	}

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
	

	
	public String createTokenForUser(UserId userId) {
		if(storage.getUser(userId) == null) {
			throw new IllegalArgumentException("Cannot create a JWT for unknown user with id: " + userId);
		}
		return TokenHandler.createToken(userId.toString(), jwtConfig.getJwtDuration(), getName(), jwtConfig.getTokenSignAlgorithm());
	}
	
	@Data
	public static class JWTConfig{
		private int jwtDuration; // Hours
		
		@JsonIgnore
		private Algorithm tokenSignAlgorithm = Algorithm.HMAC256(TokenHandler.generateTokenSecret());

	}

	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		return TokenHandler.extractToken(request);
	}

}
