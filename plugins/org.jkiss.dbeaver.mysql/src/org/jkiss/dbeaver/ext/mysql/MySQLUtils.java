/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.registry.OSDescriptor;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQL utils
 */
public class MySQLUtils {

    static final Log log = LogFactory.getLog(MySQLUtils.class);

    private static Map<String, Integer> typeMap = new HashMap<String, Integer>();
    public static final String COLUMN_POSTFIX_PRIV = "_priv";

    static {
        typeMap.put("BIT", java.sql.Types.BIT);
        typeMap.put("TINYINT", java.sql.Types.TINYINT);
        typeMap.put("SMALLINT", java.sql.Types.SMALLINT);
        typeMap.put("MEDIUMINT", java.sql.Types.INTEGER);
        typeMap.put("INT", java.sql.Types.INTEGER);
        typeMap.put("INTEGER", java.sql.Types.INTEGER);
        typeMap.put("INT24", java.sql.Types.INTEGER);
        typeMap.put("BIGINT", java.sql.Types.BIGINT);
        typeMap.put("REAL", java.sql.Types.DOUBLE);
        typeMap.put("FLOAT", java.sql.Types.FLOAT);
        typeMap.put("DECIMAL", java.sql.Types.DECIMAL);
        typeMap.put("NUMERIC", java.sql.Types.DECIMAL);
        typeMap.put("DOUBLE", java.sql.Types.DOUBLE);
        typeMap.put("CHAR", java.sql.Types.CHAR);
        typeMap.put("VARCHAR", java.sql.Types.VARCHAR);
        typeMap.put("DATE", java.sql.Types.DATE);
        typeMap.put("TIME", java.sql.Types.TIME);
        typeMap.put("YEAR", java.sql.Types.DATE);
        typeMap.put("TIMESTAMP", java.sql.Types.TIMESTAMP);
        typeMap.put("DATETIME", java.sql.Types.TIMESTAMP);
        typeMap.put("TINYBLOB", java.sql.Types.BINARY);
        typeMap.put("BLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("MEDIUMBLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("LONGBLOB", java.sql.Types.LONGVARBINARY);
        typeMap.put("TINYTEXT", java.sql.Types.VARCHAR);
        typeMap.put("TEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put("MEDIUMTEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put("LONGTEXT", java.sql.Types.LONGVARCHAR);
        typeMap.put(MySQLConstants.TYPE_NAME_ENUM, java.sql.Types.CHAR);
        typeMap.put(MySQLConstants.TYPE_NAME_SET, java.sql.Types.CHAR);
        typeMap.put("GEOMETRY", java.sql.Types.BINARY);
        typeMap.put("BINARY", java.sql.Types.BINARY);
        typeMap.put("VARBINARY", java.sql.Types.VARBINARY);
    }

    public static int typeNameToValueType(String typeName)
    {
        Integer valueType = typeMap.get(typeName.toUpperCase());
        return valueType == null ? java.sql.Types.OTHER : valueType;
    }

    public static List<String> collectPrivilegeNames(ResultSet resultSet)
    {
        // Now collect all privileges columns
        try {
            List<String> privs = new ArrayList<String>();
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                String colName = rsMetaData.getColumnName(i + 1);
                if (colName.toLowerCase().endsWith(COLUMN_POSTFIX_PRIV)) {
                    privs.add(colName.substring(0, colName.length() - COLUMN_POSTFIX_PRIV.length()));
                }
            }
            return privs;
        } catch (SQLException e) {
            log.debug(e);
            return Collections.emptyList();
        }
    }

    public static Map<String, Boolean> collectPrivileges(List<String> privNames, ResultSet resultSet)
    {
        // Now collect all privileges columns
        Map<String, Boolean> privs = new TreeMap<String, Boolean>();
        for (String privName : privNames) {
            privs.put(privName, "Y".equals(JDBCUtils.safeGetString(resultSet, privName + COLUMN_POSTFIX_PRIV)));
        }
        return privs;
    }


    public static String getMySQLConsoleBinaryName()
    {
        return DBeaverCore.getInstance().getLocalSystem().isWindows() ? "mysql.exe" : "mysql";
    }
}
