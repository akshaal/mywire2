/*
 * SimpleJmx.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.jmx

import java.lang.management.ManagementFactory
import javax.management.{MBeanInfo, DynamicMBean, Attribute,
                         AttributeList, MBeanAttributeInfo,
                         MBeanOperationInfo, MBeanServer,
                         ObjectName}

import scala.collection.mutable.{Map, HashMap}
import scala.collection.JavaConversions._

trait SimpleJmx {
    private final type JmxGeneralAttr = JmxAttr[Nothing, Any]

    protected def jmxObjectName : String
    protected def jmxAttributes : List[JmxGeneralAttr] = Nil
    protected def jmxOperations : List[JmxOper] = Nil

    private val attrMap : Map[String, JmxGeneralAttr] = new HashMap ()
    private val operMap : Map[String, JmxOper] = new HashMap ()

    // Fill in attribute and operation maps
    for (jmxAttr <- jmxAttributes) {
        attrMap (jmxAttr.name) = jmxAttr
    }

    for (jmxOper <- jmxOperations) {
        operMap (jmxOper.name) = jmxOper
    }

    // Register
    ManagementFactory.getPlatformMBeanServer()
                     .registerMBean (MBean, new ObjectName(jmxObjectName))

    /**
     * MBean definition.
     */
    private object MBean extends DynamicMBean {
        override def getAttribute (attribute : String) : Object = {
            attrMap.get(attribute) match {
                case None => null
                case Some(jmxAttr) => jmxAttr.getAsObject
            }
        }

        override def setAttribute (attribute : Attribute) : Unit = {
            attrMap.get(attribute.getName) match {
                case None => null
                case Some(jmxAttr) => jmxAttr.setAsObject (attribute.getValue)
            }
        }

        override def getAttributes (attributes : Array[String]) : AttributeList =
        {
            val list = new AttributeList

            for (name <- attributes) {
                list.add(new Attribute (name, getAttribute(name)))
            }

            list
        }

        override def setAttributes (attributes : AttributeList) : AttributeList =
        {
            for (attribute <- attributes) {
                setAttribute (attribute.asInstanceOf[Attribute])
            }

            attributes
        }

        override def invoke (actionName : String,
                             params : Array[Object],
                             signature : Array[String]) : Object =
        {
            operMap.get(actionName).foreach (_.f ())
            null
        }

        override def getMBeanInfo () : MBeanInfo = {
            val attrs = jmxAttributes.map (makeAttributeInfo).toArray
            val ops = jmxOperations.map (makeOperationInfo).toArray

            new MBeanInfo (/* className = */ this.getClass.getName,
                           /* desc      = */ null,
                           /* attrs     = */ attrs,
                           /* constr    = */ null,
                           /* oper-ions = */ ops,
                           /* notifs    = */ null)
        }

        /**
         * Make operation info.
         */
        def makeOperationInfo (jmxOper : JmxOper) : MBeanOperationInfo = {
            new MBeanOperationInfo (/* name   = */ jmxOper.name,
                                    /* descr  = */ null,
                                    /* signat = */ null,
                                    /* return = */ "void",
                                    /* impact = */ MBeanOperationInfo.INFO)
        }

        /**
         * Create attribute.
         */
        def makeAttributeInfo (jmxAttr : JmxAttr[Nothing, Any]) : MBeanAttributeInfo =
        {
            val argType =
                (jmxAttr.getter, jmxAttr.setter) match {
                    case (Some(getterFunc), _) =>
                        getterFunc.getClass
                                  .getMethod ("apply")
                                  .getReturnType

                    case (_, Some(setterFunc)) =>
                        setterFunc.getClass
                                  .getMethod ("apply", classOf[Object])
                                  .getParameterTypes()(0)

                    case (None, None) =>
                        throw new IllegalArgumentException (
                            "Either getter or setter (or both)"
                            + " must be defined for jmx attribute: "
                            + jmxAttr.name)
                }

            new MBeanAttributeInfo (/* name      = */ jmxAttr.name,
                                    /* type      = */ argType.getName,
                                    /* descr     = */ null,
                                    /* readable  = */ jmxAttr.getter != None,
                                    /* writeable = */ jmxAttr.setter != None,
                                    /* isIs      = */ argType == classOf[Boolean])
        }
    }
}

/**
 * Definition of one JMX attribute.
 */
sealed case class JmxAttr[-T, +R] (name   : String,
                                   getter : Option[Function0[R]],
                                   setter : Option[Function1[T, Unit]])
                               extends NotNull
{
    private[jmx] def getAsObject () : Object = getter match {
        case Some(f) => f ().asInstanceOf[Object]
        case None => null
    }

    private[jmx] def setAsObject (v : Object) : Unit = setter match {
        case Some(f) => f (v.asInstanceOf[T])
        case None => ()
    }
}

/**
 * Definition of one JMX operation.
 */
sealed case class JmxOper (name : String, f : Function0[Any]) extends NotNull