package io.github.mbarre.schemacrawler.tool.linter;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.mbarre.schemacrawler.utils.LintUtils;

/*
 * #%L
 * Additional SchemaCrawler Lints
 * %%
 * Copyright (C) 2015 - 2016 github
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import schemacrawler.schema.Column;
import schemacrawler.schema.JavaSqlType;
import schemacrawler.schema.Table;
import schemacrawler.tools.lint.BaseLinter;
import schemacrawler.tools.lint.LintSeverity;

/**
 * Linter to check if JSON type is used instead of JSONB - PostgreSQL reserved
 * lint
 * 
 * @author mbarre
 * @since 1.0.1
 */
public class LinterTimeStampWithOutTimeZoneColumn extends BaseLinter {
	private static final Logger LOGGER = Logger.getLogger(LinterTimeStampWithOutTimeZoneColumn.class.getName());

	/**
	 * The lint that test if the proper jsonb type has been used.
	 *
	 */
	public LinterTimeStampWithOutTimeZoneColumn() {
		setSeverity(LintSeverity.high);
	}

	/**
	 * Get the description
	 * 
	 * @return the description
	 */
	
	public String getDescription() {
		return "Timestamp without time zone (timestamp) is not a permitted data type. Use timestamp with time zone (timestamptz)";
	}

	/**
	 * Get the summaru
	 * 
	 * @return the summaru
	 */
	@Override
	public String getSummary() {
		return "Use timeStamptz instead of just TimeStamp";
	}

	/**
	 * The lint that does the job
	 * 
	 * @param table
	 *            table
	 * @param connection
	 *            connection
	 */
	@Override
	protected void lint(final Table table, final Connection connection) {
		List<Column> columns = getColumns(table);
		for (Column column : columns) {
			LOGGER.log(Level.INFO, "Checking {0}...", column.getFullName());
			if (LintUtils.isSqlTypeTimeStampBased(column.getColumnDataType().getJavaSqlType().getVendorTypeNumber())) {
				addLint(table, getDescription(), column.getFullName());
			}
		}
	}

}
