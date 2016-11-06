package test;

import rx.Scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static rx.Observable.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.observeSynchronous;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.syncTestOperation;

public class AbandonSlow {
    public static void main(final String[] args) {
        new AbandonSlow().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("request alternative on timeout");
        observeSyncTestOperation(1, "1st")
            .subscribeOn(scheduler)
            .timeout(500, TimeUnit.MILLISECONDS)
            .onErrorResumeNext(exception -> {
                output("timeout...trying alternative");
                if (exception instanceof TimeoutException) {
                    return observeSynchronous(i -> syncTestOperation(i, "2nd", 300), 2);    // quick one
                } else {
                    throw new RuntimeException(exception);
                }
            })
            .onExceptionResumeNext(just("default-value"))   //
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



