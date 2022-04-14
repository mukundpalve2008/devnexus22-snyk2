/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class PostgreDatabaseRestoreSettings extends PostgreBackupRestoreSettings {

    private static final Log log = Log.getLog(PostgreDatabaseRestoreSettings.class);

    private String inputFile;
    private boolean cleanFirst;
    private boolean noOwner;
    private boolean createDatabase;

    private PostgreDatabaseRestoreInfo restoreInfo;
    private File restoreFolder;

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public boolean isCleanFirst() {
        return cleanFirst;
    }

    public void setCleanFirst(boolean cleanFirst) {
        this.cleanFirst = cleanFirst;
    }

    public boolean isNoOwner() {
        return noOwner;
    }

    public void setNoOwner(boolean noOwner) {
        this.noOwner = noOwner;
    }

    public boolean isCreateDatabase() {
        return createDatabase;
    }

    public void setCreateDatabase(boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public PostgreDatabaseRestoreInfo getRestoreInfo() {
        return restoreInfo;
    }

    public void setRestoreInfo(PostgreDatabaseRestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);

        if (restoreFolder == null|| !restoreFolder.exists()) {
            log.warn("Output directory does not exists, using user home directory instead");
            this.restoreFolder = null;
        }
        if (this.restoreFolder == null) {
            this.restoreFolder = RuntimeUtils.getUserHomeDir();
        }
        inputFile = store.getString("pg.restore.inputFile");
        cleanFirst = store.getBoolean("pg.restore.cleanFirst");
        noOwner = store.getBoolean("pg.restore.noOwner");
        createDatabase = store.getBoolean("pg.restore.createDatabase");

        if (store instanceof DBPPreferenceMap) {
            String catalogId = store.getString("pg.restore.database");

            if (!CommonUtils.isEmpty(catalogId)) {
                try {
                    runnableContext.run(true, true, monitor -> {
                        try {
                            PostgreDatabase database = (PostgreDatabase) DBUtils.findObjectById(monitor, getProject(), catalogId);
                            if (database == null) {
                                throw new DBException("Database " + catalogId + " not found");
                            }
                            restoreInfo = new PostgreDatabaseRestoreInfo(database);
                        } catch (Throwable e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error("Error loading objects configuration", e);
                } catch (InterruptedException e) {
                    // Ignore
                }
            } else {
                for (DBSObject object : getDatabaseObjects()) {
                    if (object instanceof PostgreDatabase) {
                        restoreInfo = new PostgreDatabaseRestoreInfo((PostgreDatabase) object);
                        break;
                    }
                }
            }
        }

        if (restoreInfo == null) {
            throw new DBException("Cannot find database for restoring");
        }
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);

        store.setValue("pg.restore.inputFile", inputFile);
        store.setValue("pg.restore.cleanFirst", cleanFirst);
        store.setValue("pg.restore.noOwner", noOwner);
        store.setValue("pg.restore.createDatabase", createDatabase);
        store.setValue("pg.restore.database", DBUtils.getObjectFullId(restoreInfo.getDatabase()));
    }

    public File getRestoreFolder(@NotNull PostgreDatabaseRestoreInfo info) {
        if (restoreFolder == null) {
            restoreFolder = new File(replaceVars(info, getOutputFolderPattern()));
        }
        return restoreFolder;
    }

    private String replaceVars(@NotNull PostgreDatabaseRestoreInfo info, String pattern) {
        return GeneralUtils.replaceVariables(pattern, name -> {
            switch (name) {
                case NativeToolUtils.VARIABLE_DATABASE:
                    return info.getDatabase().getName();
                case NativeToolUtils.VARIABLE_HOST:
                    return info.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case NativeToolUtils.VARIABLE_CONN_TYPE:
                    return info.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getConnectionType().getId();
                case NativeToolUtils.VARIABLE_TIMESTAMP:
                    return RuntimeUtils.getCurrentTimeStamp();
                case NativeToolUtils.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    return NativeToolUtils.replaceVariables(name);
            }
        });
    }

}
