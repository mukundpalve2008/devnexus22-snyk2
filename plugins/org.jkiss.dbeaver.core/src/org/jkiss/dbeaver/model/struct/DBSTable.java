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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * Table
 */
public interface DBSTable extends DBSEntity, DBPQualifiedObject
{

    boolean isView();

    DBSObjectContainer getContainer();

    /**
     * Table columns
     * @return list of columns
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSTableColumn> getColumns(DBRProgressMonitor monitor) throws DBException;

    /**
     * Retrieve table column by it's name (case insensitive)
     * @param monitor
     *@param columnName column name  @return column or null
     * @throws DBException on any DB error
     */
    DBSTableColumn getColumn(DBRProgressMonitor monitor, String columnName) throws DBException;

    /**
     * Table indices
     * @return list of indices
     * @throws DBException  on any DB error
     * @param monitor
     */
    Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    /**
     * Keys are: primary keys and unique keys.
     * Foreign keys can be obtained with {@link #getReferences(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor)}
     * @return list of constraints
     * @throws DBException on any DB error
     * @param monitor
     */
    @Override
    Collection<? extends DBSTableConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this table foreign keys
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor
     */
    @Override
    Collection<? extends DBSTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets foreign keys which refers this table
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor
     */
    @Override
    Collection<? extends DBSTableForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException;

}
