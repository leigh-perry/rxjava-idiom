package test;

import rx.Observable;
import rx.Scheduler;

import java.util.concurrent.TimeUnit;

import static rx.Observable.defer;
import static rx.Observable.just;
import static rx.Observable.range;
import static rx.Observable.timer;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.DELIBERATE_EXCEPTION;
import static test.TestUtil.output;
import static test.TestUtil.pause;

public class ErrorBackoff {
    public static void main(final String[] args) {
        new ErrorBackoff().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads
        Integer result;

        startTest("error backoff explicit timings");
        result =
            observeSyncOperation(3)
                .retryWhen(exceptions ->
                    exceptions
                        .zipWith(just(400, 600, 800, 1500), (exception, backoffMsec) -> backoffMsec)
                        .flatMap(backoffMsec -> {
                                output("delay %s", backoffMsec);
                                return timer(backoffMsec, TimeUnit.MILLISECONDS);
                            }
                        )
                )
                .subscribeOn(scheduler)
                .toBlocking()
                .singleOrDefault(999);
        output("result: %s", result);

        startTest("error backoff constant");
        result =
            observeSyncOperation(Integer.MAX_VALUE)
                .retryWhen(exceptions ->
                    exceptions
                        .take(2)    // limit to 3 tries (2 retries)
                        .delay(200, TimeUnit.MILLISECONDS)
                )
                .subscribeOn(scheduler)
                .toBlocking()
                .singleOrDefault(-1);
        output("result: %s", result);

        startTest("error backoff linear");
        result =
            observeSyncOperation(Integer.MAX_VALUE)
                .retryWhen(exceptions ->
                    exceptions
                        .zipWith(range(1, 3), (exception, index) -> index)
                        .flatMap(index -> {
                                int backoffMsec = index * 400;
                                output("delay %s", backoffMsec);
                                return timer(backoffMsec, TimeUnit.MILLISECONDS);
                            }
                        )
                )
                .subscribeOn(scheduler)
                .toBlocking()
                .singleOrDefault(-1);
        output("result: %s", result);
    }

    private void startTest(final String name) {
        TestUtil.startTest(name);
        startCounting();
    }

    private volatile int count = 0;

    private void startCounting() {
        count = 0;
    }

    private Observable<Integer> observeSyncOperation(final int succeedAfter) {
        return defer(() -> {
            if (count++ == succeedAfter) {
                output("started sync operation");
                pause(1000);
                output("        done sync operation");
                return just(1234);
            } else {
                output("started sync operation");
                pause(300);
                output("%s", DELIBERATE_EXCEPTION);
                throw (new RuntimeException(DELIBERATE_EXCEPTION));
            }
        });
    }
}



