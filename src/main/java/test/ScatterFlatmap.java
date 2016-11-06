package test;

import rx.Scheduler;

import static rx.schedulers.Schedulers.computation;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class ScatterFlatmap {
    public static void main(final String[] args) {
        new ScatterFlatmap().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = computation();    // io has more threads

        startTest("scatter flatmap");
        stream(15)
            .flatMap(i ->
                observeSyncTestOperation(i, "")
                    .subscribeOn(scheduler)
            )
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



