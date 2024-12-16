package com.bakdata.conquery.io.storage;

/**
 * Currently just a marker interface
 */
public sealed interface Storage permits NamespacedStorage, MetaStorage {
}
