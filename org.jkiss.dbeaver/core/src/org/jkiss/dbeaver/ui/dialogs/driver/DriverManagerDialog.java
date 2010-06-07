/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.controls.DriverTreeControl;

import java.util.ArrayList;
import java.util.List;

/**
 * EditDriverDialog
 */
public class DriverManagerDialog extends Dialog implements ISelectionChangedListener, IDoubleClickListener {

    private DataSourceProviderDescriptor selectedProvider;
    private DriverDescriptor selectedDriver;

    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    private DriverTreeControl treeControl;

    public DriverManagerDialog(Shell shell)
    {
        super(shell);
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Driver Manager");
        getShell().setImage(DBeaverActivator.getImageDescriptor("/icons/driver_manager.png").createImage());

        Composite group = (Composite)super.createDialogArea(parent);
        GridLayout layout = (GridLayout)group.getLayout();
        layout.numColumns = 2;
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 300;
        group.setLayoutData(gd);

        {
            treeControl = new DriverTreeControl(group);
            treeControl.initDrivers(this);
        }
        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            layout = new GridLayout(1, true);
            buttonBar.setLayout(layout);
            buttonBar.setLayoutData(new GridData(GridData.FILL_BOTH));

            newButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            newButton.setText("&New");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            newButton.setLayoutData(gd);
            newButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    createDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            editButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            editButton.setText("&Edit ...");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            editButton.setLayoutData(gd);
            editButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    editDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            deleteButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            deleteButton.setText("&Delete");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            deleteButton.setLayoutData(gd);
            deleteButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    deleteDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });
        }
        return group;
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            true);
    }

    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            setReturnCode(OK);
            close();
        }
    }

    public void selectionChanged(SelectionChangedEvent event)
    {
        this.selectedDriver = null;
        this.selectedProvider = null;
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DriverDescriptor) {
                selectedDriver = (DriverDescriptor) selectedObject;
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                selectedProvider = (DataSourceProviderDescriptor)selectedObject;
            }
        }
        this.updateButtons();
    }

    public void doubleClick(DoubleClickEvent event)
    {
        if (selectedDriver != null) {
            editDriver();
        }
    }

    private void updateButtons()
    {
        newButton.setEnabled(selectedProvider != null && selectedProvider.isDriversManagable());
        editButton.setEnabled(selectedDriver != null);
        deleteButton.setEnabled(selectedDriver != null && selectedDriver.getProviderDescriptor().isDriversManagable());
    }

    private void createDriver()
    {
        if (selectedProvider != null) {
            EditDriverDialog dialog = new EditDriverDialog(getShell(), selectedProvider);
            if (dialog.open() == IDialogConstants.OK_ID) {
                treeControl.refresh(selectedProvider);
            }
        }
    }

    private void editDriver()
    {
        if (selectedDriver != null) {
            EditDriverDialog dialog = new EditDriverDialog(getShell(), selectedDriver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                // Do nothing
            }
        }
    }

    private void deleteDriver()
    {
        List<DataSourceDescriptor> usedDS = new ArrayList<DataSourceDescriptor>();
        for (DataSourceDescriptor ds : DataSourceRegistry.getDefault().getDataSources()) {
            if (ds.getDriver() == selectedDriver) {
                usedDS.add(ds);
            }
        }
        if (!usedDS.isEmpty()) {
            MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            StringBuilder message = new StringBuilder("Your can't delete driver '" + selectedDriver.getName() +"' because it's used by next data source(s):");
            for (DataSourceDescriptor ds : usedDS) {
                message.append("\n - ").append(ds.getName());
            }
            messageBox.setMessage(message.toString());
            messageBox.setText("Can't delete driver");
            messageBox.open();
            return;
        }
        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
        messageBox.setMessage("Do you really want to delete driver '" + selectedDriver.getName() + "'?");
        messageBox.setText("Delete driver");
        int response = messageBox.open();
        if (response == SWT.YES) {
            selectedDriver.getProviderDescriptor().removeDriver(selectedDriver);
            selectedDriver.getProviderDescriptor().getRegistry().saveDrivers();
            treeControl.refresh();
        }
    }
}