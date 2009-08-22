/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package module

import com.google.inject.{Module => GuiceModule, Binder,
                          Singleton, Inject}
import com.google.inject.name.Names

import Predefs._
import utils.TimeUnit

class Module extends GuiceModule {
    val prefsResource = "/mywire.properties"

    val prefs = new Prefs (prefsResource)

    val monitoringInterval = prefs.getTimeUnit("jacore.monitoring.interval")

    val lowPriorityPoolThreads = prefs.getInt("jacore.pool.low.threads")
    val lowPriorityPoolLatencyLimit = prefs.getTimeUnit("jacore.pool.low.latency")
    val lowPriorityPoolExecutionLimit = prefs.getTimeUnit("jacore.pool.low.execution")

    val normalPriorityPoolThreads = prefs.getInt("jacore.pool.normal.threads")
    val normalPriorityPoolLatencyLimit = prefs.getTimeUnit("jacore.pool.normal.latency")
    val normalPriorityPoolExecutionLimit = prefs.getTimeUnit("jacore.pool.normal.execution")

    val hiPriorityPoolThreads = prefs.getInt("jacore.pool.hi.threads")
    val hiPriorityPoolLatencyLimit = prefs.getTimeUnit("jacore.pool.hi.latency")
    val hiPriorityPoolExecutionLimit = prefs.getTimeUnit("jacore.pool.hi.execution")

    val schedulerLatencyLimit = prefs.getTimeUnit("jacore.scheduler.latency")

    val daemonStatusJmxName = prefs.getString("jacore.status.jmx.name")
    val daemonStatusUpdateInterval = prefs.getTimeUnit("jacore.status.update.interval")
    val daemonStatusFile = prefs.getString("jacore.status.file")

    val fileReadBytesLimit = 1024*1024

    // -- tests

    require (daemonStatusUpdateInterval > monitoringInterval * 2,
             "daemonStatusUpdateInterval must be greater than 2*monitoringInterval")

    // - - - - - - - - - - - - Bindings - - - - - - - - - -

    override def configure (binder : Binder) = {
        binder.bind (classOf[Prefs])
              .toInstance (prefs)

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("jacore.file.buffer.limit"))
              .toInstance (fileReadBytesLimit)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.scheduler.latency"))
              .toInstance (schedulerLatencyLimit)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.monitoring.interval"))
              .toInstance (monitoringInterval)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.status.update.interval"))
              .toInstance (daemonStatusUpdateInterval)

        binder.bind (classOf[String])
              .annotatedWith (Names.named ("jacore.status.file"))
              .toInstance (daemonStatusFile)


        // Hi priority pool parameters

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("jacore.pool.hi.threads"))
              .toInstance (hiPriorityPoolThreads)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.hi.latency"))
              .toInstance (hiPriorityPoolLatencyLimit)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.hi.execution"))
              .toInstance (hiPriorityPoolExecutionLimit)


        // Normal priority pool parameters

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("jacore.pool.normal.threads"))
              .toInstance (normalPriorityPoolThreads)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.normal.latency"))
              .toInstance (normalPriorityPoolLatencyLimit)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.normal.execution"))
              .toInstance (normalPriorityPoolExecutionLimit)

        
        // Low priority pool parameters
        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("jacore.pool.low.threads"))
              .toInstance (lowPriorityPoolThreads)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.low.latency"))
              .toInstance (lowPriorityPoolLatencyLimit)

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("jacore.pool.low.execution"))
              .toInstance (lowPriorityPoolExecutionLimit)


        // Daemon

        binder.bind (classOf[String])
              .annotatedWith (Names.named("jacore.status.jmx.name"))
              .toInstance (prefs.getString ("jacore.status.jmx.name"))
    }
}