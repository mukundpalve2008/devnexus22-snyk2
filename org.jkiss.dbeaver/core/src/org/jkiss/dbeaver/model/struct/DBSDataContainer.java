/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;

import java.util.List;

/**
 * Data container.
 * Provides facilities to query object for data.
 * Any data container MUST support data read. Other function may be not supported (client can check it with {@link #getSupportedFeatures()}).
 */
public interface DBSDataContainer extends DBSObject {

    public static final int DATA_INSERT         = 1;
    public static final int DATA_UPDATE         = 2;
    public static final int DATA_DELETE         = 4;

    int getSupportedFeatures();

    long readData(
        DBCExecutionContext context,
        DBDDataReceiver dataReceiver,
        long firstRow,
        long maxRows)
        throws DBException;

    long insertData(
        DBCExecutionContext context,
        List<DBDColumnValue> columns,
        DBDDataReceiver keysReceiver)
        throws DBException;

    long updateData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns,
        List<DBDColumnValue> updateColumns,
        DBDDataReceiver keysReceiver)
        throws DBException;

    long deleteData(
        DBCExecutionContext context,
        List<DBDColumnValue> keyColumns)
        throws DBException;

}
