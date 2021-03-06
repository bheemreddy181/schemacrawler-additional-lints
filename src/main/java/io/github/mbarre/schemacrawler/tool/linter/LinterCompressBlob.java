package io.github.mbarre.schemacrawler.tool.linter;

/*-
 * #%L
 * Additional SchemaCrawler Lints
 * %%
 * Copyright (C) 2015 - 2017 github
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import schemacrawler.schema.Column;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.Config;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.tools.lint.BaseLinter;
import schemacrawler.tools.lint.LintSeverity;

/**
 * Created by barmi83 on 3/24/17.
 */
public class LinterCompressBlob extends BaseLinter {
    private static final Logger LOGGER = Logger.getLogger(LinterCompressBlob.class.getName());
    public static final int MIN_COMPRESSION_PERCENT = 20;
    public static final String COMPRESSION_PERCENT_CONFIG_PARAM = "minCompressionPercent";

    private int minCompressionPercent;

    /**
     * The lint that
     *
     */
    public LinterCompressBlob() {
        super();
        setSeverity(LintSeverity.high);
        minCompressionPercent = MIN_COMPRESSION_PERCENT;
    }

    @Override
    public void configure(Config config) {
        minCompressionPercent = config.getIntegerValue(COMPRESSION_PERCENT_CONFIG_PARAM, MIN_COMPRESSION_PERCENT);
    }

    /**
     * Get lint descrption
     * @return lint description
     */
    @Override
    public String getDescription() {
        return getSummary();
    }

    /**
     * Get lint Summary
     * @return lint Summary
     */
    @Override
    public String getSummary() {
        return "BLOB should be archived";
    }

    /**
     * The lint that does the job
     * @param table table
     * @param connection connection
     */
    @Override
    protected void lint(final Table table, final Connection connection) throws SchemaCrawlerException{

        try (Statement stmt = connection.createStatement()){

            List<Column> columns = getColumns(table);
            int columnDataType;
            int count;
            int distinctCount;

            for (Column column : columns) {
				column.getColumnDataType().getJavaSqlType();
                columnDataType = column.getColumnDataType().getJavaSqlType().getVendorTypeNumber();
                if(columnDataType == Types.BINARY || columnDataType == Types.BLOB) {
                    inspectBlobData(stmt, table, column);
                }
            }
        }catch (SQLException ex) {
            LOGGER.severe(ex.getMessage());
            throw new SchemaCrawlerException(ex.getMessage(), ex);
        }

    }


    private void inspectBlobData(Statement stmt, Table table, Column column) throws SchemaCrawlerException{
        String tableName = table.getName().replaceAll("\"", "");
        String columnName = column.getName().replaceAll("\"", "");

        String sql = "select " + columnName + " from " + tableName ;
        LOGGER.log(Level.CONFIG, "SQL : {0}", sql);

        InputStream byte_stream;
        try(ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next()){
                File file = new File("target/file");

                try(FileOutputStream fos = new FileOutputStream(file)){
                    byte[] buffer = new byte[1];
                    byte_stream = rs.getBinaryStream(columnName);
                    int nbBytes = 0;
                    while (byte_stream.read(buffer) > 0 && nbBytes < 512000) {
                        fos.write(buffer);
                        nbBytes++;
                    }
                    byte_stream.close();
                    int extract_size = nbBytes-1;

                    /* Create Output Stream that will have final zip files */
                    OutputStream zip_output = new FileOutputStream(new File("target/zip_output.zip"));
                    /* Create Archive Output Stream that attaches File Output Stream / and specifies type of compression */
                    ArchiveOutputStream logical_zip = null;
                    logical_zip = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, zip_output);
                    /* Create Archieve entry - write header information*/
                    logical_zip.putArchiveEntry(new ZipArchiveEntry("target/file"));
                    /* Copy input file */
                    IOUtils.copy(new FileInputStream(new File("target/file")), logical_zip);
                    /* Close Archieve entry, write trailer information */
                    logical_zip.closeArchiveEntry();
                    /* Finish addition of entries to the file */
                    logical_zip.finish();
                    /* Close output stream, our files are zipped */
                    zip_output.close();

                    File zip_file = new File("target/zip_output.zip");
                    if(zip_file.length() < extract_size) {
                        double rate = (zip_file.length() - extract_size)*100/extract_size;
                        if (Math.abs(rate) >= minCompressionPercent) {
                            LOGGER.info("Extract size :" + extract_size + "ko, zip file size :" + zip_file.length() + " rate :" +rate+"%");
                            addLint(table, "Blob should be compressed", column.getFullName());
                        }
                    }

                } catch (IOException | ArchiveException e){
                    LOGGER.severe(e.getMessage());
                    throw new SchemaCrawlerException(e.getMessage(), e);
                }
            }
        }catch (SQLException ex) {
            LOGGER.severe(ex.getMessage());
            throw new SchemaCrawlerException(ex.getMessage(), ex);
        }
    }

    }
