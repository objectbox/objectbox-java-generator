package io.objectbox.generator.model;

import io.objectbox.generator.IdUid;

/** To-many relationship from a source entity to many target entities. */
public class ToManyStandalone extends ToManyBase {

    private IdUid modelId;

    // TODO actually parse and set this
    private String dbName;

    public ToManyStandalone(Schema schema, Entity sourceEntity, Entity targetEntity) {
        super(schema, sourceEntity, targetEntity);
    }

    public IdUid getModelId() {
        return modelId;
    }

    public void setModelId(IdUid modelId) {
        this.modelId = modelId;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    void init2ndPass() {
        super.init2ndPass();
    }

    void init3rdPass() {
        super.init3rdPass();
    }

}
