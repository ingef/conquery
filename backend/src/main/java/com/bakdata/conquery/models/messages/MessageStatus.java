package com.bakdata.conquery.models.messages;

import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class MessageStatus {
	
	public static enum Status {OK, FAILED}
	
	private final Future<Status> status;
}
