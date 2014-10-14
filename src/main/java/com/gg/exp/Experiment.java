package com.gg.exp;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@CommonsLog
public class Experiment<I, OO, NO> {
    public Function<I, OO> oldFlow;
    public Function<I, NO> newFlow;
    private MetricRegistry metricRegistry;
    private ThreadPoolExecutor threadPoolExecutor;
    private String name;
    @Setter
    private int percentOfTimesToExecute = 100;
    private PrimitiveIterator.OfInt intStream;

    public Experiment(Function<I, OO> oldFlow, Function<I, NO> newFlow, String name) {
        this(oldFlow, newFlow, name, new MetricRegistry());
    }

    public Experiment(Function<I, OO> oldFlow, Function<I, NO> newFlow, String name, MetricRegistry metricRegistry) {
        this(oldFlow, newFlow, name, metricRegistry, new ThreadPoolExecutor(10, Integer.MAX_VALUE, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
    }

    public Experiment(Function<I, OO> oldFlow, Function<I, NO> newFlow, String name, MetricRegistry metricRegistry, ThreadPoolExecutor threadPoolExecutor) {
        this.oldFlow = oldFlow;
        this.newFlow = newFlow;
        this.metricRegistry = metricRegistry;
        this.name = name;
        this.threadPoolExecutor = threadPoolExecutor;
        intStream = new Random().ints(0, 100).iterator();
    }

    public OO run(I input) throws ExecutionException, InterruptedException {
        Timed<OO> oldOutput = execAndTime(oldFlow).apply(input);
        threadPoolExecutor.execute(() -> verifyAndRecord(oldOutput, input));
        return oldOutput.getValue();
    }

    public void verifyAndRecord(Timed<OO> oldFlowOutput, I input) {
        if (intStream.next() < percentOfTimesToExecute) {
            Timed<NO> newFlowOutput = execAndTime(newFlow).apply(input);
            metricRegistry.timer(name + "oldFlow").update(oldFlowOutput.getTimeInNanos(), TimeUnit.NANOSECONDS);
            metricRegistry.timer(name + "newFlow").update(newFlowOutput.getTimeInNanos(), TimeUnit.NANOSECONDS);
            boolean isMismatch = newFlowOutput.getValue().equals(oldFlowOutput.getValue());
            String metricName = isMismatch ? "match" : "mismatch";
            metricRegistry.counter(name + metricName).inc();
            if (isMismatch)
                log.error(String.format("Mismatch:- Input: %s OldFlowOutput:%s NewFlowOutput:%s", input, oldFlowOutput.getValue(), newFlowOutput.getValue()));
        }
    }

    public Map<String, Metric> getAll() {
        return metricRegistry.getMetrics();
    }

    private <A, B> Function<A, Timed<B>> execAndTime(Function<A, B> fn) {
        return (input) -> {
            long startTime = System.nanoTime();
            B result = fn.apply(input);
            long timeTaken = System.nanoTime() - startTime;
            return new Timed<>(result, timeTaken);
        };
    }

    @Getter
    private class Timed<T> {
        private T value;
        private long timeInNanos;

        private Timed(T value, long timeInNanos) {
            this.value = value;
            this.timeInNanos = timeInNanos;
        }
    }
}
