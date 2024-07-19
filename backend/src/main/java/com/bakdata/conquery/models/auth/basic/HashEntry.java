package com.bakdata.conquery.models.auth.basic;

/**
 * Container class for the entries in the store consisting of the salted password hash and the corresponding salt.
 */
public record HashEntry(String hash) {}

