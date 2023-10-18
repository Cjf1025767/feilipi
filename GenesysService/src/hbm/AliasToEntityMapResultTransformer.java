package hbm;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.transform.AliasedTupleSubsetResultTransformer;

public class AliasToEntityMapResultTransformer extends AliasedTupleSubsetResultTransformer {
	private static final long serialVersionUID = 1L;
	public static final AliasToEntityMapResultTransformer INSTANCE = new AliasToEntityMapResultTransformer();

    private AliasToEntityMapResultTransformer() {
    }

    public Object transformTuple(Object[] tuple, String[] aliases) {
        Map result = new HashMap(tuple.length);

        for(int i = 0; i < tuple.length; ++i) {
            String alias = aliases[i];
            if (alias != null) {
                result.put(alias.toLowerCase(), tuple[i]);
            }
        }

        return result;
    }

    public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
        return false;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}