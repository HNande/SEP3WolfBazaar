package dk.via.sep3.support;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class RecordingStreamObserver<T> implements StreamObserver<T> {

    private final List<T> values = new ArrayList<>();
    private Throwable error;
    private boolean completed;

    @Override
    public void onNext(T value) {
        values.add(value);
    }

    @Override
    public void onError(Throwable error) {
        this.error = error;
    }

    @Override
    public void onCompleted() {
        completed = true;
    }

    public List<T> values() {
        return List.copyOf(values);
    }

    public Throwable error() {
        return error;
    }

    public boolean completed() {
        return completed;
    }
}