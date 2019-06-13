package com.bakdata.eva;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.concept.ResultInfo;

import lombok.experimental.UtilityClass;


@UtilityClass
public class EvaConstants {

	public static final String CONTEXT_INDEX = "index";
	public static final String DATE_RANGE = "date_range";
	public static final String EVENT_DATE = "event_date";
	public static final ResultInfo CONTEXT_INDEX_INFO = new ResultInfo(CONTEXT_INDEX, ResultType.INTEGER, 0, 0);
	public static final ResultInfo DATE_RANGE_INFO = new ResultInfo(DATE_RANGE, ResultType.STRING, 0, 0);
	public static final ResultInfo EVENT_DATE_INFO = new ResultInfo(EVENT_DATE, ResultType.DATE, 0, 0);
	public static final String FEATURE_PREFIX = "feature_";
	public static final String OUTCOME_PREFIX = "outcome_";
	public static final String GROUP = "group";

}
