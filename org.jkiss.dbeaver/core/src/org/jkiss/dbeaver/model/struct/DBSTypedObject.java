/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * DBSColumnDefinition
 */
public interface DBSTypedObject extends DBPObject
{
    /**
     * Database specific type name
     * @return type name
     */
    String getTypeName();

    /**
     * JDBC type number.
     * Refer java.sql.Types for possible values
     * @return value type
     */
    int getValueType();

    /**
     * Value scale
     * @return scale
     */
    int getScale();

    /**
     * Value precision
     * @return precision
     */
    int getPrecision();

}