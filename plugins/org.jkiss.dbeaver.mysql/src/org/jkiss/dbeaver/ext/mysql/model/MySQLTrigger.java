/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class MySQLTrigger extends AbstractTrigger implements MySQLSourceObject
{
    static final Log log = LogFactory.getLog(MySQLTrigger.class);

    private MySQLCatalog catalog;
    private MySQLTable table;
    private String body;
    private String charsetClient;
    private String sqlMode;

    public MySQLTrigger(
        MySQLCatalog catalog,
        MySQLTable table,
        ResultSet dbResult)
    {
        this.catalog = catalog;
        this.table = table;

        setName(JDBCUtils.safeGetString(dbResult, "Trigger"));
        setManipulationType(DBSManipulationType.getByName(JDBCUtils.safeGetString(dbResult, "Event")));
        setActionTiming(DBSActionTiming.getByName(JDBCUtils.safeGetString(dbResult, "Timing")));
        this.body = JDBCUtils.safeGetString(dbResult, "Statement");
        this.charsetClient = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_CHARACTER_SET_CLIENT);
        this.sqlMode = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_SQL_MODE);
    }

    public String getBody()
    {
        return body;
    }

    @Property(name = "Table", viewable = true, order = 4)
    public MySQLTable getTable()
    {
        return table;
    }

    @Property(name = "Client Charset", order = 5)
    public String getCharsetClient()
    {
        return charsetClient;
    }

    @Property(name = "SQL Mode", order = 6)
    public String getSqlMode()
    {
        return sqlMode;
    }

    public DBSCatalog getParentObject()
    {
        return catalog;
    }

    public MySQLDataSource getDataSource()
    {
        return catalog.getDataSource();
    }

    @Property(name = "Definition", hidden = true, editable = true, updatable = true, order = -1)
    public String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        return getBody();
    }

    public void setSourceText(String sourceText) throws DBException
    {
        body = sourceText;
    }
}