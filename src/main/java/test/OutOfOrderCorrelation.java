package test;

import rx.Observable;
import rx.Scheduler;

import java.util.concurrent.TimeUnit;

import static rx.Observable.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class OutOfOrderCorrelation {
    public static void main(final String[] args) {
        new OutOfOrderCorrelation().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        ////////////////////////////////////////////////////

        class Trade {
            String id;

            public Trade(final String id) {
                this.id = id;
            }

            @Override
            public String toString() {
                return "Trade{'" + id + '\'' + '}';
            }
        }

        class Reconciliation {
            String id;

            public Reconciliation(final String id) {
                this.id = id;
            }

            @Override
            public String toString() {
                return "Recon{'" + id + '\'' + '}';
            }
        }

        class Result {
            final String id;
            final boolean gotTrade;
            final boolean gotReconcile;

            public Result(final String id, final boolean gotTrade, final boolean gotReconcile) {
                this.id = id;
                this.gotTrade = gotTrade;
                this.gotReconcile = gotReconcile;
            }

            public String getId() {
                return id;
            }

            @Override
            public String toString() {
                return "Result{'" + id + '\'' + ", trade=" + gotTrade + ", recon=" + gotReconcile + '}';
            }
        }

        ////////////////////////////////////////////////////

        startTest("correlate");
        final Observable<Trade> trades =
            stream(9, -1, i -> new Trade(i.toString()), 400, false)
                .subscribeOn(scheduler);

        final Observable<Reconciliation> reconciliations =
            stream(5, -1, i -> new Reconciliation(i.toString()), 100, false)
                .subscribeOn(scheduler);

        final Observable<Result> outcomes =
            trades
                .map(t -> new Result(t.id, true, false))
                .mergeWith(reconciliations.map(r -> new Result(r.id, false, true)))
                .groupBy(Result::getId)
                .flatMap(group -> {
                        final String id = group.getKey();
                        return group
                            .scan((r1, r2) -> new Result(id, r1.gotTrade || r2.gotTrade, r1.gotReconcile || r2.gotReconcile))
                            .skipWhile(r -> !r.gotTrade || !r.gotReconcile)
                            .take(1)
                            .timeout(500, TimeUnit.MILLISECONDS, just(new Result(id, false, false)), scheduler);
                    }
                );

        outcomes
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                    -> " + s));
    }
}

