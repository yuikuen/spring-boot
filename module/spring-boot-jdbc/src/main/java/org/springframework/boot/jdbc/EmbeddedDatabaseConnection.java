/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingFunction;

/**
 * Connection details for {@link EmbeddedDatabaseType embedded databases}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Nidhi Desai
 * @author Moritz Halbritter
 * @since 1.0.0
 * @see #get(ClassLoader)
 */
public enum EmbeddedDatabaseConnection {

	/**
	 * No Connection.
	 */
	NONE(null),

	/**
	 * H2 Database Connection.
	 */
	H2("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"),

	/**
	 * Derby Database Connection.
	 */
	DERBY("jdbc:derby:memory:%s;create=true"),

	/**
	 * HSQL Database Connection.
	 * @since 2.4.0
	 */
	HSQLDB("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:%s");

	private final @Nullable String alternativeDriverClass;

	private final @Nullable String url;

	EmbeddedDatabaseConnection(@Nullable String url) {
		this(null, url);
	}

	EmbeddedDatabaseConnection(@Nullable String fallbackDriverClass, @Nullable String url) {
		this.alternativeDriverClass = fallbackDriverClass;
		this.url = url;
	}

	/**
	 * Returns the driver class name.
	 * @return the driver class name
	 */
	public @Nullable String getDriverClassName() {
		// See https://github.com/spring-projects/spring-boot/issues/32865
		return switch (this) {
			case NONE -> null;
			case H2 -> DatabaseDriver.H2.getDriverClassName();
			case DERBY -> DatabaseDriver.DERBY.getDriverClassName();
			case HSQLDB -> DatabaseDriver.HSQLDB.getDriverClassName();
		};
	}

	/**
	 * Returns the {@link EmbeddedDatabaseType} for the connection.
	 * @return the database type
	 */
	public @Nullable EmbeddedDatabaseType getType() {
		// See https://github.com/spring-projects/spring-boot/issues/32865
		return switch (this) {
			case NONE -> null;
			case H2 -> EmbeddedDatabaseType.H2;
			case DERBY -> EmbeddedDatabaseType.DERBY;
			case HSQLDB -> EmbeddedDatabaseType.HSQL;
		};
	}

	/**
	 * Returns the URL for the connection using the specified {@code databaseName}.
	 * @param databaseName the name of the database
	 * @return the connection URL
	 */
	public @Nullable String getUrl(String databaseName) {
		Assert.hasText(databaseName, "'databaseName' must not be empty");
		return (this.url != null) ? String.format(this.url, databaseName) : null;
	}

	boolean isEmbeddedUrl(String url) {
		// See https://github.com/spring-projects/spring-boot/issues/32865
		return switch (this) {
			case NONE -> false;
			case H2 -> url.contains(":h2:mem");
			case DERBY -> true;
			case HSQLDB -> url.contains(":hsqldb:mem:");
		};
	}

	boolean isDriverCompatible(@Nullable String driverClass) {
		return (driverClass != null
				&& (driverClass.equals(getDriverClassName()) || driverClass.equals(this.alternativeDriverClass)));
	}

	/**
	 * Convenience method to determine if a given driver class name and url represent an
	 * embedded database type.
	 * @param driverClass the driver class
	 * @param url the jdbc url (can be {@code null})
	 * @return true if the driver class and url refer to an embedded database
	 * @since 2.4.0
	 */
	public static boolean isEmbedded(@Nullable String driverClass, @Nullable String url) {
		if (driverClass == null) {
			return false;
		}
		EmbeddedDatabaseConnection connection = getEmbeddedDatabaseConnection(driverClass);
		if (connection == NONE) {
			return false;
		}
		return (url == null || connection.isEmbeddedUrl(url));
	}

	private static EmbeddedDatabaseConnection getEmbeddedDatabaseConnection(String driverClass) {
		return Stream.of(H2, HSQLDB, DERBY)
			.filter((connection) -> connection.isDriverCompatible(driverClass))
			.findFirst()
			.orElse(NONE);
	}

	/**
	 * Convenience method to determine if a given data source represents an embedded
	 * database type.
	 * @param dataSource the data source to interrogate
	 * @return true if the data source is one of the embedded types
	 */
	public static boolean isEmbedded(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			return new IsEmbedded().apply(connection);
		}
		catch (SQLException ex) {
			// Could not connect, which means it's not embedded
			return false;
		}
	}

	/**
	 * Returns the most suitable {@link EmbeddedDatabaseConnection} for the given class
	 * loader.
	 * @param classLoader the class loader used to check for classes
	 * @return an {@link EmbeddedDatabaseConnection} or {@link #NONE}.
	 */
	public static EmbeddedDatabaseConnection get(@Nullable ClassLoader classLoader) {
		for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
			if (candidate == NONE) {
				continue;
			}
			String driverClassName = candidate.getDriverClassName();
			Assert.state(driverClassName != null, "'driverClassName' must not be null");
			if (ClassUtils.isPresent(driverClassName, classLoader)) {
				return candidate;
			}
		}
		return NONE;
	}

	/**
	 * Determine if a {@link Connection} is embedded.
	 */
	private static final class IsEmbedded implements ThrowingFunction<Connection, Boolean> {

		@Override
		public Boolean applyWithException(Connection connection) throws SQLException, DataAccessException {
			DatabaseMetaData metaData = connection.getMetaData();
			String productName = metaData.getDatabaseProductName();
			if (productName == null) {
				return false;
			}
			productName = productName.toUpperCase(Locale.ENGLISH);
			EmbeddedDatabaseConnection[] candidates = EmbeddedDatabaseConnection.values();
			for (EmbeddedDatabaseConnection candidate : candidates) {
				if (candidate == NONE) {
					continue;
				}
				EmbeddedDatabaseType type = candidate.getType();
				Assert.state(type != null, "'type' must not be null");
				if (productName.contains(type.name())) {
					String url = metaData.getURL();
					return (url == null || candidate.isEmbeddedUrl(url));
				}
			}
			return false;
		}

	}

}
