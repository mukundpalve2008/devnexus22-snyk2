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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCIndexManager;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.dialogs.struct.EditIndexDialog;

import java.util.Collections;

/**
 * Oracle index manager
 */
public class OracleIndexManager extends JDBCIndexManager<OracleTableIndex, OracleTablePhysical> {

    @Override
    protected OracleTableIndex createDatabaseObject(
        IWorkbenchWindow workbenchWindow,
        IEditorPart activeEditor,
        DBECommandContext context, OracleTablePhysical parent,
        Object from)
    {
        EditIndexDialog editDialog = new EditIndexDialog(
            workbenchWindow.getShell(),
            OracleMessages.edit_oracle_index_manager_dialog_title,
            parent,
            Collections.singletonList(DBSIndexType.OTHER));
        if (editDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        StringBuilder idxName = new StringBuilder(64);
        idxName.append(CommonUtils.escapeIdentifier(parent.getName())).append("_") //$NON-NLS-1$
            .append(CommonUtils.escapeIdentifier(editDialog.getSelectedColumns().iterator().next().getName()))
            .append("_IDX"); //$NON-NLS-1$
        final OracleTableIndex index = new OracleTableIndex(
            parent,
            DBObjectNameCaseTransformer.transformName((DBPDataSource) parent.getDataSource(), idxName.toString()),
            false,
            editDialog.getIndexType());
        int colIndex = 1;
        for (DBSTableColumn tableColumn : editDialog.getSelectedColumns()) {
            index.addColumn(
                new OracleTableIndexColumn(
                    index,
                    (OracleTableColumn) tableColumn,
                    colIndex++,
                    true));
        }
        return index;
    }

    @Override
    protected String getDropIndexPattern(OracleTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
