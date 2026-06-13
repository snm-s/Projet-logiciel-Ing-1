package model.observer;

import java.util.ArrayList;
import java.util.List;

public class Subject<T> {
    private final List<Observer<T>> observers = new ArrayList<>();

    /**
     * Adds observer.
     * @param observer the observer.
     */
    public void addObserver(Observer<T> observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Removes observer.
     * @param observer the observer.
     */
    public void removeObserver(Observer<T> observer) {
        observers.remove(observer);
    }

    /**
     * Notifies observers.
     * @param event the event.
     */
    public void notifyObservers(T event) {
        for (Observer<T> observer : new ArrayList<>(observers)) {
            observer.update(event);
        }
    }
}
