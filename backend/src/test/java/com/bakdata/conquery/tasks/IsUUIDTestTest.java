package com.bakdata.conquery.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class IsUUIDTestTest {

	private static Stream<String> randomUUIDs(){
		return Stream.generate(UUID::randomUUID).map(UUID::toString).limit(50);
	}

	@ParameterizedTest
	@MethodSource(value = "randomUUIDs")
	public void UUIDisUUID(String text){
		log.info(text);
		assertThat(text).matches(QueryCleanupTask::isDefaultLabel);
	}

	@Test
	public void BrokenUUIDisNotUUID(){
		assertThat("205e461e-f9c0-8df2-504983f2ba13").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));
		assertThat("8eb4ddc7-491c-44e1#b319-1f2997d1c3f1").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));
		assertThat("This-is-not-a-8eb4ddc7-491c-44e1-b319-1f2997d1c3f1").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));
		assertThat("8eb4ddc7-491c-44e1--319-1f2997d1c3f1").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));
		assertThat("This-is-not-a-uu-id").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));

		// This is a valid UUID but with characters outside of it's value range.
		assertThat("8eb4ddc7-491c-44e1-b319-1f2997h1c3g1").matches(Predicate.not(QueryCleanupTask::isDefaultLabel));
	}



}
