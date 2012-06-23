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

import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBCStatement;

/**
* Query transformer for RS limit
*/
class QueryTransformerLimit implements DBCQueryTransformer {

    private Object offset;
    private Object length;
    private boolean limitSet;

    @Override
    public void setParameters(Object... parameters) {
        this.offset = parameters[0];
        this.length = parameters[1];
    }

    @Override
    public String transformQueryString(String query) throws DBCException {
        String testQuery = query.toUpperCase().trim();
        if (!testQuery.startsWith("SELECT") || testQuery.indexOf("LIMIT") != -1) {
            limitSet = false;
        } else {
            query = query + SQLUtils.TOKEN_TRANSFORM_START + " LIMIT " + offset + ", " + length + SQLUtils.TOKEN_TRANSFORM_END;
            limitSet = true;
        }
        return query;
    }

    @Override
    public void transformStatement(DBCStatement statement, int parameterIndex) throws DBCException {
        if (!limitSet) {
            statement.setLimit(((Number)offset).longValue(), ((Number)length).longValue());
        }
    }
}
