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

package org.springframework.boot.sql.autoconfigure.init;

import java.nio.charset.Charset;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * {@link ConfigurationProperties Configuration properties} for initializing an SQL
 * database.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.sql.init")
public class SqlInitializationProperties {

	/**
	 * Locations of the schema (DDL) scripts to apply to the database.
	 */
	private @Nullable List<String> schemaLocations;

	/**
	 * Locations of the data (DML) scripts to apply to the database.
	 */
	private @Nullable List<String> dataLocations;

	/**
	 * Platform to use in the default schema or data script locations,
	 * schema-${platform}.sql and data-${platform}.sql.
	 */
	private String platform = "all";

	/**
	 * Username of the database to use when applying initialization scripts (if
	 * different).
	 */
	private @Nullable String username;

	/**
	 * Password of the database to use when applying initialization scripts (if
	 * different).
	 */
	private @Nullable String password;

	/**
	 * Whether initialization should continue when an error occurs.
	 */
	private boolean continueOnError = false;

	/**
	 * Statement separator in the schema and data scripts.
	 */
	private String separator = ";";

	/**
	 * Encoding of the schema and data scripts.
	 */
	private @Nullable Charset encoding;

	/**
	 * Mode to apply when determining whether initialization should be performed.
	 */
	private DatabaseInitializationMode mode = DatabaseInitializationMode.EMBEDDED;

	public @Nullable List<String> getSchemaLocations() {
		return this.schemaLocations;
	}

	public void setSchemaLocations(@Nullable List<String> schemaLocations) {
		this.schemaLocations = schemaLocations;
	}

	public @Nullable List<String> getDataLocations() {
		return this.dataLocations;
	}

	public void setDataLocations(@Nullable List<String> dataLocations) {
		this.dataLocations = dataLocations;
	}

	public String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public @Nullable Charset getEncoding() {
		return this.encoding;
	}

	public void setEncoding(@Nullable Charset encoding) {
		this.encoding = encoding;
	}

	public DatabaseInitializationMode getMode() {
		return this.mode;
	}

	public void setMode(DatabaseInitializationMode mode) {
		this.mode = mode;
	}

}
