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
/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;

import java.util.Collection;

/**
 * Provides a listener for dropping nodes onto the editor drawing
 */
public class NodeDropTargetListener extends AbstractTransferDropTargetListener {

    public NodeDropTargetListener(EditPartViewer viewer)
    {
        super(viewer, DatabaseObjectTransfer.getInstance());
    }

    @Override
    protected void updateTargetRequest()
    {

    }

    @Override
    protected Request createTargetRequest()
    {
        CreateRequest request = new CreateRequest();
        request.setFactory(new CreationFactory() {
            @Override
            public Object getNewObject()
            {
                Collection<DBPNamedObject> objects = DatabaseObjectTransfer.getInstance().getObject();
                if (objects == null) {
                    return null;
                }

                return DiagramObjectCollector.generateEntityList(
                    ((DiagramPart) getViewer().getRootEditPart().getContents()).getDiagram(),
                    objects);
            }

            @Override
            public Object getObjectType()
            {
                return RequestConstants.REQ_CREATE;
            }
        });
        request.setLocation(getDropLocation());
        return request;
    }

}

