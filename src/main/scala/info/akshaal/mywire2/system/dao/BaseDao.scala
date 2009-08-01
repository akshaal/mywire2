package info.akshaal.mywire2.system.dao

import com.ibatis.common.resources.Resources
import com.ibatis.sqlmap.client.SqlMapClientBuilder

private[dao] abstract class BaseDao {
    protected val sqlmap = BaseDao.sqlmap
}

private[dao] object BaseDao {
    private val reader = Resources.getResourceAsReader ("sqlmap.xml")
    private val sqlmap = SqlMapClientBuilder.buildSqlMapClient(reader)
}