package test;

import rx.Scheduler;

import static rx.schedulers.Schedulers.computation;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class ScatterGroupBy {
    public static void main(final String[] args) {
        new ScatterGroupBy().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = computation();    // io has more threads

        startTest("scatter groupby");
        stream(17)
            .groupBy(i -> i % 3)
            .flatMap(
                group -> {
                    output("        start group %s", group.getKey());
                    return
                        group.flatMap(
                            i ->
                                observeSyncTestOperation(i, "" + group.getKey() + ":")
                                    .subscribeOn(scheduler)
                        );
                }
            ).subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



