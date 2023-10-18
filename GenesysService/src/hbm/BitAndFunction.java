package hbm;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class BitAndFunction implements SQLFunction {

	@Override
	public Type getReturnType(Type arg0, Mapping arg1) throws QueryException {
		return org.hibernate.type.IntegerType.INSTANCE;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public String render(Type arg0, @SuppressWarnings("rawtypes") List arguments, SessionFactoryImplementor arg2) throws QueryException {
		if(arguments.size() != 2){
			throw new IllegalArgumentException("BitAndFunction requires 2 arguments!"); 
		}
		return "(" + arguments.get(0).toString() + " & " + arguments.get(1).toString() + ")"; 
	}

}
