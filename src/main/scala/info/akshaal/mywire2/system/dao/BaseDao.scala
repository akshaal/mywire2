package info.akshaal.mywire2.dao

import com.ibatis.common.resources.Resources
import com.ibatis.sqlmap.client.SqlMapClientBuilder

class BaseDao {
    protected val sqlmap = BaseDao.sqlmap
}

object BaseDao {
    private val reader = Resources.getResourceAsReader ("sqlmap.xml")
    private val sqlmap = SqlMapClientBuilder.buildSqlMapClient(reader)
}