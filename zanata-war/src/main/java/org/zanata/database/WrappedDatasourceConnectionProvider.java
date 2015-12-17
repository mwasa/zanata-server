/*
 * Copyright 2013, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.zanata.database;

import com.google.common.base.Throwables;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Sean Flanigan <a
 *         href="mailto:sflaniga@redhat.com">sflaniga@redhat.com</a>
 */
public class WrappedDatasourceConnectionProvider implements ConnectionProvider {
    private static final long serialVersionUID = 1L;
    private final WrapperManager wrapperManager = new WrapperManager();

    private static final Class<? extends ConnectionProvider> delegateClass;

    static {
        try {
            // Hibernate 4.2 (for EAP)
            Class<? extends ConnectionProvider> aClass = delegateClass =
                    (Class<? extends ConnectionProvider>) Class.forName(
                            "org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl");
        } catch (ClassNotFoundException e) {
            try {
                // Hibernate 5 (for Wildfly)
                delegateClass =
                        (Class<? extends ConnectionProvider>) Class.forName(
                                "org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl");
            } catch (ClassNotFoundException cnfex) {
                throw new RuntimeException("Hibernate not detected on the classpath!");
            }
        }
    }

    private ConnectionProvider delegate;

    public WrappedDatasourceConnectionProvider() {
        try {
            delegate = delegateClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrapperManager.wrapIfNeeded(delegate.getConnection());
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        delegate.closeConnection(conn);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return delegate.supportsAggressiveRelease();
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return delegate.isUnwrappableAs(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return delegate.unwrap(unwrapType);
    }
}
