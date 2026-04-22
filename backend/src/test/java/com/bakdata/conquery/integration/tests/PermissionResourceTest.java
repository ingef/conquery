package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.io.jetty.IllegalArgumentExceptionMapper;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.io.jetty.NoSuchElementExceptionMapper;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.PermissionResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
public class PermissionResourceTest {

	private static final AdminProcessor ADMIN_PROCESSOR = mock(AdminProcessor.class);
	private static final ResourceExtension EXT = ResourceExtension.builder()
			.addResource(new PermissionResource(ADMIN_PROCESSOR))
			.setRegisterDefaultExceptionMappers(false)
			.addProvider(JsonValidationExceptionMapper.class)
			.addProvider(NoSuchElementExceptionMapper.class)
			.addProvider(IllegalArgumentExceptionMapper.class)
			.build();

	static Stream<Arguments> testParams() {
		return Stream.of(
				Arguments.of("domain", 204),
				Arguments.of("domain:", 204),
				Arguments.of("domain:operation", 204),
				Arguments.of("domain:operation:", 204),
				Arguments.of("domain:operation:instance", 204),
				Arguments.of("domain:operation1,:instance", 204),
				Arguments.of("domain-hyphen:operation,:instance@at", 204),
				Arguments.of("domain-hyphen:operation,:instance_underscore", 204),
				Arguments.of("domain:operation1,operation2:instance", 204),
				Arguments.of("*", 204),
				Arguments.of("*:", 204),
				Arguments.of("", 422),
				Arguments.of(":", 422),
				Arguments.of("domain::instance", 422),
				Arguments.of("domain:operation:instance:too_many_parts", 422),
				Arguments.of("domain:,", 400)

		);
	}


	@ParameterizedTest
	@MethodSource("testParams")
	void createPermission(String permission, int httpStatus) {
		try (Response response = EXT.target("/permissions/testUser")
									.request()
									.accept(MediaType.APPLICATION_JSON)
									.post(Entity.json(permission))) {
			assertThat(response.getStatus()).as(response.toString()).isEqualTo(httpStatus);

		}

	}
}
