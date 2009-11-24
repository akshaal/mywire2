/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.domain

/**
 * Quality of service information.
 */
case class Qos (memoryUsedPercent : Double,
                schedulerLatencyNs : Long,
                hiPoolExecutionNs : Long,
                hiPoolLatencyNs : Long,
                normalPoolExecutionNs : Long,
                normalPoolLatencyNs : Long,
                lowPoolExecutionNs : Long,
                lowPoolLatencyNs : Long)
            extends ExportNotification
{
    override def toMap : Map[String, Any] = {
        super.toMap
             .updated ("memoryUsedPercent", memoryUsedPercent)
             .updated ("schedulerLatencyNs", schedulerLatencyNs)
             .updated ("hiPoolExecutionNs", hiPoolExecutionNs)
             .updated ("hiPoolLatencyNs", hiPoolLatencyNs)
             .updated ("lowPoolExecutionNs", lowPoolExecutionNs)
             .updated ("lowPoolLatencyNs", lowPoolLatencyNs)
             .updated ("normalPoolExecutionNs", normalPoolExecutionNs)
             .updated ("normalPoolLatencyNs", normalPoolLatencyNs)
    }
}
