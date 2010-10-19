/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package daemon

import java.util.concurrent.Future

import com.google.inject.{Guice, Injector}
import com.google.inject.AbstractModule
import com.google.inject.name.Names

import scala.collection.mutable.{Set, HashSet}

import info.akshaal.daemonhelper.{OSException, DaemonHelper}

import module.Module

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Actor
import info.akshaal.jacore.utils.GuiceUtils
import info.akshaal.jacore.utils.io.ClassUtils
import info.akshaal.jacore.logger.Logging
import info.akshaal.jacore.jmx.{SimpleJmx, JmxOper, JmxAttr}

/**
 * Abstract daemon.
 *
 * @param module Instantance of module that will is used for injector.
 * @param pidFile path to pid file of this daemon
 * @param additionalActorClasses instance of these class will be created using reflections or guice.
 * @param additionalAutostartActorPackages A number of packages that is supposed
 *        to be scanned to find actors that implement Autostart trait. These actors will
 *        be started automatically.
 */
abstract class BaseDaemon (module : Module,
                           pidFile : String,
                           additionalActorClasses : Seq [Class [_ <: Actor]] = Seq (),
                           additionalAutostartActorPackages : Seq [String] = Seq ())
                  extends Logging
                     with SimpleJmx
{
    override lazy val jmxObjectName = module.daemonJmxName

    /**
     * Main pid of the daemon.
     */
    private[this] val pid = readFileLinesAsString (pidFile, "latin1").trim.toInt

    // Some stuff
    infoLazy ("Loading daemon: main pid=" + pid + ", version=" + module.version)

    // Scan for all additional actor classes
    private[this] val allAdditionalActorClassesFuture : Future [Set [Class[_ <: Actor]]] =
        DaemonBoot.executor.submit {
            val all = new HashSet [Class[_ <: Actor]]
            all ++= additionalActorClasses

            for (pkg <- additionalAutostartActorPackages) {
                val classes =
                        ClassUtils.findClasses (pkg,
                                                Thread.currentThread.getContextClassLoader,
                                                classOf [Autostart].isAssignableFrom (_))

                all ++= classes.asInstanceOf [List [Class [Actor]]]
            }
            
            all
        }

    private[this] def allAdditionalActorClasses : Set [Class[_ <: Actor]] =
                            allAdditionalActorClassesFuture.get

    /**
     * Injector that holds basic stuff (mywire actors). This injector is created first.
     * Using this injector we can construct other object depending on basic actors
     * and place them into the final injector.
     */
    protected final val basicInjector = Guice.createInjector (module)

    /**
     * All actors that are to be started/stopped automaticcaly. Populated in init method.
     */
    private[this] final val allAdditionalActors : Set[Actor] = new HashSet

    /**
     * A set of classes that are instantiated using full-blown injector.
     */
    private[this] final val actorClassesForGuice : Set [Class [_ <: Actor]] = new HashSet

    /**
     * MywireManager instance of the application.
     */
    protected final val mywireManager = basicInjector.getInstanceOf [MywireManager]

    /**
     * Operations exposed through jmx.
     */
    protected override lazy val jmxOperations =
            List (JmxOper ("graph", createGraph),
                  JmxOper ("restart", restart))

    /**
     * List of exposed JMX attributes.
     */
    override protected lazy val jmxAttributes = List (
        JmxAttr ("version",    Some (() => module.version),   None)
    )
    
    /**
     * Main injector.
     */
    private[this] final var mainInjector : Option[Injector] = None

    /**
     * Called by native executable to initialize the application before starting it.
     */
    def init () : Unit = {
        // Lock memory
        try {
            DaemonHelper.mlockall ();
            debug ("Memory has been locked");
        } catch {
            case e : OSException =>
                warn ("Failed to lock memory for thread " +:+ e.getMessage, e);
        }

        /**
         * Additional module. Module that is in charge of creating and registering additional
         * actors from scala object (modules).
         */
        object additionalModule extends AbstractModule {
            debugLazy ("All additional actor classes" +:+ allAdditionalActorClasses)

            // Separate actor object classes from actor classes
            for (clazz <- allAdditionalActorClasses) {
                ClassUtils.getModuleInstance (clazz) match {
                    case Some (obj) => allAdditionalActors += obj
                    case None       => actorClassesForGuice += clazz
                }
            }

            debugLazy ("Set of additional actors after instantion by MODULE$"
                       +:+ allAdditionalActors)

            def configure () : Unit = {
                for (actor <- allAdditionalActors if actor.isInstanceOf [Autoregister]) {
                    val autoreg = actor.asInstanceOf [Autoregister]
                    val actorParentClass = actor.getClass.getSuperclass

                    def forceBind [A, B] (clazz : Class[A], obj : B) : Unit = {
                        bind (clazz)
                                .annotatedWith (Names.named (autoreg.registrationName))
                                .toInstance (obj.asInstanceOf [A])

                        debugLazy ("Binding " + clazz + " to " + obj)
                    }

                    forceBind (actorParentClass, actor)
                }
            }
        }

        /**
         * Injector that is supposed to be used to instantiate all IoC classes of the app.
         */
        val injector = basicInjector.createChildInjector (additionalModule)
        mainInjector = Some (injector)

        // Instantiate actor classes by guice
        debugLazy ("Actor classes to be instantiated by Guice" +:+ actorClassesForGuice)        
        allAdditionalActors ++= actorClassesForGuice.map (injector.getInstance (_))

        // Debug
        debugLazy ("All additional actors" +:+ allAdditionalActors)
    }

    /**
     * Called by native executable to start the application after the application
     * has been initialized.
     */
    def start () : Unit = {
        mywireManager.start

        mywireManager.jacoreManager.startActors (allAdditionalActors)

        DaemonBoot.executor.shutdown ()
    }

    /**
     * Called by native executable to stop the application before destroying it.
     */
    def stop () : Unit = {
        mywireManager.jacoreManager.stopActors (allAdditionalActors)

        mywireManager.stop
    }

    /**
     * Called by native executable to destroy the application.
     */
    def destroy () : Unit = {
        unregisterJmxBean
    }

    /**
     * Creates graphviz graph definition.
     */
    private[this] def createGraph () : String = {
        val injector =
            mainInjector match {
                case None => basicInjector
                case Some (inj) => inj
            }

        GuiceUtils.createModuleGraphAsString (injector, actorClassesForGuice.toSeq : _*)
    }

    /**
     * Restart the daemon.
     */
    private[this] def restart () : Unit = {
        debug ("About to restart the daemon")

        DaemonHelper.kill (pid, DaemonHelper.SIG_HUP);

        debug ("HUP signal has been sent")
    }
}
