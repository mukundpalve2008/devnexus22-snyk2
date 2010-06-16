/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSListener;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectAction;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor implements DBSDataSourceContainer, IObjectImageProvider, IAdaptable, IActionFilter
{
    static Log log = LogFactory.getLog(DataSourceDescriptor.class);

    private DriverDescriptor driver;
    private DBPConnectionInfo connectionInfo;

    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private Date createDate;
    private Date updateDate;
    private Date loginDate;
    private DataSourcePreferenceStore preferenceStore;

    private final List<DBSListener> listeners = new ArrayList<DBSListener>();
    private DBPDataSource dataSource;

    private transient List<DBPDataSourceUser> users = new ArrayList<DBPDataSourceUser>();
    private transient Image iconNormal;
    private transient Image iconConnected;
    private transient Image iconError;
    private transient boolean connectFailed = false;


    public DataSourceDescriptor(
        DriverDescriptor driver,
        DBPConnectionInfo connectionInfo)
    {
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        this.createDate = new Date();
        this.preferenceStore = new DataSourcePreferenceStore(this);
    }

    public void dispose()
    {
        if (!listeners.isEmpty()) {
            log.warn("Data source container '" + this.getName() + "' listeners still registered [" + listeners.size() + "]");
            listeners.clear();
        }
        users.clear();
        iconNormal = null;
        if (iconConnected != null) {
            iconConnected.dispose();
            iconConnected = null;
        }
        if (iconError != null) {
            iconError.dispose();
            iconError = null;
        }
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(DBPConnectionInfo connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    public boolean isShowSystemObjects()
    {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects)
    {
        this.showSystemObjects = showSystemObjects;
    }

    public DBSObject getParentObject()
    {
        return null;
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.isConnected()) {
            this.reconnect(monitor, this);
        }
        return true;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public Date getUpdateDate()
    {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate)
    {
        this.updateDate = updateDate;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public DBCSession getSession(DBRProgressMonitor monitor, boolean forceNew) throws DBException
    {
        if (!isConnected()) {
            connect(this);
        }
        return getDataSource().getSession(monitor, forceNew);
    }

    public DBeaverCore getViewCallback()
    {
        return driver.getProviderDescriptor().getRegistry().getCore();
    }

    public boolean isConnected()
    {
        return dataSource != null;
    }

    public void connect(final Object source)
        throws DBException
    {
        if (this.isConnected()) {
            return;
        }
        if (!CommonUtils.isEmpty(Job.getJobManager().find(this))) {
            // Already connecting/disconnecting - jsut return
            return;
        }
        final String oldName = this.getConnectionInfo().getUserName();
        final String oldPassword = this.getConnectionInfo().getUserPassword();
        if (!this.isSavePassword()) {
            // Ask for password
            if (!askForPassword()) {
                throw new DBException("Authentification canceled");
            }
        }

        ConnectJob connectJob = new ConnectJob(this);
        connectJob.addJobChangeListener(new JobChangeAdapter() {
            public void done(IJobChangeEvent event)
            {
                if (event.getResult().isOK()) {
                    connectFailed = false;
                    DataSourceDescriptor.this.dataSource = ((ConnectJob)event.getJob()).getDataSource();
                    if (DataSourceDescriptor.this.dataSource == null) {
                        log.error("Null datasource returned from connector");
                    }
                    if (!isSavePassword()) {
                        // Rest password back to null
                        getConnectionInfo().setUserName(oldName);
                        getConnectionInfo().setUserPassword(oldPassword);
                    }
                    getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                        DataSourceEvent.Action.CONNECT,
                        DataSourceDescriptor.this,
                        source);
                } else {
                    connectFailed = true;
                    getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                        DataSourceEvent.Action.CONNECT_FAIL,
                        DataSourceDescriptor.this,
                        source);
                }
            }
        });
        connectJob.schedule();
    }

    public void disconnect(final Object source)
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return;
        }
        if (!CommonUtils.isEmpty(Job.getJobManager().find(this))) {
            // Already connecting/disconnecting - just return
            return;
        }

/*
        if (!users.isEmpty()) {
            log.info("Can't close datasource connection: there are " + users.size() + " active user(s)");
            return;
        }
*/
        DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress()
        {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                try {
                    Job.getJobManager().join(dataSource, monitor.getNestedMonitor());
                } catch (Exception e) {
                    log.error(e);
                    return;
                }

                DisconnectJob disconnectJob = new DisconnectJob(dataSource);
                disconnectJob.addJobChangeListener(new JobChangeAdapter() {
                    public void done(IJobChangeEvent event)
                    {
                        dataSource = null;
                        getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                            DataSourceEvent.Action.DISCONNECT,
                            DataSourceDescriptor.this,
                            source);
                    }
                });
                disconnectJob.schedule();
            }
        });
    }

    public void invalidate()
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return;
        }
        DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress()
        {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                try {
                    dataSource.checkConnection(monitor);
                }
                catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

    public void acquire(DBPDataSourceUser user)
    {
        users.add(user);
    }

    public void release(DBPDataSourceUser user)
    {
        users.remove(user);
    }

    public void addListener(DBSListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(DBSListener listener)
    {
        synchronized (listeners) {
            if (!listeners.remove(listener)) {
                log.warn("Listener [" + listener + "] is not registered in data source container '" + this.getName() + "'");
            }
        }
    }

    public void fireEvent(DBSObjectAction action, DBSObject object)
    {
        List<DBSListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<DBSListener>(listeners);
        }
        for (DBSListener listener : listenersCopy) {
            listener.handleObjectEvent(action, object);
        }
    }

    public AbstractPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void reconnect(DBRProgressMonitor monitor, Object source)
        throws DBException
    {
        this.disconnect(source);
        this.connect(source);
    }

    public void resetPassword()
    {
        if (connectionInfo != null) {
            connectionInfo.setUserPassword(null);
        }
    }

    public Object getAdapter(Class adapter)
    {
        if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return this;
        } else if (adapter == IPropertySource.class) {
            DBPDataSourceInfo info = null;
            if (this.isConnected()) {
                info = this.getDataSource().getInfo();
            }
            StringBuilder addr = new StringBuilder();
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                addr.append(connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                addr.append(':').append(connectionInfo.getHostPort());
            }

            PropertyCollector props = new PropertyCollector(this, false);
            props.addProperty("driverType", "Driver Type", driver.getName());
            if (info != null) {
                //props.addProperty("driverName", "Driver Name", info.getDriverName() + " " + info.getDriverVersion());
            }
            props.addProperty("address", "Address", addr.toString());
            props.addProperty("database", "Database Name", connectionInfo.getDatabaseName());
            if (info != null) {
                //props.addProperty("databaseType", "Database Type", info.getDatabaseProductName() + " " + info.getDatabaseProductVersion());
            }
            props.addProperty("url", "URL", connectionInfo.getJdbcURL());
            return props;
        }/* else if (adapter == IWorkbenchAdapter.class) {
            return new IWorkbenchAdapter() {

                public Object[] getChildren(Object o)
                {
                    return null;
                }

                public ImageDescriptor getImageDescriptor(Object object)
                {
                    return ImageDescriptor.createFromImage(driver.getIcon());
                }

                public String getLabel(Object o)
                {
                    return "DataSource " + getName();
                }

                public Object getParent(Object o)
                {
                    return null;
                }
            };
        }*/
        return null;
    }

    public boolean askForPassword()
    {
        ConnectionAuthDialog auth = new ConnectionAuthDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            this);
        int result = auth.open();
        if (result == IDialogConstants.OK_ID) {
            if (isSavePassword()) {
                // Update connection properties
                getDriver().getProviderDescriptor().getRegistry().updateDataSource(this);
            }
            return true;
        }
        return false;
    }

    public boolean testAttribute(Object target, String name, String value)
    {
        if (name.equals("connected")) {
            return String.valueOf(this.isConnected()).equals(value);
        } else if (name.equals("savePassword")) {
            return String.valueOf(this.isSavePassword()).equals(value);
        }
        return false;
    }

    public Image getObjectImage()
    {
        if (iconNormal == null) {
            iconNormal = driver.getIcon();

            // Create overlay image for ocnnected icon
            {
                OverlayImageDescriptor connectedDescriptor = new OverlayImageDescriptor(iconNormal.getImageData());
                connectedDescriptor.setBottomRight(new ImageDescriptor[] {DBIcon.OVER_SUCCESS.getImageDescriptor()} );
                iconConnected = new Image(iconNormal.getDevice(), connectedDescriptor.getImageData());
            }
            {
                OverlayImageDescriptor failedDescriptor = new OverlayImageDescriptor(iconNormal.getImageData());
                failedDescriptor.setBottomRight(new ImageDescriptor[] {DBIcon.OVER_ERROR.getImageDescriptor()} );
                iconError = new Image(iconNormal.getDevice(), failedDescriptor.getImageData());
            }
        }
        if (isConnected()) {
            return iconConnected;
        } else if (connectFailed) {
            return iconError;
        } else {
            return iconNormal;
        }
    }

}
