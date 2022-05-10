package com.bakdata.conquery.io.jackson.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Mappers;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.junit.jupiter.api.Test;

public class ClassToInstanceMapDeserializerTest {

	@Test
	void test() throws JSONException, IOException {
		ClassToInstanceMap<Object> map = MutableClassToInstanceMap.create();
		map.putInstance(MajorTypeId.class, MajorTypeId.DATE);
		map.putInstance(String.class, "test");
		
		String v= Mappers.getMapper().writeValueAsString(map);
		assertThat(
				Mappers.getMapper().readValue(v, ClassToInstanceMap.class)
		).isEqualTo(map);
	}

}
