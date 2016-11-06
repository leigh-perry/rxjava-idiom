package test;

import rx.Observable;
import rx.Scheduler;

import static rx.Observable.defer;
import static rx.Observable.just;
import static rx.Observable.switchOnNext;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class ConnectionSwitch {
    public static void main(final String[] args) {
        new ConnectionSwitch().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        //interval(30, DAYS).map(i -> "Hi...").subscribe(this::send);

        // TODO finish

        final Observable<String> connections = stream(3, "conn");
        final Observable<Observable<Double>> search =
            connections
                .subscribeOn(scheduler)
                .map(this::connect);

        startTest("switch");
        switchOnNext(search)
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                    -> " + s));
    }

    private Observable<Double> connect(final String connectionName) {
        return defer(() -> just(Double.valueOf(connectionName.substring(4))));
    }
}


