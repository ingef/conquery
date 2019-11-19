package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

public class ClassToInstanceMapDeserializerTest {

	@Test
	void test() throws JSONException, IOException {
		ClassToInstanceMap<Object> map = MutableClassToInstanceMap.create();
		map.putInstance(MajorTypeId.class, MajorTypeId.DATE);
		map.putInstance(String.class, "test");
		
		String v=Jackson.MAPPER.writeValueAsString(map);
		assertThat(
			Jackson.MAPPER.readValue(v, ClassToInstanceMap.class)
		).isEqualTo(map);
	}

}
