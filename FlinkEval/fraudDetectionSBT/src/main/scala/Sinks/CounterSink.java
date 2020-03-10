package Sinks;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import org.apache.flink.walkthrough.common.entity.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterSink implements SinkFunction<Counter> {
    private static final long serialVersionUID= 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CounterSink.class);

    @Override
    public void invoke(Counter value, Context context) {
        LOG.info(value.toString());
    }
}
