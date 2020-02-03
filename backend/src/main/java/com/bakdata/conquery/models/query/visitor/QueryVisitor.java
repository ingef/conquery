package com.bakdata.conquery.models.query.visitor;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.util.QueryUtils;

/**
 * Visits the elements of which a query consist.
 * For example in {@link QueryProcessor} to check if the query consists only of a single reusing node.
 * Reference implementations can be found in {@link QueryUtils}.
 */
@FunctionalInterface
public interface QueryVisitor extends Consumer<Visitable> {
}
