package com.bakdata.conquery.apiv1.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.bakdata.conquery.io.HasResourceBundle;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Specifies the tempporal resolution that should be used in the resulting
 * {@link DateContext} for grouping. It is important for this class to keep the order of the
 * Enum members.
 *
 */
public enum DateContextMode implements HasResourceBundle {
	/**
	 * For returning contexts with a single {@link CDateRange} for the entire
	 * {@link FeatureGroup}.
	 */
	COMPLETE{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			ArrayUtils.indexOf(DateContextMode.values(), this);
			return List.of(range);
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * years.
	 */
	YEARS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredYears();
		}
	},

	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * quarters.
	 */
	QUARTERS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredQuarters();
		}
	},
	
	/**
	 * The {@link CDateRange} contexts per {@link FeatureGroup} are subdivided into
	 * days.
	 */
	DAYS{
		@Override
		public List<CDateRange> subdivideRange(CDateRange range) {
			return range.getCoveredDays();
		}
	};
	
//	This causes a runtime error at the moment. See workaround below.
//	private DateContextMode() {
//		List<DateContextMode> list = new ArrayList<>();
//		DateContextMode current = this;
//		do {
//			list.add(current);
//			current = getNextCoarserSubdivision(current);
//		}while(current != null);
//		
//		thisAndCoarserSubdivisions = UnmodifiableList.unmodifiableList(list);
//	}
	
	private List<DateContextMode> thisAndCoarserSubdivisions;
	
	/**
	 * WORKAROUND because the eclipse compiler has bug similar to this one:
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=81454
	 */
	@JsonIgnore
	public List<DateContextMode> getThisAndCoarserSubdivisions(){
		if (thisAndCoarserSubdivisions != null) {
			return thisAndCoarserSubdivisions;
		}
		List<DateContextMode> list = new ArrayList<>();
		DateContextMode current = this;
		do {
			// Sort from coarse to finer
			list.add(0,current);
			current = getNextCoarserSubdivision(current);
		}while(current != null);
		
		thisAndCoarserSubdivisions = UnmodifiableList.unmodifiableList(list);
		return thisAndCoarserSubdivisions;
	}
	

	public List<CDateRange> subdivideRange(CDateRange range){
		throw new UnsupportedOperationException();
	}
	
	private static DateContextMode getNextCoarserSubdivision(DateContextMode mode) {
		DateContextMode[] modes = DateContextMode.values();
		int index = ArrayUtils.indexOf(modes, mode) - 1;
		return index >= 0? modes[index] : null;
	}
	
	@Override
	public ResourceBundle getBundle(Locale locale) {
		return ResourceBundle.getBundle(DateContextModeResource.class.getName(), locale);
	}
}
