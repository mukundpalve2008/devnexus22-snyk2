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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;

/**
 * SQLDocumentProvider
 */
class SQLDocumentProvider extends FileRefDocumentProvider {

    @Override
    protected IEditorInput createNewEditorInput(IFile newFile)
    {
        return new SQLEditorInput(newFile);
    }

    @Override
    protected Document createDocument(Object element) throws CoreException
    {
        Document document = super.createDocument(element);


        return document;
    }
}
