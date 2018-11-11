package com.indix.mlflow_gocd.utils;

public interface Function<I, O> {
    public O apply(I input);
}
