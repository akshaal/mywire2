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
import javax.management.ObjectName

import org.apache.ibatis.session.SqlSessionFactory

import org.specs.SpecificationWithJUnit

import org.apache.activemq.broker.BrokerService
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.pool.PooledConnectionFactory
import org.apache.activemq.command.ActiveMQTopic

import javax.jms.{Connection, Destination}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.daemon.DaemonStatus
import info.akshaal.jacore.actor.{Actor, LowPriorityActorEnv, HiPriorityActorEnv}
import info.akshaal.jacore.test.TestHelper
import info.akshaal.jacore.utils.IbatisUtils._
import info.akshaal.jacore.utils.Prefs

import daemon.{BaseDaemon, Autostart, Autoregister}
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
            val actor4 = injector.getInstanceOf [autostart.Actor4]
            val actor5 = injector.getInstanceOf [autostart.Actor5]

            actor1.started  must_==  0
            actor1.stopped  must_==  0

            actor2.started  must_==  0
            actor2.stopped  must_==  0

            actor4.started  must_==  0
            actor4.stopped  must_==  0

            actor5.started  must_==  0
            actor5.stopped  must_==  0

            autostart.obj.ActorObject.started  must_==  0
            autostart.obj.ActorObject.stopped  must_==  0

            IntegrationDaemon.init
            IntegrationDaemon.start

            Thread.sleep (10.seconds.asMilliseconds)

            val graph = srv.invoke (daemonObj, "graph", Array(), Array()).asInstanceOf [String]
            writeGraph (graph)
            graph  must find (".*(Actor1).*")
            graph  must find (".*(Actor2).*")
            graph  must find (".*(Actor3).*")
            graph  must find (".*(Actor4).*")
            graph  must find (".*(actorObject).*")
            graph  must find (".*(MywireManager).*")
            graph  must find (".*(DaemonStatus).*")

            srv.getAttribute (daemonObj, "version")  must_==  version

            actor1.started  must_==  1
            actor1.stopped  must_==  0

            actor2.started  must_==  1
            actor2.stopped  must_==  0

            actor4.started  must_==  1
            actor4.stopped  must_==  0

            actor5.started  must_==  0
            actor5.stopped  must_==  0

            autostart.obj.ActorObject.started  must_==  1
            autostart.obj.ActorObject.stopped  must_==  0

            Thread.sleep (10.seconds.asMilliseconds)

            daemonStatus.isDying         must beFalse
            daemonStatus.isShuttingDown  must beFalse

            IntegrationDaemon.stop
            IntegrationDaemon.destroy

            actor1.started  must_==  1
            actor1.stopped  must_==  1

            actor2.started  must_==  1
            actor2.stopped  must_==  1

            actor4.started  must_==  1
            actor4.stopped  must_==  1

            actor5.started  must_==  0
            actor5.stopped  must_==  0

            autostart.obj.ActorObject.started  must_==  1
            autostart.obj.ActorObject.stopped  must_==  1
        }
    }

    def writeGraph (graph : String) : Unit = {
        val debugDir = System.getProperty ("jacore.module.debug.dir")
        if (debugDir != null) {
            new File (debugDir).mkdirs

            val filename = debugDir + "/" + "integration-module.dot"
            withCloseableIO (new PrintWriter (new File (filename), "UTF-8")) (_.print (graph))
        }
    }
}

object IntegrationTest extends TestHelper {
    val prefs = new Prefs ("mywire.properties")
    val version = prefs.getString("version")

    // Prepare pid
    val pidFileFile = File.createTempFile ("Mywire2pid", "IntegrationTest")
    val pidFile = pidFileFile.getAbsolutePath
    pidFileFile.deleteOnExit

    withCloseableIO (new PrintWriter (new File (pidFile), "latin1")) (_.print ("12333451"))

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
    override val injector = IntegrationDaemon.publicBasicInjector
    val daemonStatus = injector.getInstanceOf [DaemonStatus]

    object IntegrationDaemon extends BaseDaemon (
                    module = IntegrationModule,
                    pidFile = pidFile,
                    additionalActorClasses = List (classOf [autostart.Actor4]),
                    additionalAutostartActorPackages = List ("info.akshaal.mywire2.test.integration.autostart"))
    {
        def publicBasicInjector = basicInjector

        override lazy val jmxObjectName = "mywire:name=integrationDaemon" + hashCode
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
        override lazy val daemonJmxName = "mywire:name=integrationTestDaemon" + hashCode
        override lazy val daemonStatusUpdateInterval = 9.seconds
        override lazy val daemonStatusFile = daemonStatusFileFile.getAbsolutePath

        override lazy val qosInterval = 1 seconds

        override def configure () = {
            super.configure ()

            // sqlmap
            val dataSource = createPooledDataSource ("jdbc.properties")
            val sqlConfig = createJdbcConfiguration ("integration", dataSource)
            sqlConfig.parseMapperXmlsInPackages ("info.akshaal.mywire2.sqlmaps")

            val sqlSessionFactory = createSqlSessionFactory (sqlConfig)

            bind (classOf[SqlSessionFactory]).annotatedWith (classOf [LogDB])
                  .toInstance (sqlSessionFactory)

            // JMS
            val connectionFactory = new PooledConnectionFactory (amqConnectionFactory)
            val connection = connectionFactory.createConnection
            connection.start

            bind (classOf[Connection])
                  .annotatedWith (classOf[JmsIntegrationExport])
                  .toInstance (connection)

            val exportTopic = new ActiveMQTopic ("integration-test-export")
            bind (classOf[Destination])
                  .annotatedWith (classOf[JmsIntegrationExport])
                  .toInstance (exportTopic)
        }
    }
}

package autostart {
    trait ActorStartStopCounting extends Actor {
        this : Actor =>

        var started = 0
        var stopped = 0

        override def start () = {
            val s = super.start ()

            started += 1

            s
        }

        override def stop () = {
            val s = super.stop ()

            stopped += 1

            s
        }
    }

    package obj {
        class ActorBase 
                extends Actor (actorEnv = IntegrationTest.injector
                                                         .getInstanceOf [HiPriorityActorEnv])
                   with ActorStartStopCounting

        object ActorObject extends ActorBase with Autostart with Autoregister {
            def registrationName = "actorObject"
        }
    }

    class ActorBase (actorEnv : LowPriorityActorEnv) extends Actor (actorEnv = actorEnv)
                                                        with ActorStartStopCounting

    @Singleton
    class Actor1 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
                                                            with Autostart

    @Singleton
    class Actor2 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
                                                            with Autostart

    @Singleton
    class Actor3 @Inject() (actorEnv : LowPriorityActorEnv,
                            @Named ("actorObject") ob : obj.ActorBase)
                    extends ActorBase (actorEnv = actorEnv)
                       with Autostart

    @Singleton
    class Actor4 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)

    @Singleton
    class Actor5 @Inject() (actorEnv : LowPriorityActorEnv) extends ActorBase (actorEnv = actorEnv)
}
