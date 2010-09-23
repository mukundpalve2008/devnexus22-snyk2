/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;

/**
 * GridDataProvider
 */
public interface ISpreadsheetController {

    boolean isEditable();

    boolean isCellEditable(int col, int row);

    boolean isInsertable();

    boolean showCellEditor(
        GridPos cell,
        boolean inline,
        Composite inlinePlaceholder);

    void fillContextMenu(
        GridPos cell,
        IMenuManager manager);

}
