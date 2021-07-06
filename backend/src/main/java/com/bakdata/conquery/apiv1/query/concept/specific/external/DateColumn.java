package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;

@Getter
public abstract class DateColumn extends FormatColumn {

	private final DateFormat format;

	public DateColumn(DateFormat format) {
		this.format = format;
	}

	@CPSType(id = "DATE_SET", base = FormatColumn.class)
	public static class DateSet extends DateColumn {
		public DateSet() {
			super(DateFormat.DATE_SET);
		}
	}

	@CPSType(id = "DATE_RANGE", base = FormatColumn.class)
	public static class DateRange extends DateColumn {
		public DateRange() {
			super(DateFormat.DATE_RANGE);
		}
	}

	@CPSType(id = "EVENT_DATE", base = FormatColumn.class)
	public static class EventDate extends DateColumn {
		public EventDate() {
			super(DateFormat.EVENT_DATE);
		}
	}


	@CPSType(id = "START_DATE", base = FormatColumn.class)
	public static class DateStart extends DateColumn {
		public DateStart() {
			super(DateFormat.START_END_DATE);
		}
	}

	@CPSType(id = "END_DATE", base = FormatColumn.class)
	public static class DateEnd extends DateColumn {
		public DateEnd() {
			super(DateFormat.START_END_DATE);
		}
	}
}
