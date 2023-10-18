package hbm;

import java.sql.Types;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class MSSQLServerDialect extends org.hibernate.dialect.SQLServer2008Dialect{
	public MSSQLServerDialect () {
        super(); 
        registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());  //-9
        registerHibernateType(Types.LONGNVARCHAR, StandardBasicTypes.STRING.getName()); //-16
        //函数名不区分大小写
        registerFunction("dateaddsecond", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "dateadd(ss, ?1, ?2)"));
        registerFunction("datediffsecond", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datediff(ss, ?1, ?2)"));
        registerFunction("dayofweek", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datepart(weekday, ?1)"));//返回一周中的第几天(周日=1, 周1=2)
        registerFunction("second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(second, ?1)" ) );
		registerFunction("minute", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(minute, ?1)" ) );
		registerFunction("hour", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(hour, ?1)" ) );
		registerFunction("bitand", new BitAndFunction());
		registerFunction("length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "len(?1)" ) );
		registerFunction("row_number", new SQLFunctionTemplate( StandardBasicTypes.BIG_INTEGER, "ROW_NUMBER()" ) );
		registerFunction("week", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(week, ?1)" ) );//返回一年中的第几周
    }

}
