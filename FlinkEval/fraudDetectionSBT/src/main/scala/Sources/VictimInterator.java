package Sources;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class VictimInterator implements Iterator<Victim>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Timestamp INITIAL_TIMESTAMP = Timestamp.valueOf("2019-01-01 00:00:00");

    private static final long SIX_MINUTES = 6 * 60 * 1000;

    private final boolean bounded;

    private int index = 0;

    private long timestamp;

    static VictimInterator bounded() {
        return new VictimInterator(true);
    }

    static VictimInterator unbounded() {
        return new VictimInterator(false);
    }

    private VictimInterator(boolean bounded) {
        this.bounded = bounded;
        this.timestamp = INITIAL_TIMESTAMP.getTime();
    }

    @Override
    public boolean hasNext() {
        if (index < data.size()) {
            return true;
        } else if (!bounded) {
            index = 0;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Victim next() {
        Victim transaction = data.get(index++);
        transaction.setTimestamp(timestamp);
        timestamp += SIX_MINUTES;
        return transaction;
    }

    private static List<Victim> data = Arrays.asList(
            new Victim(1, 0L, 188.23),
            new Victim(2, 0L, 374.79),
            new Victim(3, 0L, 112.15),
            new Victim(4, 0L, 478.75),
            new Victim(5, 0L, 208.85),
            new Victim(1, 0L, 379.64),
            new Victim(2, 0L, 351.44),
            new Victim(3, 0L, 320.75),
            new Victim(4, 0L, 259.42),
            new Victim(5, 0L, 273.44),
            new Victim(1, 0L, 267.25),
            new Victim(2, 0L, 397.15),
            new Victim(4, 0L, 231.94),
            new Victim(5, 0L, 384.73),
            new Victim(1, 0L, 419.62),
            new Victim(2, 0L, 412.91),
            new Victim(3, 0L, 0.77),
            new Victim(4, 0L, 22.10),
            new Victim(5, 0L, 377.54),
            new Victim(1, 0L, 375.44),
            new Victim(2, 0L, 230.18),
            new Victim(3, 0L, 0.80),
            new Victim(4, 0L, 350.89),
            new Victim(5, 0L, 127.55),
            new Victim(1, 0L, 483.91),
            new Victim(2, 0L, 228.22),
            new Victim(3, 0L, 871.15),
            new Victim(4, 0L, 64.19),
            new Victim(5, 0L, 79.43),
            new Victim(1, 0L, 56.12),
            new Victim(2, 0L, 256.48),
            new Victim(3, 0L, 148.16),
            new Victim(4, 0L, 199.95),
            new Victim(5, 0L, 252.37),
            new Victim(1, 0L, 274.73),
            new Victim(2, 0L, 473.54),
            new Victim(3, 0L, 119.92),
            new Victim(4, 0L, 323.59),
            new Victim(5, 0L, 353.16),
            new Victim(1, 0L, 211.90),
            new Victim(2, 0L, 280.93),
            new Victim(3, 0L, 347.89),
            new Victim(4, 0L, 459.86),
            new Victim(5, 0L, 82.31),
            new Victim(1, 0L, 373.26),
            new Victim(2, 0L, 479.83),
            new Victim(3, 0L, 454.25),
            new Victim(4, 0L, 83.64),
            new Victim(5, 0L, 292.44));
}
