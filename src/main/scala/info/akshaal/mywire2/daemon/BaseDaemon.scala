/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package daemon

import com.google.inject.Guice

import scala.collection.mutable.{Set, HashSet}

import info.akshaal.daemonhelper.{OSException, DaemonHelper}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.module.Module
import info.akshaal.jacore.actor.Actor
import info.akshaal.jacore.utils.{ClassUtils, GuiceUtils}
import info.akshaal.jacore.logger.Logging
import info.akshaal.jacore.jmx.{SimpleJmx, JmxOper}

/**
 * Abstract daemon.
 *
 * @param Instantance of module that will is used for injector.
 */
abstract class BaseDaemon (module : Module) extends Logging with SimpleJmx {
    /**
     * Injector that is supposed to be used to instantiate all IoC classes of the app.
     */
    protected final val injector = Guice.createInjector (module)

    /**
     * MywireManager instance of the application.
     */
    protected final val mywireManager = injector.getInstanceOf [MywireManager]

    /**
     * All actors that are to be started/stopped automaticcaly.
     */
    private[this] final val allAdditionalActors : Set[Actor] = new HashSet

    /**
     * Operations exposed through jmx.
     */
    override lazy val jmxOperations = List (JmxOper ("graph", createGraph))
    
    /**
     * Called by native executable to initialize the application before starting it.
     */
    def init () : Unit = {
        // Lock memory
        try {
            DaemonHelper.mlockall ();
            info ("Memory has been locked");
        } catch {
            case e : OSException =>
                warn ("Failed to lock memory for thread " + ": " + e.getMessage, e);
        }

        // Actor classes
        val allAdditionalActorClasses : Set [Class [_ <: Actor]] = new HashSet
        allAdditionalActorClasses ++= additionalActorClasses
        
        for (pkg <- additionalAutostartActorPackages) {
            val classes =
                    ClassUtils.findClasses (pkg,
                                            Thread.currentThread.getContextClassLoader,
                                            classOf [Autostart].isAssignableFrom (_))

            allAdditionalActorClasses ++= classes.asInstanceOf [List [Class [Actor]]]
        }

        // Actors
        allAdditionalActors ++= additionalActors
        allAdditionalActors ++= allAdditionalActorClasses.map (injector.getInstance (_))

        // Debug
        debugLazy ("All additional actors: " + allAdditionalActors)
    }

    /**
     * Called by native executable to start the application after the application
     * has been initialized.
     */
    def start () : Unit = {
        mywireManager.start

        mywireManager.jacoreManager.startActors (allAdditionalActors)
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
        val allAdditionalActorsClasses = allAdditionalActors.map (_.getClass)

        GuiceUtils.createModuleGraphAsString (injector, allAdditionalActorsClasses.toSeq : _*)
    }

    /**
     * Actors start automatically.
     */
    protected def additionalActors : Seq [Actor] = Seq ()

    /**
     * Actors classes to instantiate using injector and start them.
     */
    protected def additionalActorClasses : Seq [Class [_ <: Actor]] = Seq ()

    /**
     * A number of packages that is supposed to be scanned to find
     * actors that implement Autostart trait. These actors will
     * be started automatically.
     */
    protected def additionalAutostartActorPackages : Seq [String] = Seq ()
}
