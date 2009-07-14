package info.akshaal.mywire2.dao

object LogDao extends BaseDao {
    def insert () = {
        sqlmap.insert ("insertLog")
    }
}
