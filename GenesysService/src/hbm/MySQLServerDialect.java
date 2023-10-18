package hbm;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class MySQLServerDialect extends org.hibernate.dialect.MySQLDialect{
	public MySQLServerDialect () {
        super();
        registerHibernateType(-9, "string");  
        registerHibernateType(-16, "string"); 
        //函数名不区分大小写
        registerFunction("dateaddsecond", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "DATE_ADD(?2, INTERVAL ?1 SECOND)"));
        registerFunction("datediffsecond", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "TIMESTAMPDIFF(SECOND, ?1, ?2)"));
        registerFunction("dayofweek", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "WEEKDAY(?1)"));
        registerFunction("second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(SECOND FROM ?1)" ) );
		registerFunction("minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(MINUTE FROM ?1)" ) );
		registerFunction("hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "EXTRACT(HOUR FROM ?1)" ) );
		registerFunction("bitand", new BitAndFunction());
		registerFunction("week", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "week(?1,4)" ) );//返回一年中的第几周
    }

}
