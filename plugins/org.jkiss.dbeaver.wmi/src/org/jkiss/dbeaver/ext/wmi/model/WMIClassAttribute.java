package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.wmi.service.WMIObjectAttribute;
import org.jkiss.wmi.service.WMIQualifiedObject;

/**
 * Class attribute
 */
public abstract class WMIClassAttribute<T extends WMIObjectAttribute> implements DBSEntity
{
    protected final WMIClass wmiClass;
    protected final T attribute;

    protected WMIClassAttribute(WMIClass wmiClass, T attribute)
    {
        this.wmiClass = wmiClass;
        this.attribute = attribute;
    }

    public WMIClass getParentObject()
    {
        return wmiClass;
    }

    public DBPDataSource getDataSource()
    {
        return wmiClass.getDataSource();
    }

    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return attribute.getName();
    }

    public String getDescription()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }
}
