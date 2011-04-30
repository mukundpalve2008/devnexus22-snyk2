/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.ISearchExecutor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.properties.ObjectPropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private Searcher searcher;
    private SearchColorProvider searchColorProvider;
    private Color searchHighlightColor;
    private Color newObjectColor;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchPart, node, metaNode);
        searcher = new Searcher();
        searchColorProvider = new SearchColorProvider();
        searchHighlightColor = new Color(parent.getDisplay(), 170, 255, 170);
        newObjectColor = new Color(parent.getDisplay(), 0xFF, 0xB6, 0xC1);
    }

    @Override
    public void dispose()
    {
//        if (objectEditorHandler != null) {
//            objectEditorHandler.dispose();
//            objectEditorHandler = null;
//        }
        UIUtils.dispose(searchHighlightColor);
        UIUtils.dispose(newObjectColor);
        super.dispose();
    }

    @Override
    protected ISearchExecutor getSearchRunner()
    {
        return searcher;
    }

    @Override
    protected LoadingJob<Collection<DBNNode>> createLoadService()
    {
        return LoadingUtils.createService(
            new ItemLoadService(getNodeMeta()),
            new ObjectsLoadVisualizer());
    }

    @Override
    protected EditingSupport makeEditingSupport(ViewerColumn viewerColumn, int columnIndex)
    {
        return new CellEditingSupport(columnIndex);
    }

    @Override
    public IColorProvider getObjectColorProvider()
    {
        return searchColorProvider;
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", getRootNode() instanceof DBSWrapper ? (DBSWrapper)getRootNode() : null);
            this.metaNode = metaNode;
        }

        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<DBNNode>();
                List<? extends DBNNode> children = getRootNode().getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
/*
                    if (item instanceof DBNDatabaseFolder) {
                        continue;
                    }
*/
                    if (metaNode != null) {
                        if (!(item instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        if (((DBNDatabaseNode)item).getMeta() != metaNode) {
                            continue;
                        }
                    }
                    items.add(item);
                }
                return items;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    private class CellEditingSupport extends EditingSupport {

        private int columnIndex;

        public CellEditingSupport(int columnIndex)
        {
            super(getItemsViewer());
            this.columnIndex = columnIndex;
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null && property.isEditable(getObjectValue(object))) {
                return property.createPropertyEditor(getControl());
            }
            return null;
        }

        @Override
        protected boolean canEdit(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            return property != null && property.isEditable(getObjectValue(object));
        }

        protected Object getValue(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                return getListPropertySource().getPropertyValue(getObjectValue(object), property.getId());
            }
            return null;
        }

        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                getListPropertySource().setPropertyValue(getObjectValue(object), property.getId(), value);
            }
        }

    }

    private class Searcher extends ObjectSearcher<DBNNode> {
        @Override
        protected void setInfo(String message)
        {
            ItemListControl.this.setInfo(message);
        }

        @Override
        protected Collection<DBNNode> getContent()
        {
            return (Collection<DBNNode>) getItemsViewer().getInput();
        }

        @Override
        protected void selectObject(DBNNode object)
        {
            getItemsViewer().setSelection(object == null ? new StructuredSelection() : new StructuredSelection(object));
        }

        @Override
        protected void updateObject(DBNNode object)
        {
            getItemsViewer().update(object, null);
        }

        @Override
        protected void revealObject(DBNNode object)
        {
            getItemsViewer().reveal(object);
        }

    }

    private class SearchColorProvider implements IColorProvider {

        public Color getForeground(Object element)
        {
            return null;
        }

        public Color getBackground(Object element)
        {
            if (searcher != null && searcher.hasObject((DBNNode) element)) {
                return searchHighlightColor;
            }
            if (element instanceof DBNDatabaseNode && !((DBNDatabaseNode) element).getObject().isPersisted()) {
                return newObjectColor;
            }
            return null;
        }
    }

}
