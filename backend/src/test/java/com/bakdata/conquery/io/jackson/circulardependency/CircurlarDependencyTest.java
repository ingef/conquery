package com.bakdata.conquery.io.jackson.circulardependency;

import java.io.IOException;
import javax.validation.Validator;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import io.dropwizard.jersey.validation.Validators;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CircurlarDependencyTest {

	private static Container container;
	private static RefChild refChild;
	private static SimpleChild simpleChild;

	@BeforeAll
	public static void init() {

		refChild = new RefChild();
		simpleChild = new SimpleChild();
		container =  new Container();
		container.setContainerChildren(new IChild[]{simpleChild, refChild});
		refChild.setParent(container);

	}

	@Test
	public void referencesTest() throws JSONException, IOException {
		final Validator validator = Validators.newValidator();
		SerializationTestUtil
				.forType(Container.class)
				.injectables(new Injectable() {
					@Override
					public MutableInjectableValues inject(MutableInjectableValues values) {
						return values.add(Validator.class, validator);
					}
				})
				.test(container);
	}


}
