package com.bakdata.conquery.models.messages;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class MessageAnswers implements Awaitable {
	
	private final List<MessageAnswer> answers;
	
	@Override
	public void awaitAnyResult() {
		for(MessageAnswer answer : answers) {
			answer.awaitAnyResult();
		}
	}

	@Override
	public void awaitSuccess() {
		for(MessageAnswer answer : answers) {
			answer.awaitSuccess();
		}
	}
}
