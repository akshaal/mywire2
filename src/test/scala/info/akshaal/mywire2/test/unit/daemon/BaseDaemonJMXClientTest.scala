/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.daemon

import java.lang.management.ManagementFactory

import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.jmx.{JmxAttr, JmxOper, SimpleJmx}

import module.Module
import daemon.BaseDaemonJMXClient

class BaseDaemonJMXClientTest extends SpecificationWithJUnit ("Daemon JMX client specification") {
    import BaseDaemonJMXClientTest._

    "BaseDaemonJMXClient" should {
        "connect to daemon by jmx and restart it waiting for result" in {
            FakeDaemon.versionAsked   must_==  0
            FakeDaemon.restartCalled  must_==  0

            val client = new BaseDaemonJMXClient (TestModule) {
                override protected val daemonUrl : String = ""
                override protected val jmxUser : String = ""
                override protected val jmxPassword : String = ""
                override def getServer () = ManagementFactory.getPlatformMBeanServer()
            }

            client.run ()

            FakeDaemon.unregisterJmxBean

            FakeDaemon.versionAsked   must_==  3
            FakeDaemon.restartCalled  must_==  1
        }

        "not restart if versions are the same" in {
            FakeDaemon2.versionAsked   must_==  0
            FakeDaemon2.restartCalled  must_==  0

            val client = new BaseDaemonJMXClient (TestModule2) {
                override protected val daemonUrl : String = ""
                override protected val jmxUser : String = ""
                override protected val jmxPassword : String = ""
                override def getServer () = ManagementFactory.getPlatformMBeanServer()
            }

            client.run ()

            FakeDaemon2.unregisterJmxBean

            FakeDaemon2.versionAsked   must_==  1
            FakeDaemon2.restartCalled  must_==  0
        }

        "not restart if new version is a snapshot" in {
            FakeDaemon3.versionAsked   must_==  0
            FakeDaemon3.restartCalled  must_==  0

            val client = new BaseDaemonJMXClient (TestModule3) {
                override protected val daemonUrl : String = ""
                override protected val jmxUser : String = ""
                override protected val jmxPassword : String = ""
                override def getServer () = ManagementFactory.getPlatformMBeanServer()
            }

            client.run ()

            FakeDaemon3.unregisterJmxBean

            FakeDaemon3.versionAsked   must_==  1
            FakeDaemon3.restartCalled  must_==  0
        }
    }
}

object BaseDaemonJMXClientTest {
    object TestModule extends Module {
        override lazy val daemonJmxName = "mywire:name=jmxClientTestDaemon" + hashCode
        override lazy val version = "newVersion"
    }
    
    object FakeDaemon extends SimpleJmx {
        var versionAsked = 0
        var restartCalled = 0

        override lazy val jmxObjectName = TestModule.daemonJmxName

        override lazy val jmxAttributes = List (
            JmxAttr ("version",
                     Some (() => {
                            versionAsked += 1

                            if (versionAsked == 3 && restartCalled == 1) {
                                TestModule.version
                            } else {
                                "strangeVersion"
                            }
                        }),
                     None)
        )

        override lazy val jmxOperations = List (
            JmxOper ("restart", () => restartCalled += 1)
        )
    }

    object TestModule2 extends Module {
        override lazy val daemonJmxName = "mywire:name=jmxClientTestDaemon" + hashCode
        override lazy val version = "strangeVersion"
    }

    object FakeDaemon2 extends SimpleJmx {
        var versionAsked = 0
        var restartCalled = 0

        override lazy val jmxObjectName = TestModule2.daemonJmxName

        override lazy val jmxAttributes = List (
            JmxAttr ("version", Some (() => {versionAsked += 1; TestModule2.version}), None)
        )

        override lazy val jmxOperations = List (
            JmxOper ("restart", () => restartCalled += 1)
        )
    }

    object TestModule3 extends Module {
        override lazy val daemonJmxName = "mywire:name=jmxClientTestDaemon" + hashCode
        override lazy val version = "strangeVersion-SNAPSHOT"
    }

    object FakeDaemon3 extends SimpleJmx {
        var versionAsked = 0
        var restartCalled = 0

        override lazy val jmxObjectName = TestModule3.daemonJmxName

        override lazy val jmxAttributes = List (
            JmxAttr ("version", Some (() => {versionAsked += 1; "oldStrangeVersion"}), None)
        )

        override lazy val jmxOperations = List (
            JmxOper ("restart", () => restartCalled += 1)
        )
    }
}