package org.greenrobot.greendao.generator;

import java.util.Map;

/**
 * Created by Markus on 20.09.2016.
 */
public class InternalAccess {
    public static void init2ndAnd3rdPass(Schema schema) {
        schema.init2ndPass();
        schema.init3rdPass();
    }

    public static void setPropertyToDbType(Schema schema, Map<PropertyType, String> propertyToDbType) {
        schema.setPropertyToDbType(propertyToDbType);
    }

}
