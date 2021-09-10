package com.bakdata.conquery.util;

import java.util.HashSet;
import java.util.Set;

public class SimpleObservable<T> {
	public interface Observer<T> {
		void onObservableChanged(T data);
	}

	// this is the object we will be synchronizing on ("the monitor")
	private final Object MONITOR = new Object();
	private Set<Observer<T>> mObservers;

	/**
	 * This method adds a new Observer - it will be notified when Observable changes
	 */
	public void register(Observer<T> observer) {
		if (observer == null) return;
		synchronized (MONITOR) {
			if (mObservers == null) {
				mObservers = new HashSet<>(1);
			}
			if (mObservers.add(observer) && mObservers.size() == 1) {
				//TODO some initialization when first observer added
			}
		}
	}

	/**
	 * This method removes an Observer - it will no longer be notified when Observable changes
	 */
	public void unregister(Observer<T> observer) {
		if (observer == null) return;
		synchronized (MONITOR) {
			if (mObservers != null && mObservers.remove(observer) && mObservers.isEmpty()) {
				//TODO some cleanup when last observer removed
			}
		}
	}

	/**
	 * This method notifies currently registered observers about Observable's change
	 */
	public void notifyObservers(T data) {
		Set<Observer<T>> observersCopy;
		synchronized (MONITOR) {
			if (mObservers == null) return;
			observersCopy = new HashSet<>(mObservers);
		}
		for (Observer<T> observer : observersCopy) {
			observer.onObservableChanged(data);
		}
	}
}
