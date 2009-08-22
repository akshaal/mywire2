package info.akshaal.mywire2
package system
package dao

import com.google.inject.{Singleton, Inject}

import domain.LogRecord

@Singleton
private[system] final class LogDao @Inject() () extends BaseDao {
    final def insertRecord (logRecord : LogRecord) = {
        sqlmap.insert ("insertLogRecord", logRecord)
    }
}
