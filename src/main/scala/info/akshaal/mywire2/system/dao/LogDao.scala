package info.akshaal.mywire2.system.dao

import domain.LogRecord

private[system] object LogDao extends BaseDao {
    def insertRecord (logRecord : LogRecord) = {
        sqlmap.insert ("insertLogRecord", logRecord)
    }
}
