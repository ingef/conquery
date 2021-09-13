package com.bakdata.conquery.util;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SimpleObservable<T> {
	public interface Observer<T> {
		void onObservableChanged(T data);
	}

	private final Set<Observer<T>> mObservers = Collections.synchronizedSet(new HashSet<>());

	/**
	 * This method adds a new Observer - it will be notified when Observable changes
	 */
	public void register(@NonNull Observer<T> observer) {
		mObservers.add(observer);
	}

	/**
	 * This method removes an Observer - it will no longer be notified when Observable changes
	 */
	public void unregister(@NonNull Observer<T> observer) {
		mObservers.remove(observer);
	}

	/**
	 * This method notifies currently registered observers about Observable's change
	 */
	public void notifyObservers(T data) {
		synchronized (mObservers) {
			for (Observer<T> observer : mObservers) {
				observer.onObservableChanged(data);
			}
		}

	}
}
