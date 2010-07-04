/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * SQLTableData
 */
public class DatabaseDataEditor extends EditorPart implements IEmbeddedWorkbenchPart, IMetaModelView, ResultSetProvider
{
    static Log log = LogFactory.getLog(DatabaseDataEditor.class);

    private ResultSetViewer resultSetView;
    private DBMModel model;
    private DBSDataContainer dataContainer;

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
    }

    public void dispose()
    {
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
        resultSetView = new ResultSetViewer(parent, getSite(), this);
    }

    public void setFocus()
    {
    }

    public void activatePart()
    {
        if (dataContainer == null) {
            IEditorInput editorInput = getEditorInput();
            if (!(editorInput instanceof IDatabaseEditorInput)) {
                log.error("Table data editor must be used only with databse editor input!");
                return;
            }
            IDatabaseEditorInput dbei = (IDatabaseEditorInput)editorInput;
            model = dbei.getModel();
            DBSObject object = dbei.getDatabaseObject();
            if (!(object instanceof DBSDataContainer)) {
                log.error("Data editor supports only data contaner objects!");
                return;
            }
            dataContainer = (DBSDataContainer) object;

            resultSetView.refresh();
        }
    }

    public void deactivatePart()
    {
/*
        if (curSession != null) {
            try {
                curSession.close();
            } catch (DBCException ex) {
                log.error("Error closing session", ex);
            }
            curSession = null;
        }
*/
    }

    public DBMModel getMetaModel()
    {
        return model;
    }

    public Viewer getViewer()
    {
        return resultSetView;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        return dataContainer.getDataSource().getContainer();
    }

    public DBPDataSource getDataSource() {
        return dataContainer.getDataSource();
    }

    public boolean isConnected()
    {
        return dataContainer != null && dataContainer.getDataSource().getContainer().isConnected();
    }

    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    public void extractResultSetData(int offset)
    {
        if (!isConnected()) {
            DBeaverUtils.showErrorDialog(getSite().getShell(), "Not Connected", "Not Connected");
            return;
        }

        new DataPumpJob().schedule();
    }

    private class DataPumpJob extends DataSourceJob {

        protected DataPumpJob()
        {
            super("Pump data from " + getDataContainer().getName(), DBIcon.SQL_EXECUTE.getImageDescriptor(), DatabaseDataEditor.this.getDataSource());
        }

        protected IStatus run(DBRProgressMonitor monitor)
        {
            IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
            int maxRows = preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);

            String statusMessage;
            boolean hasErrors = false;
            try {
                int rowCount = getDataContainer().readData(monitor, resultSetView.getDataReciever(), 0, maxRows);
                if (rowCount > 0) {
                    statusMessage = rowCount + " row(s)";
                } else {
                    statusMessage = "Empty resultset";
                }
            }
            catch (DBException e) {
                statusMessage = e.getMessage();
                hasErrors = true;
                log.error(e);
            }

            {
                // Set status
                final String message = statusMessage;
                final boolean isError = hasErrors;
                getSite().getShell().getDisplay().syncExec(new Runnable() {
                    public void run()
                    {
                        resultSetView.setStatus(message, isError);
                    }
                });
            }
            return Status.OK_STATUS;
        }
    }

}
