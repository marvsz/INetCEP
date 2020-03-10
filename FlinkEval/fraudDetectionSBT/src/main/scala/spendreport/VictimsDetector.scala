package spendreport

import Sinks.Counter
import Sources.Victim
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

@SerialVersionUID(1L)
class VictimsDetector extends KeyedProcessFunction[Long, Victim, Counter]{

  @throws[Exception]
  def processElement(
                    victim: Victim,
                    context: KeyedProcessFunction[Long, Victim, Counter]#Context,
                    collector: Collector[Counter]): Unit = {

    val alert = new Counter
    alert.setId(victim.getAccountId)
    collector.collect(alert)
  }
}
