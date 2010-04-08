package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * GenericConstraintColumn
 */
public class GenericConstraintColumn implements DBSConstraintColumn
{
    private GenericConstraint constraint;
    private GenericTableColumn tableColumn;
    private int ordinalPosition;

    public GenericConstraintColumn(GenericConstraint constraint, GenericTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

    @Property(name = "Column", viewable = true, order = 2)
    public GenericTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = true, order = 1)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return tableColumn.getName();
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public DBSObject getParentObject()
    {
        return constraint;
    }

    public GenericDataSource getDataSource()
    {
        return constraint.getDataSource();
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }
}
