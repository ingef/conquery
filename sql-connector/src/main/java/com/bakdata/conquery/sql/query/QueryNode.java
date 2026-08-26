package com.bakdata.conquery.sql.query;

/** A normalized query-tree node accepted by SQL compilation. */
public sealed interface QueryNode permits AndNode, OrNode, NegationNode, DateRestrictionNode, ConceptNode {
}
