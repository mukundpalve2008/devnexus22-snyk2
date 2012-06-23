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
package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;

public class CreateEntityDialog extends TrayDialog {

    private DBPDataSource dataSource;
    private String entityType;
    private String name;

    public CreateEntityDialog(Shell shell, DBPDataSource dataSource, String entityType)
    {
        super(shell);
        this.dataSource = dataSource;
        this.entityType = entityType;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_struct_create_entity_title + entityType);
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        Composite propsGroup = new Composite(group, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text nameText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_struct_create_entity_group_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                name = nameText.getText();
            }
        });
        return group;
    }

    public String getEntityName()
    {
        return DBObjectNameCaseTransformer.transformName(dataSource, name);
    }
}
