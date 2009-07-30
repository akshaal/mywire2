package info.akshaal.mywire2
package system
package dao

import domain.LogRecord

private[system] class LogDao extends BaseDao {
    final def insertRecord (logRecord : LogRecord) = {
        sqlmap.insert ("insertLogRecord", logRecord)
    }
}
