package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
public abstract class DateColumn extends FormatColumn {

	private final DateFormat format;

	protected DateColumn(DateFormat format) {
		this.format = format;
	}


	public static class DateSet extends DateColumn {
		public static String HANDLE = "DATE_SET";

		public DateSet() {
			super(DateFormat.DATE_SET);
		}
	}

	public static class DateRange extends DateColumn {
		public static String HANDLE = "DATE_RANGE";

		public DateRange() {
			super(DateFormat.DATE_RANGE);
		}
	}

	public static class EventDate extends DateColumn {
		public static String HANDLE = "EVENT_DATE";


		public EventDate() {
			super(DateFormat.EVENT_DATE);
		}
	}


	public static class StartDate extends DateColumn {
		public static String HANDLE = "START_DATE";

		public StartDate() {
			super(DateFormat.START_END_DATE);
		}
	}

	public static class EndDate extends DateColumn {
		public static String HANDLE = "END_DATE";

		public EndDate() {
			super(DateFormat.START_END_DATE);
		}
	}
}
