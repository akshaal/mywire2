/*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package integration

import java.io.File
import com.google.inject.{Guice, Binder}
import com.ibatis.sqlmap.client.{SqlMapClient, SqlMapClientBuilder}
import com.ibatis.common.resources.Resources
import org.specs.SpecificationWithJUnit

import org.apache.activemq.broker.BrokerService
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.pool.PooledConnectionFactory
import org.apache.activemq.command.ActiveMQTopic

import javax.jms.{ConnectionFactory, Destination}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.system.daemon.DaemonStatus
import info.akshaal.jacore.system.test.TestHelper

import system.module.Module
import system.annotation.{LogDB, JmsIntegrationExport}
import system.MywireManager

class IntegrationTest extends SpecificationWithJUnit ("Integration specification") {
    import IntegrationTest._

    IntegrationTest

    "Mywire" should {
        "survive for some time without problems" in {
            try {
                IntegrationModule.mywireManager.start

                Thread.sleep (15.seconds.asMilliseconds)

                IntegrationModule.daemonStatus.isDying         must beFalse
                IntegrationModule.daemonStatus.isShuttingDown  must beFalse
            } finally {
                IntegrationModule.mywireManager.stop
            }

            createModuleGraphInDebugDir ("integration-module.dot")
        }
    }
}

object IntegrationTest extends TestHelper {
    override val timeout = 2.seconds
    override val injector = IntegrationModule.injector

    // Prepare AMQ broker
    val mywireTestAmqDir = System.getProperty ("mywire.test.amq.dir")
    val amqDir =
            if (mywireTestAmqDir == null)
                (  System.getProperty ("java.io.tmpdir")
                 + "/mywireIntegrationTestDir-"
                 + System.getProperty ("user.name"))
            else
                mywireTestAmqDir

    val broker = new BrokerService
    broker.addConnector ("vm://localhost")
    broker.setDataDirectory (amqDir)
    broker.start

    object IntegrationModule extends Module {
        val daemonStatusFileFile = File.createTempFile ("Mywire2", "IntegrationTest")
        daemonStatusFileFile.deleteOnExit

        override lazy val monitoringInterval = 2.seconds

        override lazy val lowPriorityPoolThreads = 2
        override lazy val lowPriorityPoolLatencyLimit = 1.seconds
        override lazy val lowPriorityPoolExecutionLimit = 500.milliseconds

        override lazy val normalPriorityPoolThreads = 2
        override lazy val normalPriorityPoolLatencyLimit = 100.milliseconds
        override lazy val normalPriorityPoolExecutionLimit = 10.milliseconds

        override lazy val hiPriorityPoolThreads = 1
        override lazy val hiPriorityPoolLatencyLimit = 1.milliseconds
        override lazy val hiPriorityPoolExecutionLimit = 500.microseconds

        override lazy val schedulerLatencyLimit = 4.milliseconds

        override lazy val daemonStatusJmxName = "mywire:name=integrationTestStatus" + hashCode
        override lazy val daemonStatusUpdateInterval = 5.seconds
        override lazy val daemonStatusFile = daemonStatusFileFile.getAbsolutePath

        override lazy val qosInterval = 1 seconds

        val injector = Guice.createInjector (this)
        val mywireManager = injector.getInstanceOf [MywireManager]
        val daemonStatus = injector.getInstanceOf [DaemonStatus]

        override def configure (binder : Binder) = {
            super.configure (binder)

            // sqlmap
            val reader = Resources.getResourceAsReader ("sqlmap.xml")
            val sqlmap = SqlMapClientBuilder.buildSqlMapClient (reader)
            binder bind classOf[SqlMapClient] annotatedWith (classOf[LogDB]) toInstance sqlmap

            // JMS
            val amqConnectionFactory = new ActiveMQConnectionFactory ("vm://localhost")
            val connectionFactory = new PooledConnectionFactory (amqConnectionFactory)
            binder.bind (classOf[ConnectionFactory])
                  .annotatedWith (classOf[JmsIntegrationExport])
                  .toInstance (connectionFactory)

            val exportTopic = new ActiveMQTopic ("integration-test-export")
            binder.bind (classOf[Destination])
                  .annotatedWith (classOf[JmsIntegrationExport])
                  .toInstance (exportTopic)
        }
    }
}
