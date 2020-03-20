package com.bakdata.conquery.resources.unprotected;

import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.ServletUtils;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jetty.setup.ServletEnvironment;
import lombok.experimental.UtilityClass;
import org.apache.shiro.realm.Realm;

/**
 * This servlet is used to register resources that provide authentication
 * services for the user (e.g. login and token retrieval) but that require no
 * authentication. These are registered under the '/auth' path of the servlet.
 * Since Dropwizard is served on two ports (app and admin), we have two separate
 * sevlets. This removes the handling of port numbers if a resource must
 * redirect the user for authentication.
 */
@UtilityClass
public class AuthServlet {

	/**
	 * Marker interface for classes that provide authentication functionality for
	 * the admin servlet.
	 */
	public interface AuthAdminUnprotectedResourceProvider {

		void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig);

	}

	/**
	 * Marker interface for classes that provide authentication functionality for
	 * the api servlet.
	 */
	public interface AuthApiUnprotectedResourceProvider {

		void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig);

	}

	public static void registerUnprotectedApiResources(AuthorizationController controller, MetricRegistry metrics, ConqueryConfig config, ServletEnvironment servletEnvironment, ObjectMapper objectMapper) {

		DropwizardResourceConfig jerseyConfig = ServletUtils.createServlet("auth", metrics, config, servletEnvironment, objectMapper);

		// Scan realms if they need to add resources
		for (Realm realm : controller.getRealms()) {
			if (realm instanceof AuthApiUnprotectedResourceProvider) {
				((AuthApiUnprotectedResourceProvider) realm).registerApiUnprotectedAuthenticationResources(jerseyConfig);
			}
		}
	}

	public static void registerUnprotectedAdminResources(AuthorizationController controller, MetricRegistry metrics, ConqueryConfig config, ServletEnvironment servletEnvironment, ObjectMapper objectMapper) {

		DropwizardResourceConfig jerseyConfig = ServletUtils.createServlet("auth", metrics, config, servletEnvironment, objectMapper);

		// Scan realms if they need to add resources
		for (Realm realm : controller.getRealms()) {
			if (realm instanceof AuthAdminUnprotectedResourceProvider) {
				((AuthAdminUnprotectedResourceProvider) realm).registerAdminUnprotectedAuthenticationResources(jerseyConfig);
			}
		}
	}

}
