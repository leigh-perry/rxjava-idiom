package test;

import rx.Scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static rx.Observable.defer;
import static rx.Observable.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.pause;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class SmartBatched {
    public static void main(final String[] args) {
        new SmartBatched().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("smart-batched");
        stream(15)
            // batch up to 300 msec or max of 4
            .buffer(300, TimeUnit.MILLISECONDS, 4)
            //.doOnEach(batch -> output("            %s emitted", batch.getValue()))
            .filter(batch -> !batch.isEmpty())
            .flatMap(batch ->
                defer(() -> just(slowSyncOperation(batch)))
                    .subscribeOn(scheduler)
            )
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                        " + s));
    }

    private String slowSyncOperation(final List<Integer> batch) {
        final String prefix = batch.toString();
        output("            %s started sync", prefix);
        pause(2000);
        output("            %s done sync", prefix);
        return "result:" + prefix;
    }
}



