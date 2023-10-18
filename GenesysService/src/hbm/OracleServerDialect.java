package hbm;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class OracleServerDialect extends org.hibernate.dialect.Oracle12cDialect{
	public OracleServerDialect () {
        super();
        //函数名不区分大小写
        registerFunction("dateaddsecond", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "(?2+INTERVAL '?1' SECOND)"));
        registerFunction("datediffsecond", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "((CAST(?1 AS DATE)-CAST(?2 AS DATE))*24*60*60)"));
        registerFunction("dayofweek", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "TO_CHAR(?1,'D')"));
        registerFunction("second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(SECOND FROM ?1)" ) );
		registerFunction("minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(MINUTE FROM ?1)" ) );
		registerFunction("hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(HOUR FROM ?1)" ) );
		registerFunction("week", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "TO_CHAR(?1,'WW')"));
    }

}
