<#-- @ftlvariable name="entity" type="org.greenrobot.greendao.generator.Entity" -->
/** called by internal mechanisms, do not call yourself. */
@Generated
public void __setDaoSession(DaoSession daoSession) {
    this.daoSession = daoSession;
    myDao = daoSession != null ? daoSession.get${entity.classNameDao?cap_first}() : null;
}