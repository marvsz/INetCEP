package Sources;

import org.apache.flink.streaming.api.functions.source.FromIteratorFunction;

import java.io.Serializable;
import java.util.Iterator;

public class VictimSource extends FromIteratorFunction<Victim> {

    private static final long serialVersionUID = 1L;

    public VictimSource() {
        super(new VictimSource.RateLimitedIterator<>(VictimInterator.unbounded()));
    }

    private static class RateLimitedIterator<T> implements Iterator<T>, Serializable {

        private static final long serialVersionUID = 1L;

        private final Iterator<T> inner;

        private RateLimitedIterator(Iterator<T> inner) {
            this.inner = inner;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return inner.next();
        }
    }
}
