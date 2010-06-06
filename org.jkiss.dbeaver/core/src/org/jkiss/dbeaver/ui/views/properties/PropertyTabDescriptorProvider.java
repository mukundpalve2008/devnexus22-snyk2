/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptorProvider;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;
import org.eclipse.ui.views.properties.tabbed.AbstractSectionDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISection;

import java.util.List;
import java.util.ArrayList;

/**
 * PropertyTabDescriptorProvider
 */
public class PropertyTabDescriptorProvider implements ITabDescriptorProvider {

    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection)
    {
        List<ISectionDescriptor> standardSections = new ArrayList<ISectionDescriptor>();
        standardSections.add(new AbstractSectionDescriptor() {
            public String getId()
            {
                return PropertiesContributor.SECTION_STANDARD;
            }

            public ISection getSectionClass()
            {
                return new PropertySectionStandard();
            }

            public String getTargetTab()
            {
                return PropertiesContributor.TAB_STANDARD;
            }

            @Override
            public boolean appliesTo(IWorkbenchPart part, ISelection selection)
            {
                return true;
            }
        });
        return new ITabDescriptor[] {
            new PropertyTabDescriptor(
                PropertiesContributor.CATEGORY_MAIN,
                PropertiesContributor.TAB_STANDARD,
                "Info",
                standardSections)
        };
    }

}
