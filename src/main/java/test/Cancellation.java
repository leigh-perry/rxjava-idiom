package test;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class Cancellation {
    public static void main(final String[] args) {
        new Cancellation().runAll();
    }

    private void runAll() {

    }

    // .NET cancellation handling

    // http://joeduffyblog.com/2015/11/19/asynchronous-everything/

    //    Cancellation
    //
    //    The need to have cancellable work isn?t anything new. I came up with the CancellationToken
    //    abstraction in .NET, largely in response to some of the challenges we had around ambient authority
    //    with prior ?implicitly scoped? attempts.
    //
    //    The difference in Midori was the scale. Asynchronous work was everywhere. It sprawled out across
    //    processes and, sometimes, even machines. It was incredibly difficult to chase down run-away work. My
    //    simple use-case was how to implement the browser?s ?cancel? button reliably. Simply rendering a
    //    webpage involved a handful of the browser?s own processes, plus the various networking processes ?
    //    including the NIC?s device driver ? along with the UI stack, and more. Having the ability to
    //    instantly and reliably cancel all of this work was not just appealing, it was required.
    //
    //    The solution ended up building atop the foundation of CancellationToken.
    //
    //    They key innovation was first to rebuild the idea of CancellationToken on top of our overall message
    //    passing model, and then to weave it throughout in all the right places. For example
    //    CancellationTokens could extend their reach across processes. Whole async objects could be wrapped
    //    in a CancellationToken, and used to trigger revocation. Whole async functions could be invoked with
    //    a CancellationToken, such that cancelling propagated downward. Areas like storage needed to manually
    //    check to ensure that state was kept consistent. In summary, we took a ?whole system? approach to the
    //    way cancellation was plumbed throughout the system, including extending the reach of cancellation
    //    across processes. I was happy with where we landed on this one.


    // check out
    //      .net source
    //          http://referencesource.microsoft.com/#mscorlib/system/threading/CancellationTokenSource.cs

    //      port:
    //          https://github.com/BoltsFramework/Bolts-Android/tree/master/bolts-tasks/src/main/java/bolts

    static class EventHandler {
        public void register(final BiConsumer<Object, Object> cancelHandler) {
        }
    }

    /**
     * represents a ?potential request for cancellation?. This struct is passed into
     * method calls as a parameter and the method can poll on it or register a callback
     * to be fired when cancellation is requested
     */
    static class CancellationToken {
        private boolean isCancellationRequested;
        private final CountDownLatch countDownLatch = new CountDownLatch(1);

        public boolean isCancellationRequested() {
            return isCancellationRequested;
        }

        public void setCancellationRequested(final boolean cancellationRequested) {
            isCancellationRequested = cancellationRequested;
        }

        public CancellationTokenRegistration register(final Runnable o) {
            // TODO add to list
            return new CancellationTokenRegistration();
        }

        public void deregister(final CancellationTokenRegistration cancellationTokenRegistration) {
            // TODO remove
        }

        // TODO CancellationToken.WaitHandle is lazily-allocated
        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }
    }

    /**
     * provides the mechanism for initiating a cancellation request and it has a Token
     * property for obtaining an associated token. It would have been natural to combine
     * these two classes into one, but this design allows the two key operations (initiating
     * a cancellation request vs. observing and responding to cancellation) to be cleanly
     * separated. In particular, methods that take only a CancellationToken can observe
     * a cancellation request but cannot initiate one
     */
    static class CancellationTokenSource {
        private final CancellationToken token = new CancellationToken();

        public void cancel() {
            token.setCancellationRequested(true);
        }

        public CancellationToken getToken() {
            return token;
        }
    }

    static class OperationCanceledException extends RuntimeException {
        private final CancellationToken token;

        public OperationCanceledException(final CancellationToken token) {
            this.token = token;
        }
    }

    static class CancellationTokenRegistration implements AutoCloseable {
        private CancellationToken token;

        @Override
        public void close() {
            token.deregister(this);
        }
    }

    EventHandler externalEvent;

    void exampleSync() {
        final CancellationTokenSource cts = new CancellationTokenSource();

        // wire up an external requester
        externalEvent.register((sender, obj) -> cts.cancel());

        try {
            final int val = longRunningFunc(cts.getToken());
        } catch (final OperationCanceledException e) {
            //cleanup after cancellation if required...
        }
    }

    private int longRunningFunc(final CancellationToken token) {
        int total = 0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                total++;
            }
            if (token.isCancellationRequested()) { // observe cancellation
                throw new OperationCanceledException(token); // acknowledge cancellation
            }
        }

        return total;
    }

    void exampleAsync(final CancellationToken token) {
        final CountDownLatch latch = new CountDownLatch(1);

        //register a callback that will set the MRE, close deregisters
        try (CancellationTokenRegistration registration = token.register(latch::countDown)) {
            latch.await();
            if (token.isCancellationRequested()) //did cancellation wake us?
            {
                throw new OperationCanceledException(token);
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void waitOrBeCancelled(final CountDownLatch wh, final CancellationToken token) {
        someLibWaitAny(wh, token.getCountDownLatch());

        if (token.isCancellationRequested()) //did cancellation wake us?
        {
            throw new OperationCanceledException(token);
        }
    }

    // running multiple asynchronous operations which share a common CancellationToken

    void sharingCancellationToken() {
        final CancellationTokenSource cts = new CancellationTokenSource();
        startAsyncFunc1(cts.getToken());
        startAsyncFunc2(cts.getToken());
        startAsyncFunc3(cts.getToken());
        //...
        cts.cancel();    // all listeners see the same cancellation request.
    }

    private void startAsyncFunc3(final CancellationToken token) {
    }

    private void startAsyncFunc2(final CancellationToken token) {
    }

    private void startAsyncFunc1(final CancellationToken token) {
    }

    private void someLibWaitAny(final CountDownLatch wh, final CountDownLatch countDownLatch) {
    }

    private void callbackCancellation() {
        final int[] data = { 1, 2, 3 };
        final CancellationTokenSource cts = new CancellationTokenSource();

        final Stream<Integer> query =
            Stream.of(1, 2, 3)
                //.withCancellation(cts.getToken()) // token given to library code
                .map(x -> slowFunc(x, cts.getToken())); // token passed to user code
    }

    private int slowFunc(final int x, final CancellationToken token) {
        // both observing the same token
        final int result = 2;
//        while (result > 0) {
//            if (token.isCancellationRequested()) {
//                throw new OperationCanceledException(token);
//            }
//
//            //
//        }

        return result;
    }
}


