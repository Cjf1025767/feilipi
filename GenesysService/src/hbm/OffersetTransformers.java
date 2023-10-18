package hbm;

import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.ResultTransformer;

public class OffersetTransformers {

    public static final AliasToEntityMapResultTransformer ALIAS_TO_ENTITY_MAP;


    private OffersetTransformers() {
    }

    public static ResultTransformer aliasToBean(Class target) {
        return new AliasToBeanResultTransformer(target);
    }

    static {
        ALIAS_TO_ENTITY_MAP = AliasToEntityMapResultTransformer.INSTANCE;

    }
}