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
package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * MySQLUser
 */
public class MySQLUser implements DBAUser, DBPSaveableObject
{
    static final Log log = LogFactory.getLog(MySQLUser.class);

    private MySQLDataSource dataSource;
    private String userName;
    private String host;
    private String passwordHash;

    private String sslType;
    private byte[] sslCipher;
    private byte[] x509Issuer;
    private byte[] x509Subject;

    private int maxQuestions;
    private int maxUpdates;
    private int maxConnections;
    private int maxUserConnections;

    private List<MySQLGrant> grants;
    private boolean persisted;

    public MySQLUser(MySQLDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        if (resultSet != null) {
            this.persisted = true;
            this.userName = JDBCUtils.safeGetString(resultSet, "user");
            this.host = JDBCUtils.safeGetString(resultSet, "host");
            this.passwordHash = JDBCUtils.safeGetString(resultSet, "password");

            this.sslType = JDBCUtils.safeGetString(resultSet, "ssl_type");
            this.sslCipher = JDBCUtils.safeGetBytes(resultSet, "ssl_cipher");
            this.x509Issuer = JDBCUtils.safeGetBytes(resultSet, "x509_issuer");
            this.x509Subject = JDBCUtils.safeGetBytes(resultSet, "x509_subject");

            this.maxQuestions = JDBCUtils.safeGetInt(resultSet, "max_questions");
            this.maxUpdates = JDBCUtils.safeGetInt(resultSet, "max_updates");
            this.maxConnections = JDBCUtils.safeGetInt(resultSet, "max_connections");
            this.maxUserConnections = JDBCUtils.safeGetInt(resultSet, "max_user_connections");
        } else {
            this.persisted = false;
            this.userName = "user";
            this.host = "%";
        }
    }

    @Override
    @Property(name = "User name", viewable = true, order = 1)
    public String getName() {
        return userName + "@" + host;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getFullName() {
        return "'" + userName + "'@'" + host + "'";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @Override
    public MySQLDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
        DBUtils.fireObjectUpdate(this);
    }

    @Property(name = "Host mask", viewable = true, order = 2)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void clearGrantsCache()
    {
        this.grants = null;
    }

    public List<MySQLGrant> getGrants(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.grants != null) {
            return this.grants;
        }
        if (!isPersisted()) {
            this.grants = new ArrayList<MySQLGrant>();
            return this.grants;
        }

        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Read catalog privileges");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("SHOW GRANTS FOR " + getFullName());
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MySQLGrant> grants = new ArrayList<MySQLGrant>();
                    while (dbResult.next()) {
                        List<MySQLPrivilege> privileges = new ArrayList<MySQLPrivilege>();
                        boolean allPrivilegesFlag = false;
                        boolean grantOption = false;
                        String catalog = null;
                        String table = null;

                        String grantString = JDBCUtils.safeGetString(dbResult, 1).trim().toUpperCase();
                        if (grantString.endsWith(" WITH GRANT OPTION")) {
                            grantOption = true;//privileges.add(getDataSource().getPrivilege(monitor, MySQLPrivilege.GRANT_PRIVILEGE));
                        }
                        Matcher matcher = MySQLGrant.GRANT_PATTERN.matcher(grantString);
                        if (matcher.find()) {
                            StringTokenizer st = new StringTokenizer(matcher.group(1), ",");
                            while (st.hasMoreTokens()) {
                                String privName = st.nextToken().trim();
                                if (privName.equalsIgnoreCase(MySQLPrivilege.ALL_PRIVILEGES)) {
                                    allPrivilegesFlag = true;
                                    continue;
                                }
                                MySQLPrivilege priv = getDataSource().getPrivilege(monitor, privName);
                                if (priv == null) {
                                    log.warn("Could not find privilege '" + privName + "'");
                                } else {
                                    privileges.add(priv);
                                }
                            }
                            catalog = matcher.group(2);
                            table = matcher.group(3);
                        } else {
                            log.warn("Could not parse GRANT string: " + grantString);
                            continue;
                        }
                        grants.add(
                            new MySQLGrant(
                                this,
                                privileges,
                                catalog,
                                table,
                                allPrivilegesFlag,
                                grantOption));
                    }
                    this.grants = grants;
                    return this.grants;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
    }

    public String getSslType() {
        return sslType;
    }

    void setSslType(String sslType) {
        this.sslType = sslType;
    }

    public byte[] getSslCipher() {
        return sslCipher;
    }

    void setSslCipher(byte[] sslCipher) {
        this.sslCipher = sslCipher;
    }

    public byte[] getX509Issuer() {
        return x509Issuer;
    }

    void setX509Issuer(byte[] x509Issuer) {
        this.x509Issuer = x509Issuer;
    }

    public byte[] getX509Subject() {
        return x509Subject;
    }

    void setX509Subject(byte[] x509Subject) {
        this.x509Subject = x509Subject;
    }

    public int getMaxQuestions() {
        return maxQuestions;
    }

    public void setMaxQuestions(int maxQuestions) {
        this.maxQuestions = maxQuestions;
    }

    public int getMaxUpdates() {
        return maxUpdates;
    }

    public void setMaxUpdates(int maxUpdates) {
        this.maxUpdates = maxUpdates;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxUserConnections() {
        return maxUserConnections;
    }

    public void setMaxUserConnections(int maxUserConnections) {
        this.maxUserConnections = maxUserConnections;
    }

}