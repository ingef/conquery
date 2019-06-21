package com.bakdata.eva.models.preproc.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.eva.models.preproc.output.daysinrange.DaysInRange;
import com.bakdata.eva.models.preproc.output.daysinrange.Identifier;
import com.bakdata.eva.models.preproc.output.daysinrange.PatientEvent;
import com.bakdata.eva.models.preproc.output.daysinrange.TypeIdDaysInRangeMerger;
import com.bakdata.eva.models.preproc.output.daysinrange.TypeIdDaysInRangeMerger.DaysInRangeEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Ints;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Setter
@Getter
@CPSType(id="INGEF_DAYS_IN_RANGE", base= AutoOutput.class)
public class DaysInRangeOutput implements AutoOutput {

	public static final int CONSTANT_COLUMN_OFFSET = 4;
	@NotNull @Valid
	private List<Output> identifiers = new ArrayList<>();
	@Min(0)
	private int yearColumn;
	private TypeId dayType;
	@Min(0)
	private int quarterColumn;
	@JsonIgnore
	private transient final Collection<PatientEvent> emitted = new ArrayList<>();
	@JsonIgnore
	private transient final TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																					.mergeCondition(DaysInRangeOutput::isFullQuarter)
																					.rangeFunction(DaysInRange::rangeFromStart)
																					.consumer(emitted::add)
																					.build();


	private static boolean isFullQuarter(DaysInRange current) {
		return QuarterUtils.isBeginOfQuarter(current.getStart()) || QuarterUtils.isEndOfQuarter(current.getEnd());
	}

	@Override
	public List<OutRow> createOutput(int primaryId, String[] row, PPColumn[] columns, int inputSource, long lineId) throws ParsingException {
		DaysInRangeEntry entries = create(primaryId, row, columns, inputSource, lineId);

		if(entries != null) {
			merger.process(entries);
		}

		return retrieveCurrent();
	}

	private List<OutRow> retrieveCurrent() throws ParsingException {
		try {
			List<OutRow> result = new ArrayList<>();
			for (PatientEvent patientEvent : emitted) {
				OutRow outRow = toOutRow(patientEvent);
				result.add(outRow);
			}
			emitted.clear();
			return result;
		}catch (IllegalArgumentException e) {
			throw new ParsingException("Failed in DaysInRangeOutput", e);
		}
	}

	@Override
	public int getWidth() {
		return CONSTANT_COLUMN_OFFSET + identifiers.size();
	}

	@Override
	public ColumnDescription getColumnDescription(int column) {
		switch (column) {
			case 0: return new ColumnDescription("date", MajorTypeId.DATE_RANGE);
			case 1: return new ColumnDescription("dayType", MajorTypeId.STRING);
			case 2: return new ColumnDescription("date_start", MajorTypeId.DATE);
			case 3: return new ColumnDescription("date_end", MajorTypeId.DATE);
//			case 4: return new ColumnDescription("pid", MajorTypeId.STRING);
			default: return getIdentifierColumnDescription(column - CONSTANT_COLUMN_OFFSET);
		}
	}

	private ColumnDescription getIdentifierColumnDescription(int index) {
		Output identifier = identifiers.get(index);
		return identifier.getColumnDescription();
	}

	@Override
	public List<OutRow> finish() throws ParsingException {
		merger.clearRemaining();
		return retrieveCurrent();
	}

	private OutRow toOutRow(PatientEvent patientEvent) throws ParsingException {

		Object[] result = new Object[getWidth()];
		result[0] = patientEvent.getRange();

		result[1] = patientEvent.getColumns()[1].getParser().parse(patientEvent.getType());

		result[2] = CDate.ofLocalDate(patientEvent.getRange().getMin());
		result[3] = CDate.ofLocalDate(patientEvent.getRange().getMax());

		final List<Object> values = patientEvent.getIdentifier().getValues();

		// skip pid as it is passed on seperately

		for(int i = 1; i < values.size(); i++) {
			result[i + CONSTANT_COLUMN_OFFSET - 1] = values.get(i);
		}

		return new OutRow(patientEvent.getPrimaryId(), patientEvent.getColumns(), result);
	}

	private DaysForType getDaysForType(String[] values) {
		String type = dayType.getType();
		int days = getDays(values, dayType.getColumn());
		return new DaysForType(days, type);
	}

	private int getDays(String[] values, int dayId) {
		return Ints.tryParse(values[dayId]);
	}

	private CDateRange getQuarter(String[] values) {
		int year = getYear(values);
		int quarter = getQuarterId(values);
		return QuarterUtils.fromQuarter(year, quarter);
	}

	private int getYear(String[] values) {
		return Integer.parseInt(values[yearColumn]);
	}

	private int getQuarterId(String[] values) {
		return Integer.parseInt(values[quarterColumn]);
	}

	private DaysInRangeEntry create(int primaryId, String[] values, PPColumn[] columns, int inputSource, long lineId) throws ParsingException {
		List<Object> identifierValues = new ArrayList<>();

		// PID is always part of merging
		identifierValues.add(primaryId);

		for (int idIndex = 0; idIndex < identifiers.size(); idIndex++) {
			Output output = identifiers.get(idIndex);

			List<Object> outputValues = output.createOutput(columns[idIndex + CONSTANT_COLUMN_OFFSET].getParser(), values, inputSource, lineId);

			if (outputValues.size() != 1) {
				throw new ParsingException("Unsupported identifier format");
			}

			identifierValues.add(outputValues.get(0));
		}

		Identifier identifier = new Identifier(identifierValues);

		DaysForType daysForType = getDaysForType(values);
		CDateRange quarter = getQuarter(values);

		int days = daysForType.getDays();

		if(days <= 0 || quarter.getNumberOfDays() < days){
			return null;
		}

		String type = daysForType.getType();
		DaysInRange daysInRange = new DaysInRange(quarter, days);

		return new DaysInRangeEntry(primaryId, columns, daysInRange, identifier, type);
	}

	@NoArgsConstructor
	@Setter
	@Getter
	public static class TypeId {

		@NotNull
		private String type;
		@Min(0)
		private int column;
	}

	@Data
	private static class DaysForType {

		private final int days;
		private final String type;
	}
}
