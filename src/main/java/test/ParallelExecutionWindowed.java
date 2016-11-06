package test;

import rx.schedulers.Schedulers;

import static rx.Observable.from;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;
import static test.TestUtil.syncTestOperation;

// https://gist.github.com/benjchristensen/a0350776a595fd6e3810
public class ParallelExecutionWindowed {

    public static void main(final String[] args) {
        flatMapWindowedExampleAsync();
        flatMapBufferedExampleAsync();
    }

    /**
     * If a single stream needs to be split across multiple CPUs it is generally more efficient to do it in batches.
     * <p>
     * The `buffer` operator can be used to batch into chunks that are then each processed on a separate thread.
     */
    private static void flatMapBufferedExampleAsync() {
        startTest("flatMapBufferedExampleAsync");
        stream(8)
            .buffer(3)
            .doOnNext(buffered -> output("        emit " + buffered))
            .flatMap(
                buffered ->
                    from(buffered)
                        .subscribeOn(Schedulers.computation())
                        .map(item -> syncTestOperation(item, "")),
                Runtime.getRuntime().availableProcessors()
            )
            .toBlocking()
            .forEach(s -> output("                                " + s));
    }

    /**
     * Or the `window` operator can be used instead of buffer to process them as a stream instead of buffered list.
     */
    private static void flatMapWindowedExampleAsync() {
        startTest("flatMapWindowedExampleAsync");
        stream(8)
            .window(3)
            .flatMap(
                work ->
                    work
                        .observeOn(Schedulers.computation())
                        .map(item -> syncTestOperation(item, "")),
                Runtime.getRuntime().availableProcessors()
            )
            .toBlocking()
            .forEach(s -> output("                                " + s));
    }

}



