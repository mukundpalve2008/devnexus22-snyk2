/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.SWT;
import org.eclipse.jface.action.IMenuManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {


    public final Object getValueObject(DBCResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException
    {
        try {
            return getValueObject((ResultSet) resultSet.getNestedResultSet(), columnType, columnIndex + 1);
        }
        catch (SQLException e) {
            throw new DBCException("Could not get result set value", e);
        }
    }

    public final void bindParameter(DBCStatement statement, DBSTypedObject columnMetaData, int paramIndex, Object value) throws DBCException {
        try {
            this.bindParameter((PreparedStatement) statement.getNestedStatement(), columnMetaData, paramIndex + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException("Could not bind statement parameter", e);
        }
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException
    {
        return null;
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {

    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "column_size",
            "Column Size",
            controller.getColumnMetaData().getDisplaySize());
    }

    protected static interface ValueExtractor <T extends Control> {
         Object getValueFromControl(T control);
    }

    protected <T extends Control> void initInlineControl(
        final DBDValueController controller,
        final T control,
        final ValueExtractor<T> extractor)
    {
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        control.setFont(controller.getInlinePlaceholder().getFont());
        control.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.closeInlineEditor();
                } else if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT) {
                    if ((e.stateMask & SWT.ALT) != 0) {
                        controller.updateValue(extractor.getValueFromControl(control));
                        controller.nextInlineEditor(e.keyCode == SWT.ARROW_RIGHT);
                    }
                } else if (e.keyCode == SWT.ESC) {
                    controller.closeInlineEditor();
                }
            }
            public void keyReleased(KeyEvent e)
            {
            }
        });
    }

    protected abstract Object getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException;

    protected abstract void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException;
}