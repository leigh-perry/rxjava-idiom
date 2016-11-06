package test;

import rx.Scheduler;

import java.util.Arrays;

import static rx.Observable.defer;
import static rx.Observable.empty;
import static rx.Observable.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;
import static test.TestUtil.syncTestOperation;

public class CancelSlowOnNew {
    public static void main(final String[] args) {
        new CancelSlowOnNew().runAll();
    }

    // TODO finish

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

//        startTest("cancel slow operation on new data - switchMap");
//        stream(8)
//            .switchMap(i -> {
//                output("switched:%s", i);
//                return defer(
//                    () -> {
//                        String value = syncTestOperation(i, "slow", 60);
//
//                        // Slow operation is finished now
//                        output("current %s", i);
//                        return just(value);
//                    }
//                ).subscribeOn(scheduler);
//            })
//            .subscribeOn(scheduler)
//            .toBlocking()
//            .subscribe(s -> output("                                " + s));


        startTest("cancel slow operation on new data - mutable hack");
        final Integer[] status = { null, null };  // latest requested, latest published
        stream(8)
            .doOnNext(i -> status[0] = i)
            .flatMap(i -> defer(
                () -> {
                    String value = syncTestOperation(i, "slow", 60);

                    // Slow operation is finished now
                    final boolean shouldPublish = status[1] == null || i.equals(status[0]);
                    if (shouldPublish) {
                        status[1] = i;
                        output("current %s %s", i, Arrays.toString(status));
                        return just(value);
                    } else {
                        output("obsolete %s %s", i, Arrays.toString(status));
                        return empty();
                    }
                }
            ).subscribeOn(scheduler))
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



