package info.akshaal.mywire2.dao

import info.akshaal.mywire2.domain.LogRecord

object LogDao extends BaseDao {
    def insertRecord (logRecord : LogRecord) = {
        sqlmap.insert ("insertLogRecord", logRecord)
    }
}
