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
package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.HashMap;
import java.util.Map;

/**
 * Object type
 */
public enum OracleObjectType implements DBSObjectType {

	CLUSTER("CLUSTER", null, DBSObject.class, null),
    CONSTRAINT ("CONSTRAINT", DBIcon.TREE_CONSTRAINT.getImage(), OracleTableConstraint.class, null), // fake object
	CONSUMER_GROUP("CONSUMER GROUP", null, DBSObject.class, null),
	CONTEXT("CONTEXT", null, DBSObject.class, null),
	DIRECTORY("DIRECTORY", null, DBSObject.class, null),
	EVALUATION_CONTEXT("EVALUATION CONTEXT", null, DBSObject.class, null),
    FOREIGN_KEY ("FOREIGN KEY", DBIcon.TREE_FOREIGN_KEY.getImage(), OracleTableForeignKey.class, null), // fake object
	FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE.getImage(), OracleProcedureStandalone.class, new ObjectFinder() {
        @Override
        public OracleProcedureStandalone findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	INDEX("INDEX", DBIcon.TREE_INDEX.getImage(), OracleTableIndex.class, new ObjectFinder() {
        @Override
        public OracleTableIndex findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.indexCache.getObject(monitor, schema, objectName);
        }
    }),
	INDEX_PARTITION("INDEX PARTITION", null, DBSObject.class, null),
	INDEXTYPE("INDEXTYPE", null, DBSObject.class, null),
	JAVA_CLASS("JAVA CLASS", DBIcon.TREE_JAVA_CLASS.getImage(), OracleJavaClass.class, new ObjectFinder() {
        @Override
        public OracleJavaClass findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.javaCache.getObject(monitor, schema, objectName);
        }
    }),
	JAVA_DATA("JAVA DATA", null, DBSObject.class, null),
	JAVA_RESOURCE("JAVA RESOURCE", null, DBSObject.class, null),
	JOB("JOB", null, DBSObject.class, null),
	JOB_CLASS("JOB CLASS", null, DBSObject.class, null),
	LIBRARY("LIBRARY", null, DBSObject.class, null),
	LOB("LOB", null, DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", null, DBSObject.class, null),
	OPERATOR("OPERATOR", null, DBSObject.class, null),
	PACKAGE("PACKAGE", DBIcon.TREE_PACKAGE.getImage(), OraclePackage.class, new ObjectFinder() {
        @Override
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PACKAGE_BODY("PACKAGE BODY", DBIcon.TREE_PACKAGE.getImage(), OraclePackage.class, new ObjectFinder() {
        @Override
        public OraclePackage findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	PROCEDURE("PROCEDURE", DBIcon.TREE_PROCEDURE.getImage(), OracleProcedureStandalone.class, new ObjectFinder() {
        @Override
        public OracleProcedureStandalone findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	PROGRAM("PROGRAM", null, DBSObject.class, null),
	QUEUE("QUEUE", null, DBSObject.class, null),
	RULE("RULE", null, DBSObject.class, null),
	RULE_SET("RULE SET", null, DBSObject.class, null),
	SCHEDULE("SCHEDULE", null, DBSObject.class, null),
	SEQUENCE("SEQUENCE", DBIcon.TREE_SEQUENCE.getImage(), OracleSequence.class, new ObjectFinder() {
        @Override
        public OracleSequence findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.sequenceCache.getObject(monitor, schema, objectName);
        }
    }),
	SYNONYM("SYNONYM", DBIcon.TREE_SYNONYM.getImage(), OracleSynonym.class, new ObjectFinder() {
        @Override
        public OracleSynonym findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.synonymCache.getObject(monitor, schema, objectName);
        }
    }),
	TABLE("TABLE", DBIcon.TREE_TABLE.getImage(), OracleTable.class, new ObjectFinder() {
        @Override
        public OracleTableBase findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName);
        }
    }),
	TABLE_PARTITION("TABLE PARTITION", null, DBSObject.class, null),
	TRIGGER("TRIGGER", DBIcon.TREE_TRIGGER.getImage(), OracleTrigger.class, new ObjectFinder() {
        @Override
        public OracleTrigger findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.triggerCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE("TYPE", DBIcon.TREE_DATA_TYPE.getImage(), OracleDataType.class, new ObjectFinder() {
        @Override
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE_BODY("TYPE BODY", DBIcon.TREE_DATA_TYPE.getImage(), OracleDataType.class, new ObjectFinder() {
        @Override
        public OracleDataType findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	VIEW("VIEW", DBIcon.TREE_VIEW.getImage(), OracleView.class, new ObjectFinder() {
        @Override
        public OracleView findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, OracleView.class);
        }
    }),
	WINDOW("WINDOW", null, DBSObject.class, null),
	WINDOW_GROUP("WINDOW GROUP", null, DBSObject.class, null),
	XML_SCHEMA("XML SCHEMA", null, DBSObject.class, null);
    
    static final Log log = LogFactory.getLog(OracleObjectType.class);

    private static Map<String, OracleObjectType> typeMap = new HashMap<String, OracleObjectType>();

    static {
        for (OracleObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }
    
    public static OracleObjectType getByType(String typeName)
    {
        return typeMap.get(typeName);
    }
    
    private static interface ObjectFinder<OBJECT_TYPE extends DBSObject> {
        OBJECT_TYPE findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException;
    }
    
    private final String objectType;
    private final Image image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;

    <OBJECT_TYPE extends DBSObject> OracleObjectType(String objectType, Image image, Class<OBJECT_TYPE> typeClass, ObjectFinder<OBJECT_TYPE> finder)
    {
        this.objectType = objectType;
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    public boolean isBrowsable()
    {
        return finder != null;
    }

    @Override
    public String getTypeName()
    {
        return objectType;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public Image getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return typeClass;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, OracleSchema schema, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

    public static Object resolveObject(
        DBRProgressMonitor monitor,
        OracleDataSource dataSource,
        String dbLink,
        String objectTypeName,
        String objectOwner,
        String objectName) throws DBException
    {
        if (dbLink != null) {
            return objectName;
        }
        OracleObjectType objectType = OracleObjectType.getByType(objectTypeName);
        if (objectType == null) {
            log.debug("Unrecognized object type: " + objectTypeName);
            return objectName;
        }
        if (!objectType.isBrowsable()) {
            log.debug("Unsupported object type: " + objectTypeName);
            return objectName;
        }
        final OracleSchema schema = dataSource.getSchema(monitor, objectOwner);
        if (schema == null) {
            log.debug("Schema '" + objectOwner + "' not found");
            return objectName;
        }
        final DBSObject object = objectType.findObject(monitor, schema, objectName);
        if (object == null) {
            log.debug(objectTypeName + " '" + objectName + "' not found in '" + schema.getName() + "'");
            return objectName;
        }
        return object;
    }

}
