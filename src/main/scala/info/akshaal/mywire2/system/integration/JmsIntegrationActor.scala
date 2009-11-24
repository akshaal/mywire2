/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system
package integration

import com.google.inject.{Singleton, Inject}
import javax.jms.{ConnectionFactory, Destination, Message, Session}
import java.util.HashMap

import info.akshaal.jacore.system.annotation.Act
import info.akshaal.jacore.system.jms.AbstractJmsSenderActor
import info.akshaal.jacore.system.actor.{LowPriorityActorEnv, NormalPriorityActorEnv, Actor}

import annotation.JmsIntegrationExport
import domain.ExportNotification

/**
 * This actor has no public interfaces and is not supposed to be used by other actors.
 * All this actor is supposed to do is to listen for broadcasted object of ExportNotification
 * type.
 */
@Singleton
private[system] class JmsIntegrationActor @Inject() (
                                    normalPriorityActorEnv : NormalPriorityActorEnv,
                                    sender : JmsIntegrationSenderActor)
                    extends Actor (normalPriorityActorEnv)
{
    manage (sender)

    /**
     * Handle ExportNotification's.
     */
    @Act (subscribe = true)
    protected def handleNotifications (msg : ExportNotification) : Unit = {
        val map = msg.toMap updated ("time", System.currentTimeMillis)

        sender.send (map)
    }
}

/**
 * Actor to send map messages to JMS topic.
 */
@Singleton
private[integration] class JmsIntegrationSenderActor @Inject() (
                                    lowPriorityActorEnv : LowPriorityActorEnv,
                                    @JmsIntegrationExport connectionFactory : ConnectionFactory,
                                    @JmsIntegrationExport destination : Destination)
                     extends AbstractJmsSenderActor [Map[String, Any]] (
                                    lowPriorityActorEnv : LowPriorityActorEnv,
                                    connectionFactory : ConnectionFactory,
                                    destination : Destination)
{
    /** {InheritDoc} */
    override protected def createJmsMessage (session : Session,
                                             map : Map [String, Any]) : Message =
    {
        val hashMap = new HashMap[String, Any]

        for ((key, value) <- map) {
            hashMap.put (key, value)
        }

        session.createObjectMessage (hashMap)
    }
}