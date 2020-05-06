package com.bakdata.conquery.models.query.resultinfo;

import java.util.function.Supplier;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class SimpleResultInfo extends ResultInfo {

	private final String name;
	private final ResultType type;


	@Override
	public String getName(PrintSettings settings) {
		return name;
	}
	
	public static Creator creator(String name, ResultType type) {
		return () -> new SimpleResultInfo(name, type);
	}
	
	@FunctionalInterface
	public static interface Creator extends Supplier<SimpleResultInfo> {
		
	}
}
