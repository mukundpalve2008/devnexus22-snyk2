/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDRowController;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCTableIdentifier;
import org.jkiss.dbeaver.model.dbc.DBCTableMetaData;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.sql.DefaultQueryListener;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryResult;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.runtime.sql.SQLStatementParameter;
import org.jkiss.dbeaver.runtime.sql.SQLStatementType;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ThemeConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Arrays;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements ISpreadsheetController, IPropertyChangeListener
{
    static Log log = LogFactory.getLog(ResultSetViewer.class);

    private static final int DEFAULT_ROW_HEADER_WIDTH = 50;

    private IWorkbenchPartSite site;
    private ResultSetMode mode;
    private Spreadsheet spreadsheet;
    private ResultSetProvider resultSetProvider;
    private ResultSetDataReciever dataReciever;
    private IThemeManager themeManager;

    // columns
    private DBDColumnBinding[] metaColumns;
    // Data
    private final List<Object[]> curRows = new ArrayList<Object[]>();
    // Current row number (for record mode)
    private int curRowNum = -1;
    private boolean singleSourceCells;

    // Edited rows and cells
    private final Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private final Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private Set<CellInfo> editedValues = new HashSet<CellInfo>();

    private Label statusLabel;

    private ToolItem itemAccept;
    private ToolItem itemReject;

    private ToolItem itemRowEdit;
    private ToolItem itemRowAdd;
    private ToolItem itemRowCopy;
    private ToolItem itemRowDelete;

    private ToolItem itemToggleView;
    private ToolItem itemNext;
    private ToolItem itemPrevious;
    private ToolItem itemFirst;
    private ToolItem itemLast;
    private ToolItem itemRefresh;

    private Map<ResultSetValueController, DBDValueEditor> openEditors = new HashMap<ResultSetValueController, DBDValueEditor>();
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;

    // UI modifiers
    private Color colorRed;
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color foregroundNull;

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, ResultSetProvider resultSetProvider)
    {
        super();
        this.site = site;
        this.mode = ResultSetMode.GRID;

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        this.backgroundAdded = new Color(parent.getDisplay(), 0xE4, 0xFF, 0xB5);
        this.backgroundDeleted = new Color(parent.getDisplay(), 0xFF, 0x63, 0x47);
        this.backgroundModified = new Color(parent.getDisplay(), 0xFF, 0xE4, 0xB5);
        this.foregroundNull = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);

        this.spreadsheet = new Spreadsheet(
            parent,
            SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
            site,
            this,
            new ContentProvider(),
            new ContentLabelProvider(),
            new ColumnLabelProvider(),
            new RowLabelProvider());

        createStatusBar(spreadsheet);
        changeMode(ResultSetMode.GRID);
        this.resultSetProvider = resultSetProvider;
        this.dataReciever = new ResultSetDataReciever(this);

        this.themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeManager.addPropertyChangeListener(this);
        this.spreadsheet.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        this.spreadsheet.addCursorChangeListener(new Listener() {
            public void handleEvent(Event event)
            {
                onChangeGridCursor(event.x, event.y);
            }
        });

        applyThemeSettings();
    }

    private void updateGridCursor()
    {
        GridPos point = spreadsheet.getCursorPosition();
        if (point == null) {
            onChangeGridCursor(0, 0);
        } else {
            onChangeGridCursor(point.col, point.row);
        }
    }

    private void onChangeGridCursor(int col, int row)
    {
        if (mode == ResultSetMode.GRID) {
            int rowsNum = spreadsheet.getItemCount();
            //int colsNum = spreadsheet.getColumnsCount();
            boolean isFirst = (row <= 0);
            boolean isLast = rowsNum == 0 || (row >= rowsNum - 1);

            boolean isVisible = col >= 0;
            itemFirst.setEnabled(isVisible && !isFirst);
            itemPrevious.setEnabled(isVisible && !isFirst);
            itemNext.setEnabled(isVisible && !isLast);
            itemLast.setEnabled(isVisible && !isLast);
            itemRefresh.setEnabled(resultSetProvider != null && resultSetProvider.isConnected());
        }

        boolean validPosition;
        if (mode == ResultSetMode.GRID) {
            validPosition = (col >= 0 && row >= 0);
        } else {
            validPosition = curRowNum >= 0;
        }
        itemRowEdit.setEnabled(validPosition);
        itemRowAdd.setEnabled(singleSourceCells && !CommonUtils.isEmpty(metaColumns));
        itemRowCopy.setEnabled(singleSourceCells && validPosition);
        itemRowDelete.setEnabled(singleSourceCells && validPosition);
    }

    private void updateRecord()
    {
        boolean isFirst = curRowNum <= 0;
        boolean isLast = curRows.isEmpty() || (curRowNum >= curRows.size() - 1);

        itemFirst.setEnabled(!isFirst);
        itemPrevious.setEnabled(!isFirst);
        itemNext.setEnabled(!isLast);
        itemLast.setEnabled(!isLast);

        this.initResultSet();
    }

    private void updateEditControls()
    {
        boolean hasChanges = !editedValues.isEmpty() || !addedRows.isEmpty() || !removedRows.isEmpty();
        itemAccept.setEnabled(hasChanges);
        itemReject.setEnabled(hasChanges);
    }

    private void refreshSpreadsheet(boolean rowsChanged)
    {
        if (rowsChanged) {
            GridPos curPos = spreadsheet.getCursorPosition();
            if (mode == ResultSetMode.RECORD) {
                if (curRowNum >= curRows.size()) {
                    curRowNum = curRows.size() - 1;
                }
            } else if (mode == ResultSetMode.GRID) {
                if (curPos.row >= curRows.size()) {
                    curPos.row = curRows.size() - 1;
                }
            }

            this.spreadsheet.reinitState();

            // Set cursor on new row
            if (mode == ResultSetMode.GRID) {
                spreadsheet.setCursor(curPos, false);
            }

        } else {
            this.spreadsheet.redrawGrid();
        }
        updateEditControls();
    }

    private void createStatusBar(Composite parent)
    {
        Composite statusBar = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 5;
        gl.marginHeight = 0;
        statusBar.setLayout(gl);
        
        statusLabel = new Label(statusBar, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        statusLabel.setLayoutData(gd);

        {
            ToolBar toolBar = new ToolBar(statusBar, SWT.FLAT | SWT.HORIZONTAL);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            toolBar.setLayoutData(gd);

            itemAccept = UIUtils.createToolItem(toolBar, "Apply changes", DBIcon.ACCEPT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    applyChanges();
                }
            });
            itemReject = UIUtils.createToolItem(toolBar, "Reject changes", DBIcon.REJECT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    rejectChanges();
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);

            itemRowEdit = UIUtils.createToolItem(toolBar, "Edit cell", DBIcon.ROW_EDIT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    showCellEditor(spreadsheet.getCursorPosition(), false, null);
                }
            });
            itemRowAdd = UIUtils.createToolItem(toolBar, "Add row", DBIcon.ROW_ADD, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    addNewRow(false);
                }
            });
            itemRowCopy = UIUtils.createToolItem(toolBar, "Copy row", DBIcon.ROW_COPY, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    addNewRow(true);
                }
            });
            itemRowDelete = UIUtils.createToolItem(toolBar, "Delete row", DBIcon.ROW_DELETE, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    deleteCurrentRow();
                }
            });

            new ToolItem(toolBar, SWT.SEPARATOR);

            itemToggleView = UIUtils.createToolItem(toolBar, "Toggle View", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    changeMode(mode == ResultSetMode.GRID ? ResultSetMode.RECORD : ResultSetMode.GRID);
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);
            itemFirst = UIUtils.createToolItem(toolBar, "First", DBIcon.RS_FIRST, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD) {
                        curRowNum = 0;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                    }
                }
            });
            itemPrevious = UIUtils.createToolItem(toolBar, "Previous", DBIcon.RS_PREV, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum > 0) {
                        curRowNum--;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, -1, false);
                    }
                }
            });
            itemNext = UIUtils.createToolItem(toolBar, "Next", DBIcon.RS_NEXT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum < curRows.size() - 1) {
                        curRowNum++;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, 1, false);
                    }
                }
            });
            itemLast = UIUtils.createToolItem(toolBar, "Last", DBIcon.RS_LAST, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && !curRows.isEmpty()) {
                        curRowNum = curRows.size() - 1;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                    }
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);
            itemRefresh = UIUtils.createToolItem(toolBar, "Refresh", DBIcon.RS_REFRESH, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    refresh();
                }
            });
        }
        updateEditControls();
    }

    private void changeMode(ResultSetMode resultSetMode)
    {
        this.mode = resultSetMode;
        if (mode == ResultSetMode.GRID) {
            spreadsheet.setRowHeaderWidth(DEFAULT_ROW_HEADER_WIDTH);
            itemToggleView.setImage(DBIcon.RS_MODE_GRID.getImage());
        } else {
            // Calculate width of spreadsheet panel - use longest column title
            int defaultWidth = 0;
            if (metaColumns != null) {
                GC gc = new GC(spreadsheet);
                gc.setFont(spreadsheet.getFont());
                for (DBDColumnBinding column : metaColumns) {
                    Point ext = gc.stringExtent(column.getMetaData().getColumnName());
                    if (ext.x > defaultWidth) {
                        defaultWidth = ext.x;
                    }
                }
                defaultWidth += DBIcon.EDIT_COLUMN.getImage().getBounds().width + 2;
            }
            spreadsheet.setRowHeaderWidth(defaultWidth + DEFAULT_ROW_HEADER_WIDTH);
            itemToggleView.setImage(DBIcon.RS_MODE_RECORD.getImage());
            GridPos curPos = spreadsheet.getCursorPosition();
            if (curPos != null) {
                curRowNum = curPos.row;
                if (curRowNum < 0) {
                    curRowNum = 0;
                }
            } else {
                curRowNum = 0;
            }
            updateRecord();
        }

        this.initResultSet();

        if (mode == ResultSetMode.GRID) {
            if (curRowNum >= 0) {
                spreadsheet.setCursor(new GridPos(0, curRowNum), false);
            }
        }
        spreadsheet.layout(true, true);
    }

    public ResultSetProvider getResultSetProvider()
    {
        return resultSetProvider;
    }

    public ResultSetDataReciever getDataReciever()
    {
        return dataReciever;
    }

    public void dispose()
    {
        closeEditors();

        if (!spreadsheet.isDisposed()) {
            spreadsheet.dispose();
        }
        itemAccept.dispose();
        itemReject.dispose();
        itemRowEdit.dispose();
        itemRowAdd.dispose();
        itemRowCopy.dispose();
        itemRowDelete.dispose();
        itemToggleView.dispose();
        itemNext.dispose();
        itemPrevious.dispose();
        itemFirst.dispose();
        itemLast.dispose();
        itemRefresh.dispose();
        statusLabel.dispose();
        themeManager.removePropertyChangeListener(ResultSetViewer.this);
    }

    private void applyThemeSettings()
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Font rsFont = currentTheme.getFontRegistry().get(ThemeConstants.FONT_SQL_RESULT_SET);
        if (rsFont != null) {
            this.spreadsheet.setFont(rsFont);
        }
        Color selBackColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
        if (selBackColor != null) {
            this.spreadsheet.setBackgroundSelected(selBackColor);
        }
        Color selForeColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE);
        if (selForeColor != null) {
            this.spreadsheet.setForegroundSelected(selForeColor);
        }
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)
            || event.getProperty().equals(ThemeConstants.FONT_SQL_RESULT_SET)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE))
        {
            applyThemeSettings();
        }
    }

    static boolean isColumnReadOnly(DBDColumnBinding column)
    {
        return
            column.getValueLocator() != null &&
            column.getValueLocator().getTable() instanceof DBSDataContainer;
    }

    public int getRowsCount()
    {
        return curRows.size();
    }

    int getRowIndex(Object[] row)
    {
        return curRows.indexOf(row);
    }

    public void setStatus(String status)
    {
        setStatus(status, false);
    }

    public void setStatus(String status, boolean error)
    {
        if (error) {
            statusLabel.setForeground(colorRed);
        } else {
            statusLabel.setForeground(null);
        }
        statusLabel.setText(status);
    }

    public void setColumnsInfo(DBDColumnBinding[] metaColumns)
    {
        this.metaColumns = metaColumns;
    }

    public void setData(List<Object[]> rows)
    {
        this.editedValues.clear();
        this.addedRows.clear();
        this.removedRows.clear();

        this.curRows.clear();
        this.curRows.addAll(rows);

        // Check single source flag
        this.singleSourceCells = true;
        DBSTable sourceTable = null;
        for (DBDColumnBinding column : metaColumns) {
            if (isColumnReadOnly(column)) {
                singleSourceCells = false;
                break;
            }
            if (sourceTable == null) {
                sourceTable = column.getValueLocator().getTable();
            } else if (sourceTable != column.getValueLocator().getTable()) {
                singleSourceCells = false;
                break;
            }
        }

        this.initResultSet();
    }

    public void appendData(List<Object[]> rows)
    {
        curRows.addAll(rows);
    }

    private void clearResultsView()
    {
        // Clear previous state
        spreadsheet.setRedraw(false);
        spreadsheet.clearGrid();
        spreadsheet.setRedraw(true);
    }

    private void closeEditors() {
        List<DBDValueEditor> editors = new ArrayList<DBDValueEditor>(openEditors.values());
        for (DBDValueEditor editor : editors) {
            editor.closeValueEditor();
        }
        if (!openEditors.isEmpty()) {
            log.warn("Some value editors are still registered at resulset: " + openEditors.size());
        }
        openEditors.clear();
    }

    private void initResultSet()
    {
        closeEditors();

        spreadsheet.setRedraw(false);
        spreadsheet.clearGrid();
        if (mode == ResultSetMode.RECORD) {
            this.showCurrentRows();
        } else {
            this.showRowsCount();
        }

        spreadsheet.reinitState();
        spreadsheet.setRedraw(true);

        this.updateGridCursor();
    }

    public boolean isEditable()
    {
        return !updateInProgress;
    }

    public boolean isCellEditable(int col, int row) {
        return true;
    }

    public boolean isCellModified(int col, int row) {
        return 
            !editedValues.isEmpty() &&
                editedValues.contains(new CellInfo(col, row, null));
    }

    public boolean isInsertable()
    {
        return false;
    }

    public boolean showCellEditor(
        final GridPos cell,
        final boolean inline,
        final Composite inlinePlaceholder)
    {
        final int columnIndex = (mode == ResultSetMode.GRID ? cell.col : cell.row);
        final int rowIndex = (mode == ResultSetMode.GRID ? cell.row : curRowNum);
        if (!inline) {
            GridPos testCell = new GridPos(columnIndex, rowIndex);
            for (ResultSetValueController valueController : openEditors.keySet()) {
                GridPos cellPos = valueController.getCellPos();
                if (cellPos != null && cellPos.equalsTo(testCell)) {
                    openEditors.get(valueController).showValueEditor();
                    return true;
                }
            }
        }
        DBDColumnBinding metaColumn = metaColumns[columnIndex];
        if (isColumnReadOnly(metaColumn) && inline) {
            // No inline editors for readonly columns
            return false;
        }
        ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            inline ? inlinePlaceholder : null);
        try {
            return metaColumn.getValueHandler().editValue(valueController);
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public void fillContextMenu(final GridPos cell, IMenuManager manager) {

        // Custom value items
        final int columnIndex = (mode == ResultSetMode.GRID ? cell.col : cell.row);
        final int rowIndex = (mode == ResultSetMode.GRID ? cell.row : curRowNum);
        if (rowIndex < 0 || curRows.size() <= rowIndex || columnIndex < 0 || columnIndex >= metaColumns.length) {
            return;
        }
        final ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            null);
        final Object value = valueController.getValue();

        // Standard items
        manager.add(new Separator());
        manager.add(new Action("Edit ...") {
            @Override
            public void run()
            {
                showCellEditor(cell, false, null);
            }
        });
        if (!DBUtils.isNullValue(value)) {
            manager.add(new Action("Set to NULL") {
                @Override
                public void run()
                {
                    valueController.updateValue(DBUtils.makeNullValue(value));
                }
            });
        }

        try {
            manager.add(new Separator());
            metaColumns[columnIndex].getValueHandler().fillContextMenu(manager, valueController);
        }
        catch (Exception e) {
            log.error(e);
        }
    }

    private void showCurrentRows()
    {
        setStatus("Row " + (curRowNum + 1));
    }

    private void showRowsCount()
    {
        setStatus(
            String.valueOf(curRows.size()) +
                " row" + (curRows.size() > 1 ? "s" : "") + " fetched");
    }

    public Control getControl()
    {
        return spreadsheet;
    }

    public Object getInput()
    {
        return null;
    }

    public void setInput(Object input)
    {
    }

    public ISelection getSelection()
    {
        return new StructuredSelection(spreadsheet.getSelection().toArray());
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh all rows
        this.editedValues.clear();

        this.curRows.clear();
        this.clearResultsView();
        if (resultSetProvider != null) {
            resultSetProvider.extractResultSetData(0);
        }
        updateGridCursor();
    }

    private void applyChanges()
    {
        try {
            new CellDataSaver(addedRows, removedRows, editedValues).applyChanges(null);
        } catch (DBException e) {
            log.error("Could not obtain result set metdata", e);
        }
    }

    private void rejectChanges()
    {
        new CellDataSaver(addedRows, removedRows, editedValues).rejectChanges();
    }

    private boolean isRowAdded(int row)
    {
        return addedRows.contains(new RowInfo(row));
    }

    private void addNewRow(boolean copyCurrent)
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        int rowNum;
        if (mode == ResultSetMode.RECORD) {
            rowNum = this.curRowNum;
        } else {
            rowNum = curPos.row;
        }
        if (rowNum < 0) {
            rowNum = 0;
        }
        shiftRows(rowNum, 1);

        // Add new row
        Object[] cells = new Object[metaColumns.length];
        if (copyCurrent) {
            // Copy cell values
            Object[] origRow = curRows.get(rowNum);
            for (int i = 0; i < metaColumns.length; i++) {
                cells[i] = metaColumns[i].getValueHandler().copyValueObject(origRow[i]);
            }
        }
        curRows.add(rowNum, cells);

        addedRows.add(new RowInfo(rowNum));
        refreshSpreadsheet(true);
    }

    private void shiftRows(int rowNum, int delta)
    {
        // Slide all existing edited rows/cells down
        for (CellInfo cell : editedValues) {
            if (cell.row >= rowNum) cell.row += delta;
        }
        for (RowInfo row : addedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
        for (RowInfo row : removedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
    }

    private boolean isRowDeleted(int row)
    {
        return removedRows.contains(new RowInfo(row));
    }

    private void deleteCurrentRow()
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        int rowNum;
        if (mode == ResultSetMode.RECORD) {
            rowNum = this.curRowNum;
        } else {
            rowNum = curPos.row;
        }

        RowInfo rowInfo = new RowInfo(rowNum);
        if (addedRows.contains(rowInfo)) {
            // Remove just added row 
            addedRows.remove(rowInfo);
            curRows.remove(rowNum);
            shiftRows(rowNum, -1);

            refreshSpreadsheet(true);

        } else {
            // Mark row as deleted
            removedRows.add(rowInfo);
        }
        spreadsheet.redrawGrid();
        updateEditControls();
    }

    private Image getColumnImage(DBDColumnBinding column)
    {
        if (column.getMetaData() instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)column.getMetaData()).getObjectImage();
        } else {
            return DBIcon.TREE_COLUMN.getImage();
        }
    }

    private class ResultSetValueController implements DBDValueController, DBDRowController {

        private Object[] curRow;
        private int columnIndex;
        private Composite inlinePlaceholder;

        private ResultSetValueController(Object[] curRow, int columnIndex, Composite inlinePlaceholder) {
            this.curRow = curRow;
            this.columnIndex = columnIndex;
            this.inlinePlaceholder = inlinePlaceholder;
        }

        public DBPDataSource getDataSource()
        {
            return resultSetProvider.getDataSource();
        }

        public DBDRowController getRow() {
            return this;
        }

        public DBCColumnMetaData getColumnMetaData()
        {
            return metaColumns[columnIndex].getMetaData();
        }

        public String getColumnId() {
            String dsName = getDataSource().getContainer().getName();
            String catalogName = getColumnMetaData().getCatalogName();
            String schemaName = getColumnMetaData().getSchemaName();
            String tableName = getColumnMetaData().getTableName();
            String columnName = getColumnMetaData().getColumnName();
            StringBuilder columnId = new StringBuilder(CommonUtils.escapeIdentifier(dsName));
            if (!CommonUtils.isEmpty(catalogName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(catalogName));
            }
            if (!CommonUtils.isEmpty(schemaName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(schemaName));
            }
            if (!CommonUtils.isEmpty(tableName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(tableName));
            }
            if (!CommonUtils.isEmpty(columnName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(columnName));
            }
            return columnId.toString();
        }

        public Object getValue()
        {
            return curRow[columnIndex];
        }

        public void updateValue(Object value)
        {
            Object oldValue = curRow[columnIndex];
            if (!CommonUtils.equalObjects(oldValue, value)) {
                int rowIndex = getRowIndex(curRow);
                if (rowIndex >= 0) {
                    if (!isRowAdded(rowIndex) && !isRowDeleted(rowIndex)) {
                        // Do not add edited cell for new/deleted rows 
                        CellInfo cell = new CellInfo(columnIndex, rowIndex, oldValue);
                        editedValues.add(cell);
                    }
                    curRow[columnIndex] = value;
                    // Update controls
                    site.getShell().getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            updateEditControls();
                        }
                    });
                }
            }
        }

        public void updateValueImmediately(Object value, ISQLQueryListener listener)
            throws DBException
        {
            // Update cell value
            Object oldValue = curRow[columnIndex];
            if (value instanceof DBDValue || !CommonUtils.equalObjects(oldValue, value)) {
                int rowIndex = getRowIndex(curRow);
                if (rowIndex >= 0) {
                    curRow[columnIndex] = value;

                    // Run update SQL
                    Set<CellInfo> cells = new HashSet<CellInfo>();
                    cells.add(new CellInfo(columnIndex, rowIndex, oldValue));
                    try {
                        new CellDataSaver(null, null, cells).applyChanges(listener);
                    }
                    catch (DBException e) {
                        // Rollback value
                        curRow[columnIndex] = oldValue;
                        throw e;
                    }
                }
            }
        }

        public DBDValueLocator getValueLocator()
        {
            return metaColumns[columnIndex].getValueLocator();
        }

        public DBDValueHandler getValueHandler()
        {
            return metaColumns[columnIndex].getValueHandler();
        }

        public boolean isInlineEdit()
        {
            return inlinePlaceholder != null;
        }

        public boolean isReadOnly()
        {
            return isColumnReadOnly(metaColumns[columnIndex]);
        }

        public IWorkbenchPartSite getValueSite()
        {
            return site;
        }

        public Composite getInlinePlaceholder()
        {
            return inlinePlaceholder;
        }

        public void closeInlineEditor()
        {
            spreadsheet.cancelInlineEditor();
        }

        public void nextInlineEditor(boolean next) {
            spreadsheet.cancelInlineEditor();
            spreadsheet.shiftCursor(next ? 1 : -1, 0, false);
            spreadsheet.openCellViewer(true);
        }

        public void registerEditor(DBDValueEditor editor) {
            openEditors.put(this, editor);
        }

        public void unregisterEditor(DBDValueEditor editor) {
            openEditors.remove(this);
        }

        public void showMessage(String message, boolean error)
        {
            setStatus(message, error);
        }

        public Collection<DBCColumnMetaData> getColumnsMetaData() {
            List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
            for (DBDColumnBinding column : metaColumns) {
                columns.add(column.getMetaData());
            }
            return columns;
        }

        public DBCColumnMetaData getColumnMetaData(DBCTableMetaData table, String columnName)
        {
            for (DBDColumnBinding column : metaColumns) {
                if (column.getMetaData().getTable() == table && column.getMetaData().getColumnName().equals(columnName)) {
                    return column.getMetaData();
                }
            }
            return null;
        }

        public Object getColumnValue(DBCColumnMetaData column)
        {
            for (int i = 0; i < metaColumns.length; i++) {
                DBDColumnBinding metaColumn = metaColumns[i];
                if (metaColumn.getMetaData() == column) {
                    return curRow[i];
                }
            }
            log.warn("Unknown column value requested: " + column);
            return null;
        }

        private GridPos getCellPos()
        {
            int rowIndex = getRowIndex(curRow);
            if (rowIndex >= 0) {
                return new GridPos(columnIndex, rowIndex);
            } else {
                return null;
            }
        }
    }

    private static class RowInfo implements Comparable<RowInfo> {
        int row;

        RowInfo(int row)
        {
            this.row = row;
        }
        @Override
        public int hashCode()
        {
            return row;
        }
        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof RowInfo && ((RowInfo)obj).row == row;
        }
        @Override
        public String toString()
        {
            return String.valueOf(row);
        }
        public int compareTo(RowInfo o)
        {
            return row - o.row;
        }
    }

    private static class CellInfo {
        int col;
        int row;
        Object value;

        private CellInfo(int col, int row, Object value) {
            this.col = col;
            this.row = row;
            this.value = value;
        }

        @Override
        public int hashCode()
        {
            return col ^ row;
        }

        public boolean equals (Object cell)
        {
            return cell instanceof CellInfo &&
                this.col == ((CellInfo)cell).col &&
                this.row == ((CellInfo)cell).row;
        }
        boolean equals (int col, int row)
        {
            return this.col == col && this.row == row;
        }
    }

    static class TableRowInfo {
        DBCTableMetaData table;
        DBCTableIdentifier id;
        List<CellInfo> tableCells = new ArrayList<CellInfo>();

        TableRowInfo(DBCTableMetaData table, DBCTableIdentifier id) {
            this.table = table;
            this.id = id;
        }
    }

    static class KeyValues {
        DBSTable table;
        List<DBDColumnValue> keyColumns;
    }

    static class UpdateValues extends KeyValues {
        List<DBDColumnValue> updateColumns;
    }
    
    private class CellDataSaver {

        private Set<CellInfo> cells;
        private Set<RowInfo> newRowSet;
        private Set<RowInfo> removedRowSet;
        private Map<Integer, Map<DBCTableMetaData, TableRowInfo>> updatedRows = new TreeMap<Integer, Map<DBCTableMetaData, TableRowInfo>>();
        private List<SQLStatementInfo> statements = new ArrayList<SQLStatementInfo>();

        private List<KeyValues> insertStatements = new ArrayList<KeyValues>();
        private List<KeyValues> deleteStatements = new ArrayList<KeyValues>();
        private List<KeyValues> updateStatements = new ArrayList<KeyValues>();

        private int updateCount = 0, insertCount = 0, deleteCount = 0;

        private CellDataSaver(Set<RowInfo> newRowSet, Set<RowInfo> removedRowSet, Set<CellInfo> cells)
        {
            this.cells = cells;
            this.newRowSet = newRowSet;
            this.removedRowSet = removedRowSet;
        }

        /**
         * Applies changes.
         * @throws DBException
         * @param listener
         */
        void applyChanges(ISQLQueryListener listener)
            throws DBException
        {
            prepareDeleteStatements();
            prepareInsertStatements();
            prepareUpdateStatements();
            execute(listener);
        }

        private int getMetaColumnIndex(DBCColumnMetaData column)
        {
            for (int i = 0; i < metaColumns.length; i++) {
                if (column == metaColumns[i].getMetaData()) {
                    return i;
                }
            }
            return -1;
        }

        private void prepareUpdateRows()
            throws DBException
        {
            if (this.cells == null) {
                return;
            }
            // Prepare rows
            for (CellInfo cell : this.cells) {
                Map<DBCTableMetaData, TableRowInfo> tableMap = updatedRows.get(cell.row);
                if (tableMap == null) {
                    tableMap = new HashMap<DBCTableMetaData, TableRowInfo>();
                    updatedRows.put(cell.row, tableMap);
                }

                DBDColumnBinding metaColumn = metaColumns[cell.col];
                DBCTableMetaData metaTable = metaColumn.getMetaData().getTable();
                TableRowInfo tableRowInfo = tableMap.get(metaTable);
                if (tableRowInfo == null) {
                    tableRowInfo = new TableRowInfo(metaTable, metaColumn.getValueLocator().getTableIdentifier());
                    tableMap.put(metaTable, tableRowInfo);
                }
                tableRowInfo.tableCells.add(cell);
            }
        }

        private void prepareDeleteStatements()
            throws DBException
        {
            if (this.removedRowSet == null) {
                return;
            }
            // Make delete statements
            for (RowInfo rowNum : removedRowSet) {
                DBCTableMetaData table = metaColumns[0].getMetaData().getTable();
                String tableName = table.getFullQualifiedName();
                List<SQLStatementParameter> parameters = new ArrayList<SQLStatementParameter>();
                StringBuilder query = new StringBuilder();
                query.append("DELETE FROM ").append(tableName).append(" WHERE ");

                List<? extends DBCColumnMetaData> keyColumns = metaColumns[0].getValueLocator().getKeyColumns();
                for (int i = 0; i < keyColumns.size(); i++) {
                    DBCColumnMetaData column = keyColumns.get(i);
                    if (i > 0) {
                        query.append(" AND ");
                    }
                    int colIndex = getMetaColumnIndex(column);
                    if (colIndex < 0) {
                        throw new DBCException("Can't find meta column for ID column " + column.getColumnName());
                    }

                    String columnName = DBUtils.getQuotedIdentifier(resultSetProvider.getDataSource(), column.getColumnName());
                    query.append(columnName).append("=?");
                    parameters.add(new SQLStatementParameter(
                        metaColumns[colIndex].getValueHandler(),
                        column,
                        parameters.size(),
                        curRows.get(rowNum.row)[colIndex]));
                }

                SQLStatementInfo statement = new SQLStatementInfo(query.toString(), parameters);
                statement.setOffset(rowNum.row);
                statement.setData(rowNum);
                statement.setType(SQLStatementType.DELETE);
                statements.add(statement);
            }
        }

        private void prepareInsertStatements()
            throws DBException
        {
            if (this.newRowSet == null) {
                return;
            }
            // Make insert statements
            for (RowInfo rowNum : newRowSet) {
                Object[] cellValues = curRows.get(rowNum.row);

                DBCTableMetaData table = metaColumns[0].getMetaData().getTable();
                String tableName = table.getFullQualifiedName();
                List<SQLStatementParameter> parameters = new ArrayList<SQLStatementParameter>();
                StringBuilder query = new StringBuilder();
                query.append("INSERT INTO ").append(tableName).append(" (");
                boolean hasKey = false;
                for (int i = 0; i < metaColumns.length; i++) {
                    if (DBUtils.isNullValue(cellValues[i])) {
                        // do not use null values
                        continue;
                    }
                    DBDColumnBinding column = metaColumns[i];
                    if (hasKey) query.append(",");
                    hasKey = true;
                    query.append(DBUtils.getQuotedIdentifier(resultSetProvider.getDataSource(), column.getMetaData().getColumnName()));
                }
                query.append(") VALUES (");
                hasKey = false;
                for (int i = 0; i < metaColumns.length; i++) {
                    if (DBUtils.isNullValue(cellValues[i])) {
                        continue;
                    }
                    if (hasKey) query.append(",");
                    hasKey = true;
                    query.append("?");
                }
                query.append(")");

                for (int i = 0; i < metaColumns.length; i++) {
                    if (DBUtils.isNullValue(cellValues[i])) {
                        continue;
                    }
                    DBDColumnBinding column = metaColumns[i];
                    parameters.add(new SQLStatementParameter(
                        column.getValueHandler(),
                        column.getMetaData(),
                        parameters.size(),
                        cellValues[i]));
                }

                SQLStatementInfo statement = new SQLStatementInfo(query.toString(), parameters);
                statement.setOffset(rowNum.row);
                statement.setData(rowNum);
                statement.setType(SQLStatementType.INSERT);
                statements.add(statement);
            }
        }

        private void prepareUpdateStatements()
            throws DBException
        {
            prepareUpdateRows();

            if (updatedRows == null) {
                return;
            }

            // Make statements
            for (Integer rowNum : updatedRows.keySet()) {
                Map<DBCTableMetaData, TableRowInfo> tableMap = updatedRows.get(rowNum);
                for (DBCTableMetaData table : tableMap.keySet()) {
                    TableRowInfo rowInfo = tableMap.get(table);

                    String tableName = rowInfo.table.getFullQualifiedName();
                    List<SQLStatementParameter> parameters = new ArrayList<SQLStatementParameter>();
                    StringBuilder query = new StringBuilder();
                    query.append("UPDATE ").append(tableName).append(" SET ");
                    for (int i = 0; i < rowInfo.tableCells.size(); i++) {
                        CellInfo cell = rowInfo.tableCells.get(i);
                        if (i > 0) {
                            query.append(",");
                        }
                        DBDColumnBinding metaColumn = metaColumns[cell.col];
                        String columnName = DBUtils.getQuotedIdentifier(resultSetProvider.getDataSource(), metaColumn.getMetaData().getColumnName());
                        query.append(columnName).append("=?");
                        parameters.add(new SQLStatementParameter(
                            metaColumn.getValueHandler(),
                            metaColumn.getMetaData(),
                            parameters.size(),
                            curRows.get(rowNum)[cell.col]));
                    }
                    query.append(" WHERE ");
                    Collection<? extends DBCColumnMetaData> idColumns = rowInfo.id.getResultSetColumns();
                    boolean firstCol = true;
                    for (DBCColumnMetaData idColumn : idColumns) {
                        if (!firstCol) {
                            query.append(" AND ");
                        }
                        String columnName = DBUtils.getQuotedIdentifier(resultSetProvider.getDataSource(), idColumn.getColumnName());
                        query.append(columnName).append("=?");
                        firstCol = false;

                        // Find meta column and add statement parameter
                        int columnIndex = getMetaColumnIndex(idColumn);
                        if (columnIndex < 0) {
                            throw new DBCException("Can't find meta column for ID column " + idColumn.getColumnName());
                        }
                        DBDColumnBinding metaColumn = metaColumns[columnIndex];
                        Object keyValue = curRows.get(rowNum)[columnIndex];
                        // Try to find old key value
                        for (CellInfo cell : this.cells) {
                            if (cell.equals(columnIndex, rowNum)) {
                                keyValue = cell.value;
                            }
                        }
                        // Add key parameter
                        parameters.add(new SQLStatementParameter(
                            metaColumn.getValueHandler(),
                            metaColumn.getMetaData(),
                            parameters.size(),
                            keyValue));
                    }

                    SQLStatementInfo statement = new SQLStatementInfo(query.toString(), parameters);
                    statement.setOffset(rowNum);
                    statement.setData(rowInfo);
                    statement.setType(SQLStatementType.UPDATE);
                    statements.add(statement);
                }
            }
        }

        private void execute(final ISQLQueryListener listener)
            throws DBException
        {
            // Execute statements
            SQLQueryJob executor = new SQLQueryJob(
                "Update ResultSet",
                resultSetProvider.getDataSource(),
                statements,
                null);
            executor.addQueryListener(new DefaultQueryListener() {
                @Override
                public void onStartJob() {
                    updateInProgress = true;
                    if (listener != null) listener.onStartJob();
                }

                @Override
                public void onEndQuery(SQLQueryResult result) {
                    Integer rowCount = result.getUpdateCount();
                    if (rowCount != null) {
                        switch (result.getStatement().getType()) {
                            case INSERT: insertCount += rowCount; break;
                            case DELETE: deleteCount += rowCount; break;
                            case UPDATE: updateCount += rowCount; break;
                            default: break;
                        }
                    }
/*
                    if (result.getError() == null) {
                        // Remove edited values
                        TableRowInfo rowInfo = (TableRowInfo)result.getStatement().getData();
                        if (rowInfo != null) {
                            for (CellInfo cell : rowInfo.tableCells) {
                                cells.remove(cell);
                            }
                        }
                    }
*/
                    if (listener != null) listener.onEndQuery(result);
                }

                @Override
                public void onEndJob(final boolean hasErrors) {
                    if (!hasErrors) {
                        if (cells != null) {
                            cells.clear();
                        }
                        if (newRowSet != null) {
                            newRowSet.clear();
                        }
                        final boolean rowsChanged = deleteRows(removedRowSet);

                        site.getShell().getDisplay().syncExec(new Runnable() {
                            public void run()
                            {
                                refreshSpreadsheet(rowsChanged);
                                setStatus("Instered: " + insertCount + " / Deleted: " + deleteCount + " / Updated: " + updateCount, false);
                            }
                        });
                    } else {
                        site.getShell().getDisplay().syncExec(new Runnable() {
                            public void run()
                            {
                                refreshSpreadsheet(false);
                                setStatus("Error synchronizing result set with database", true);
                            }
                        });
                    }
                    updateInProgress = false;
                    if (listener != null) listener.onEndJob(hasErrors);
                }
            });
            executor.schedule();
        }

        public void rejectChanges()
        {
            if (this.cells != null) {
                for (CellInfo cell : this.cells) {
                    ResultSetViewer.this.curRows.get(cell.row)[cell.col] = cell.value;
                }
                this.cells.clear();
            }
            boolean rowsChanged = deleteRows(this.newRowSet);
            // Remove deleted rows
            if (this.removedRowSet != null) {
                this.removedRowSet.clear();
            }

            refreshSpreadsheet(rowsChanged);
        }

        private boolean deleteRows(Set<RowInfo> rows)
        {
            if (rows != null && !rows.isEmpty()) {
                // Remove rows (in descending order to prevent concurrent modification errors)
                int[] rowsToRemove = new int[rows.size()];
                int i = 0;
                for (RowInfo rowNum : rows) rowsToRemove[i++] = rowNum.row;
                Arrays.sort(rowsToRemove);
                for (i = rowsToRemove.length; i > 0; i--) {
                    ResultSetViewer.this.curRows.remove(rowsToRemove[i - 1]);
                }
                rows.clear();
                return true;
            } else {
                return false;
            }
        }
    }

    private class ContentProvider implements IGridContentProvider {

        public GridPos getSize()
        {
            if (mode == ResultSetMode.RECORD) {
                return new GridPos(
                    1,
                    metaColumns == null ? 0 : metaColumns.length);
            } else {
                return new GridPos(
                    metaColumns == null ? 0 : metaColumns.length,
                    curRows.size());
            }
        }

        public Object[] getElements(Object inputElement)
        {
            return curRows.get(((Number)inputElement).intValue());
        }

        public void dispose()
        {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }
    }

    private class ContentLabelProvider extends LabelProvider implements IColorProvider {
        private Object getValue(Object element, boolean formatString)
        {
            GridPos cell = (GridPos)element;
            Object value;
            DBDValueHandler valueHandler;
            if (mode == ResultSetMode.RECORD) {
                // Fill record
                if (curRowNum >= curRows.size() || curRowNum < 0) {
                    log.warn("Bad current row number: " + curRowNum);
                    return null;
                }
                Object[] values = curRows.get(curRowNum);
                if (cell.row >= values.length) {
                    log.warn("Bad record row number: " + cell.row);
                    return null;
                }
                value = values[cell.row];
                valueHandler = metaColumns[cell.row].getValueHandler();
            } else {
                if (cell.row >= curRows.size()) {
                    log.warn("Bad grid row number: " + cell.row);
                    return null;
                }
                if (cell.col >= metaColumns.length) {
                    log.warn("Bad grid column number: " + cell.col);
                    return null;
                }
                value = curRows.get(cell.row)[cell.col];
                valueHandler = metaColumns[cell.col].getValueHandler();
            }
            if (formatString) {
                return valueHandler.getValueDisplayString(metaColumns[cell.col].getMetaData(), value);
            } else {
                return value;
            }
        }

        @Override
        public Image getImage(Object element)
        {
            return null;
        }

        @Override
        public String getText(Object element)
        {
            return String.valueOf(getValue(element, true));
        }

        public Color getForeground(Object element)
        {
            Object value = getValue(element, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                return null;
            }
        }

        public Color getBackground(Object element)
        {
            GridPos cell = (GridPos)element;
            int col = cell.col;
            int row = cell.row;
            if (mode == ResultSetMode.RECORD) {
                col = row;
                row = curRowNum;
            }
            if (isRowAdded(row)) {
                return backgroundAdded;
            }
            if (isRowDeleted(row)) {
                return backgroundDeleted;
            }
            if (isCellModified(col, row)) {
                return backgroundModified;
            }
            return null;
        }
    }

    private class ColumnLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.GRID) {
                int colNumber = ((Number)element).intValue();
                return getColumnImage(metaColumns[colNumber]);
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int colNumber = ((Number)element).intValue();
            if (mode == ResultSetMode.RECORD) {
                if (colNumber == 0) {
                    return "Value";
                } else {
                    log.warn("Bad column index: " + colNumber);
                    return null;
                }
            } else {
                DBDColumnBinding metaColumn = metaColumns[colNumber];
                return metaColumn.getMetaData().getLabel();
/*
                return CommonUtils.isEmpty(metaColumn.getMetaData().getTableName()) ?
                    metaColumn.getMetaData().getColumnName() :
                    metaColumn.getMetaData().getTableName() + "." + metaColumn.getMetaData().getColumnName();
*/
            }
        }
    }

    private class RowLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.RECORD) {
                int rowNumber = ((Number) element).intValue();
                return getColumnImage(metaColumns[rowNumber]);
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int rowNumber = ((Number) element).intValue();
            if (mode == ResultSetMode.RECORD) {
                return metaColumns[rowNumber].getMetaData().getColumnName();
            } else {
                return String.valueOf(rowNumber + 1);
            }
        }
    }
}
