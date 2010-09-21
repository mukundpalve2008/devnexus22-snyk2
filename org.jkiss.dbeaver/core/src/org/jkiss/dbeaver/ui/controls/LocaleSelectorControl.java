/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocaleSelectorControl
 */
public class LocaleSelectorControl extends Composite
{
    static final Log log = LogFactory.getLog(LocaleSelectorControl.class);
    private Combo languageCombo;
    private Combo countryCombo;
    private Combo variantCombo;
    private Text localeText;
    private Locale currentLocale;

    public LocaleSelectorControl(
        Composite parent,
        Locale defaultLocale)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        this.setLayout(gl);

        Group group = new Group(this, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        gl = new GridLayout(2, false);
        group.setLayout(gl);
        group.setText("Locale");

        UIUtils.createControlLabel(group, "Language");
        languageCombo = new Combo(group, SWT.DROP_DOWN);
        languageCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        languageCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                onLanguageChange(null);
                onCountryChange(null);
            }
        });
        languageCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                onLanguageChange(null);
                onCountryChange(null);
            }
        });

        UIUtils.createControlLabel(group, "Country");
        countryCombo = new Combo(group, SWT.DROP_DOWN);
        countryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        countryCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                onCountryChange(null);
            }
        });
        countryCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                onCountryChange(null);
            }
        });

        UIUtils.createControlLabel(group, "Variant");
        variantCombo = new Combo(group, SWT.DROP_DOWN);
        variantCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        variantCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                calculateLocale();
            }
        });

        UIUtils.createControlLabel(group, "Locale");
        localeText = new Text(group, SWT.BORDER | SWT.READ_ONLY);
        localeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Locale[] locales = Locale.getAvailableLocales();

        Set<String> languages = new TreeSet<String>();
        for (Locale locale : locales) {
            languages.add(locale.getLanguage() + " - " + locale.getDisplayLanguage());
        }

        currentLocale = defaultLocale;
        if (currentLocale == null) {
            currentLocale = Locale.getDefault();
        }
        for (String language : languages) {
            languageCombo.add(language);
            if (getIsoCode(language).equals(currentLocale.getLanguage())) {
                languageCombo.select(languageCombo.getItemCount() - 1);
            }
        }

        onLanguageChange(currentLocale.getCountry());
        onCountryChange(currentLocale.getVariant());
    }

    private static String getIsoCode(String value)
    {
        int divPos = value.indexOf('-');
        return divPos == -1 ? value.trim() : value.substring(0, divPos).trim();
    }

    private void onLocaleChange()
    {
        languageCombo.setText(currentLocale.getLanguage());
        onLanguageChange(currentLocale.getCountry());
        countryCombo.setText(currentLocale.getCountry());
        onCountryChange(currentLocale.getVariant());
    }

    private void onLanguageChange(String defCountry)
    {
        String language = getIsoCode(languageCombo.getText());
        Locale[] locales = Locale.getAvailableLocales();
        countryCombo.removeAll();
        Set<String> countries = new TreeSet<String>();
        for (Locale locale : locales) {
            if (language.equals(locale.getLanguage()) && !CommonUtils.isEmpty(locale.getCountry())) {
                countries.add(locale.getCountry() + " - " + locale.getDisplayCountry());
            }
        }
        for (String country : countries) {
            countryCombo.add(country);
            if (defCountry != null && defCountry.equals(getIsoCode(country))) {
                countryCombo.select(countryCombo.getItemCount() - 1);
            }
        }
        if (defCountry == null && countryCombo.getItemCount() > 0) {
            countryCombo.select(0);
        }
        calculateLocale();
    }

    private void onCountryChange(String defVariant)
    {
        String language = getIsoCode(languageCombo.getText());
        String country = getIsoCode(countryCombo.getText());
        Locale[] locales = Locale.getAvailableLocales();
        variantCombo.removeAll();
        Set<String> variants = new TreeSet<String>();
        for (Locale locale : locales) {
            if (language.equals(locale.getLanguage()) && country.equals(locale.getCountry())) {
                if (!CommonUtils.isEmpty(locale.getVariant())) {
                    if (locale.getVariant().equals(locale.getDisplayVariant())) {
                        variants.add(locale.getVariant());
                    } else {
                        variants.add(locale.getVariant() + " - " + locale.getDisplayVariant());
                    }
                }
            }
        }
        for (String variant : variants) {
            variantCombo.add(variant);
            if (defVariant != null && defVariant.equals(getIsoCode(variant))) {
                variantCombo.select(variantCombo.getItemCount() - 1);
            }
        }
        if (defVariant == null && variantCombo.getItemCount() > 0) {
            variantCombo.select(0);
        }
        calculateLocale();
    }

    private void calculateLocale()
    {
        String language = getIsoCode(languageCombo.getText());
        String country = getIsoCode(countryCombo.getText());
        String variant = getIsoCode(variantCombo.getText());
        currentLocale = new Locale(language, country, variant);
        localeText.setText(currentLocale.toString());
    }
    
    public void setLocale(Locale locale)
    {
        currentLocale = locale;
        onLocaleChange();
    }

    public Locale getSelectedLocale()
    {
        return currentLocale;
    }

}