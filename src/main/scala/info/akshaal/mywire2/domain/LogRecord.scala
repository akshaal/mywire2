package info.akshaal.mywire2
package domain

import java.util.Date
import scala.reflect.BeanProperty

import info.akshaal.jacore.`package`._

private[mywire2] sealed case class LogRecord (
                             @BeanProperty val time       : Date,
                             @BeanProperty val nano       : Long,
                             @BeanProperty val level      : LogRecordLevel.Level,
                             @BeanProperty val category   : String,
                             @BeanProperty val msg        : String,
                             @BeanProperty val thread     : String,
                             @BeanProperty val throwable  : String)
                    extends NotNull

private[mywire2] object LogRecordLevel extends JacoreEnum (initial = 0) {
    class Level extends Value

    val Debug = new Level
    val Info = new Level
    val Warn = new Level
    val Error = new Level
    val BusinessLogicInfo = new Level
    val BusinessLogicWarning = new Level
    val BusinessLogicProblem = new Level
}