package test;

import rx.Scheduler;

import static rx.Observable.just;
import static rx.schedulers.Schedulers.computation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;

public class RequestResponse {
    public static void main(final String[] args) {
        new RequestResponse().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = computation();    // io has more threads

        startTest("request-response");
        just(53)
            .flatMap(i ->
                TestUtil.observeSyncTestOperation(i, "req")
                    .subscribeOn(scheduler)
            )
            .toBlocking()
            .subscribe(s -> output("            " + s));
    }
}



