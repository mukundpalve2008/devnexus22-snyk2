/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PropertyCollector
 */
public class PropertySourceAbstract implements IPropertySource
{
    private Object object;
    private boolean loadLazyObjects;
    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private Map<Object, Object> propValues = new HashMap<Object, Object>();

    public PropertySourceAbstract(Object object)
    {
        this(object, true);
    }

    public PropertySourceAbstract(Object object, boolean loadLazyObjects)
    {
        this.object = object;
        this.loadLazyObjects = loadLazyObjects;
    }

    public PropertySourceAbstract addProperty(IPropertyDescriptor prop)
    {
        props.add(prop);
        propValues.put(prop.getId(), prop);
        return this;
    }

    public PropertySourceAbstract addProperty(Object id, String name, Object value)
    {
        if (value instanceof Collection) {
            props.add(new PropertyDescriptor(id, name));
            propValues.put(id, new PropertySourceCollection(id, (Collection) value));
        } else {
            props.add(new PropertyDescriptor(id, name));
            propValues.put(id, value);
        }
        return this;
    }

    public boolean isEmpty()
    {
        return props.isEmpty();
    }

    public Object getEditableValue()
    {
        return object;
    }

    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    public Object getPropertyValue(final Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof PropertyAnnoDescriptor) {
            try {
                value = ((PropertyAnnoDescriptor)value).readValue(object);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (value instanceof ILoadService) {
            if (loadLazyObjects) {
                final ILoadService loadService = (ILoadService) value;
                String loadText = loadService.getServiceName();
                // We assume that it can be called ONLY by properties viewer
                // So, start lazy loading job to update it after value will be loaded
                LoadingUtils.executeService(
                    loadService,
                    new PropertySheetLoadVisualizer(id, loadText));
                // Return dummy string for now
                return loadText;
            } else {
                return null;
            }
        }
        if (value instanceof Collection) {
            // Make descriptor of collection
            // Each element as separate property
            Collection collection = (Collection)value;
            collection.size();
        }
        return UIUtils.makeStringForUI(value);
    }

    public boolean isPropertySet(Object id)
    {
        return false;
    }

    public void resetPropertyValue(Object id)
    {
    }

    public void setPropertyValue(Object id, Object value)
    {
    }

    private class PropertySheetLoadVisualizer implements ILoadVisualizer {
        private Object propertyId;
        private String loadText;
        private int callCount = 0;
        private boolean completed = false;

        private PropertySheetLoadVisualizer(Object propertyId, String loadText)
        {
            this.propertyId = propertyId;
            this.loadText = loadText;
        }

        public boolean isCompleted()
        {
            return completed;
        }
        public void visualizeLoading()
        {
            String dots;
            switch (callCount++ % 4) {
            case 0: dots = ""; break;
            case 1: dots = "."; break;
            case 2: dots = ".."; break;
            case 3: default: dots = "..."; break;
            }
            propValues.put(propertyId, loadText + dots);
            refreshProperties();
        }
        public void completeLoading(Object result)
        {
            completed = true;
            propValues.put(propertyId, result);
            refreshProperties();
        }
        private void refreshProperties()
        {
            // Here is some kind of dirty hack - use direct casts to propertyshet implementation
            PropertiesPage page = PropertiesPage.getPageByObject(object);
            if (page == null) {
                PropertiesView view = ViewUtils.findView(
                    DBeaverCore.getActiveWorkbenchWindow(),
                    PropertiesView.class);
                if (view != null) {
                    page = view.getCurrentPage();
                }
            }
            if (page != null) {
                Object curObject = page.getCurrentObject();
                // Refresh only if current property sheet object is the same as for collector
                if (curObject == object) {
                    DBeaverCore.getInstance().getPropertiesAdapter().addToCache(object, PropertySourceAbstract.this);
                    try {
                        page.refresh();
                    }
                    finally {
                        DBeaverCore.getInstance().getPropertiesAdapter().removeFromCache(object);
                    }
                }
            }
        }

    }
}