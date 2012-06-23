/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GenericTableConstraint
 */
public class GenericPrimaryKey extends GenericTableConstraint
{
    private List<GenericTableConstraintColumn> columns;

    public GenericPrimaryKey(GenericTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    /**
     * Copy constructor
     * @param constraint
     */
    GenericPrimaryKey(GenericPrimaryKey constraint)
    {
        super(constraint.getTable(), constraint.getName(), constraint.getDescription(), constraint.getConstraintType(), constraint.isPersisted());
        if (constraint.columns != null) {
            this.columns = new ArrayList<GenericTableConstraintColumn>(constraint.columns.size());
            for (GenericTableConstraintColumn sourceColumn : constraint.columns) {
                this.columns.add(new GenericTableConstraintColumn(this, sourceColumn));
            }
        }
    }

    @Override
    public List<GenericTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericTableConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericTableConstraintColumn> columns)
    {
        this.columns = columns;
        if (!CommonUtils.isEmpty(this.columns) && this.columns.size() > 1) {
            Collections.sort(columns, new Comparator<GenericTableConstraintColumn>() {
                @Override
                public int compare(GenericTableConstraintColumn o1, GenericTableConstraintColumn o2)
                {
                    return o1.getOrdinalPosition() - o2.getOrdinalPosition();
                }
            });
        }
    }

    public boolean hasColumn(GenericTableColumn column)
    {
        if (this.columns != null) {
            for (GenericTableConstraintColumn constColumn : columns) {
                if (constColumn.getAttribute() == column) {
                    return true;
                }
            }
        }
        return false;
    }
}