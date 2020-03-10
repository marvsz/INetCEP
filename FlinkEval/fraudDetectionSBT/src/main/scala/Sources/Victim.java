package Sources;

import org.apache.flink.walkthrough.common.entity.Transaction;

import java.util.Objects;

public class Victim {

    private long accountId;

    private long timestamp;

    private double amount;

    public Victim() { }

    public Victim(long accountId, long timestamp, double amount) {
        this.accountId = accountId;
        this.timestamp = timestamp;
        this.amount = amount;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Victim that = (Victim) o;
        return accountId == that.accountId &&
                timestamp == that.timestamp &&
                Double.compare(that.amount, amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, timestamp, amount);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "accountId=" + accountId +
                ", timestamp=" + timestamp +
                ", amount=" + amount +
                '}';
    }
}
