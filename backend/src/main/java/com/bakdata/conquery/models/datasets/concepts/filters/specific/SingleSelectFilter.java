package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.filter.event.SubstringMultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SingleSelectFilterConverter;
import net.minidev.json.annotate.JsonIgnore;
import org.apache.parquet.Strings;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * This filter represents a select in the front end. This means that the user can select exactly one or value from a list of values.",
 *
 * @jsonExample {"label":"gender","column":"reference_data.gender","type":"SINGLE_SELECT"}
 */
@CPSType(id = "SINGLE_SELECT", base = Filter.class)
public class SingleSelectFilter extends SelectFilter<String> {

	@Override
	public FilterNode<?> createFilterNode(String value) {
		if (getSubstringRange() != null && !getSubstringRange().isAll()) {
			return new SubstringMultiSelectFilterNode(getColumn().resolve(), Collections.singleton(value), getSubstringRange());
		}

		return new MultiSelectFilterNode(getColumn().resolve(), Collections.singleton(value));
	}

	@Override
	@JsonIgnore
	public String getFilterType() {
		return FrontendFilterType.Fields.SELECT;
	}

	@Override
	public FilterConverter<SingleSelectFilter, String> createConverter() {
		return new SingleSelectFilterConverter();
	}

	@Override
	public Condition convertEventFilter(String table, String values, ConversionContext conversionContext) {
		final boolean withEmpty = Strings.isNullOrEmpty(values);
		Field<String> field = field(DSL.name(table, getColumn().getColumn()), String.class);

		if(withEmpty)  {
			return field.isNull();
		}

		return field.eq(val(values));
	}
}
