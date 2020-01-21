package com.bakdata.conquery.models.query.visitor;

import java.util.function.Consumer;

import com.bakdata.conquery.models.query.concept.CQElement;

/**
 * Visits the elements of which a query consist.
 */
@FunctionalInterface
public interface QueryVisitor extends Consumer<CQElement>{
}
