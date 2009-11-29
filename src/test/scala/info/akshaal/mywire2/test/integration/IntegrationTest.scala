    /*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package integration

import java.io.{File, PrintWriter}
import java.lang.management.ManagementFactory
import java.util.{HashMap => JavaHashMap}
import javax.management.ObjectName

import com.google.inject.{Guice, Binder, Inject, Singleton}

import org.apache.ibatis.session.{SqlSessionFactory, SqlSessionFactoryBuilder}
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.builder.xml.XMLMapperBuilder
import org.apache.ibatis.io.Resources
import org.apache.ibatis.parsing.XNode

import org.specs.SpecificationWithJUnit

import org.apache.activemq.broker.BrokerService
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.pool.PooledConnectionFactory
import org.apache.activemq.command.ActiveMQTopic

import javax.jms.{ConnectionFactory, Destination}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.daemon.DaemonStatus
import info.akshaal.jacore.actor.{Actor, LowPriorityActorEnv}
import info.akshaal.jacore.test.TestHelper
import info.akshaal.jacore.utils.Prefs

import daemon.{BaseDaemon, Autostart}
import module.Module
import annotation.{LogDB, JmsIntegrationExport}

class IntegrationTest extends SpecificationWithJUnit ("Integration specification") {
    import IntegrationTest._

    IntegrationTest

    "A daemon" should {
        "survive for some time without problems" in {
            val srv = ManagementFactory.getPlatformMBeanServer()
            val daemonObj = new ObjectName (IntegrationDaemon.jmxObjectName)
            
            val actor1 = injector.getInstanceOf [autostart.Actor1]
            val actor2 = injector.getInstanceOf [autostart.Actor2]
            val actor3 = injector.getInstanceOf [autostart.Actor3]
            val actor4 = injector.getInstanceOf [autostart.Actor4]
            val actor5 = injector.getInstanceOf [autostart.Actor5]

            try {
                actor1.started  must_==  0
                actor1.stopped  must_==  0

                actor2.started  must_==  0
                actor2.stopped  must_==  0

                actor3.started  must_==  0
                actor3.stopped  must_==  0

                actor4.started  must_==  0
                actor4.stopped  must_==  0

                actor5.started  must_==  0
                actor5.stopped  must_==  0

                IntegrationDaemon.init
                IntegrationDaemon.start

                Thread.sleep (10.seconds.asMilliseconds)

                val graph = srv.invoke (daemonObj, "graph", Array(), Array()).asInstanceOf [String]
                writeGraph (graph)
                graph  must find (".*(Actor1).*")
                graph  must find (".*(Actor2).*")
                graph  must find (".*(Actor3).*")
                graph  must find (".*(Actor4).*")
                graph  must find (".*(MywireManager).*")
                graph  must find (".*(DaemonStatus).*")

                actor1.started  must_==  1
                actor1.stopped  must_==  0

                actor2.started  must_==  1
                actor2.stopped  must_==  0

                actor3.started  must_==  1
                actor3.stopped  must_==  0

                actor4.started  must_==  1
                actor4.stopped  must_==  0

                actor5.started  must_==  0
                actor5.stopped  must_==  0

                Thread.sleep (10.seconds.asMilliseconds)

                IntegrationDaemon.daemonStatus.isDying         must beFalse
                IntegrationDaemon.daemonStatus.isShuttingDown  must beFalse
            } finally {
                IntegrationDaemon.stop
                IntegrationDaemon.destroy
            }

            actor1.started  must_==  1
            actor1.stopped  must_==  1

            actor2.started  must_==  1
            actor2.stopped  must_==  1

            actor3.started  must_==  1
            actor3.stopped  must_==  1

            actor4.started  must_==  1
            actor4.stopped  must_==  1

            actor5.started  must_==  0
            actor5.stopped  must_==  0
        }
    }

    def writeGraph (graph : String) : Unit = {
        val debugDir = System.getProperty ("jacore.module.debug.dir")
        if (debugDir != null) {
            new File (debugDir).mkdirs

            val filename = debugDir + "/" + "integration-module.dot"
            withCloseableIO (new PrintWriter (new File (filename), "UTF-8")) (out =>
                out.print (graph)
            )
        }
    }
}

object IntegrationTest extends TestHelper {
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

    val amqConnectionFactory = new ActiveMQConnectionFactory ("vm://localhost")

    override val timeout = 2.seconds
    override val injector = IntegrationDaemon.publicInjector

    object IntegrationDaemon extends BaseDaemon (IntegrationModule) {
        val daemonStatus = injector.getInstanceOf [DaemonStatus]
        val publicInjector = injector

        override lazy val jmxObjectName = "mywire:name=integrationTestStatus" + hashCode

        override protected val additionalActors : Seq [Actor] =
                    List (injector.getInstanceOf[autostart.Actor3])

        override protected val additionalActorClasses : Seq [Class [_ <: Actor]] =
                    List (classOf [autostart.Actor4])

        override protected def additionalAutostartActorPackages : Seq [String] =
                    List ("info.akshaal.mywire2.test.integration.autostart")
    }

    object IntegrationModule extends Module {
        val daemonStatusFileFile = File.createTempFile ("Mywire2", "IntegrationTest")
        daemonStatusFileFile.deleteOnExit

        override lazy val monitoringInterval = 4.seconds
        
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
        override lazy val daemonStatusUpdateInterval = 9.seconds
        override lazy val daemonStatusFile = daemonStatusFileFile.getAbsolutePath

        override lazy val qosInterval = 1 seconds

        override def configure (binder : Binder) = {
            super.configure (binder)

            // sqlmap
            val dataSourcePrefs = new Prefs ("jdbc.properties")
            val dataSourceFactory = new PooledDataSourceFactory
            dataSourceFactory.setProperties (dataSourcePrefs.properties)
            val dataSource = dataSourceFactory.getDataSource

            val transactionFactory = new JdbcTransactionFactory
            val sqlEnvironment = new Environment ("development", transactionFactory, dataSource)
            val configuration = new Configuration (sqlEnvironment)

            val sqlFragments = new JavaHashMap [String, XNode]
            val reader = Resources.getResourceAsReader ("info/akshaal/mywire2/sqlmaps/log.xml")
            val mapperBuilder = new XMLMapperBuilder (reader, configuration, "123", sqlFragments)
            mapperBuilder.parse ()

            val sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder
            val sqlSessionFactory = sqlSessionFactoryBuilder.build (configuration)

            binder.bind (classOf[SqlSessionFactory]).annotatedWith (classOf[LogDB])
                  .toInstance (sqlSessionFactory)

            // JMS
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

package autostart {
    class ActorBase (actorEnv : LowPriorityActorEnv) extends Actor (actorEnv = actorEnv)
    {
        var started = 0
        var stopped = 0

        override def start () = {
            super.start ()

            started += 1
        }

        override def stop () = {
            super.stop ()

            stopped += 1
        }
    }

    @Singleton
    class Actor1 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
                                                            with Autostart

    @Singleton
    class Actor2 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
                                                            with Autostart

    @Singleton
    class Actor3 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)

    @Singleton
    class Actor4 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)

    @Singleton
    class Actor5 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
}