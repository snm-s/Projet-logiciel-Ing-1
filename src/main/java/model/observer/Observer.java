package model.observer;

public interface Observer<T> {
    void update(T event);
}
