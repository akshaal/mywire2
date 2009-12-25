/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package daemon

import com.google.inject.{Guice, Injector}
import com.google.inject.AbstractModule
import com.google.inject.name.Names

import scala.collection.mutable.{Set, HashSet}

import info.akshaal.daemonhelper.{OSException, DaemonHelper}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.actor.Actor
import info.akshaal.jacore.utils.{ClassUtils, GuiceUtils}
import info.akshaal.jacore.logger.Logging
import info.akshaal.jacore.jmx.{SimpleJmx, JmxOper}

import module.Module

/**
 * Abstract daemon.
 *
 * @param module Instantance of module that will is used for injector.
 * @param additionalActorClasses instance of these class will be created using reflections or guice.
 * @param additionalAutostartActorPackages A number of packages that is supposed
 *        to be scanned to find actors that implement Autostart trait. These actors will
 *        be started automatically.
 */
abstract class BaseDaemon (module : Module,
                           additionalActorClasses : Seq [Class [_ <: Actor]] = Seq (),
                           additionalAutostartActorPackages : Seq [String] = Seq ())
                  extends Logging
                     with SimpleJmx
{
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
     * MywireManager instance of the application.
     */
    protected final val mywireManager = basicInjector.getInstanceOf [MywireManager]

    /**
     * Operations exposed through jmx.
     */
    override lazy val jmxOperations = List (JmxOper ("graph", createGraph))
    
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
            info ("Memory has been locked");
        } catch {
            case e : OSException =>
                warn ("Failed to lock memory for thread " + ": " + e.getMessage, e);
        }

        // A set of classes that must be instantiate using full-blown injector.
        val actorClassesForGuice : Set [Class [_ <: Actor]] = new HashSet

        /**
         * Additional module. Module that is in charge of creating and registering additional
         * actors from scala object (modules).
         */
        object additionalModule extends AbstractModule {
            private[this] val allAdditionalActorClasses : Set [Class[_ <: Actor]] = new HashSet
            allAdditionalActorClasses ++= additionalActorClasses

            for (pkg <- additionalAutostartActorPackages) {
                val classes =
                        ClassUtils.findClasses (pkg,
                                                Thread.currentThread.getContextClassLoader,
                                                classOf [Autostart].isAssignableFrom (_))

                allAdditionalActorClasses ++= classes.asInstanceOf [List [Class [Actor]]]
            }

            // Separate actor object classes from actor classes
            for (clazz <- allAdditionalActorClasses) {
                ClassUtils.getModuleInstance (clazz) match {
                    case Some (obj) => allAdditionalActors += obj
                    case None       => actorClassesForGuice += clazz
                }
            }

            def configure () : Unit = {
                for (actor <- allAdditionalActors if actor.isInstanceOf [Autoregister]) {
                    val autoreg = actor.asInstanceOf [Autoregister]
                    val actorParentClass = actor.getClass.getSuperclass

                    def forceBind [A, B] (clazz : Class[A], obj : B) : Unit = {
                        bind (clazz)
                                .annotatedWith (Names.named (autoreg.registrationName))
                                .toInstance (obj.asInstanceOf [A])
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
        allAdditionalActors ++= actorClassesForGuice.map (injector.getInstance (_))

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

        val injector =
            mainInjector match {
                case None => basicInjector
                case Some (inj) => inj
            }

        GuiceUtils.createModuleGraphAsString (injector, allAdditionalActorsClasses.toSeq : _*)
    }
}
