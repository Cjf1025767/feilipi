package hbm;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class PostgreSQLServerDialect extends org.hibernate.dialect.PostgreSQL94Dialect{
	public PostgreSQLServerDialect () {
        super();
        //函数名不区分大小写
        registerFunction("dateaddsecond", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "(?2+INTERVAL '?1' SECOND)"));
        registerFunction("datediffsecond", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "(EXTRACT(EPOCH FROM (?1 - ?2)))"));
        registerFunction("dayofweek", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "TO_CHAR(?1,'D')"));
        registerFunction("second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(SECOND FROM ?1)" ) );
		registerFunction("minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(MINUTE FROM ?1)" ) );
		registerFunction("hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(HOUR FROM ?1)" ) );
		registerFunction("bitand", new BitAndFunction());
		registerFunction("week", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(WEEK FROM ?1)" ) );//返回一年中的第几周
    }

}
