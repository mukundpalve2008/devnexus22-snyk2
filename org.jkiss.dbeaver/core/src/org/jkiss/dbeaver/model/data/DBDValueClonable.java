/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DBDContentClonable
 */
public interface DBDValueClonable extends DBDValue {

    /**
     * Makes exact copy of content object
     * @return copy  @param monitor
     */
    DBDValueClonable cloneValue(DBRProgressMonitor monitor)
        throws DBCException;

}