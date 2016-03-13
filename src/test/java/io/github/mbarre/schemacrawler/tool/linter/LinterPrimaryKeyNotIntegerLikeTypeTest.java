package io.github.mbarre.schemacrawler.tool.linter;

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

import io.github.mbarre.schemacrawler.test.utils.LintWrapper;
import io.github.mbarre.schemacrawler.test.utils.PostgreSqlDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.tools.lint.LinterRegistry;

/**
 *
 * @author barmi83
 */
public class LinterPrimaryKeyNotIntegerLikeTypeTest extends BaseLintTest {
    
    private static final String CHANGE_LOG_PKTYPE_CHECK = "src/test/db/liquibase/primaryKeyTypeCheck/db.changelog.xml";
    private static PostgreSqlDatabase database;
    
    
    @BeforeClass
    public static void  init(){
        database = new PostgreSqlDatabase();
        database.setUp(CHANGE_LOG_PKTYPE_CHECK);
    }
    
    @Test
    public void testLint() throws Exception {
        final LinterRegistry registry = new LinterRegistry();
        Assert.assertTrue(registry.hasLinter(LinterPrimaryKeyNotIntegerLikeType.class.getName()));
        
        final SchemaCrawlerOptions options = new SchemaCrawlerOptions();
        // Set what details are required in the schema - this affects the
        // time taken to crawl the schema
        options.setSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
        //options.setTableNamePattern("\"TEST_CASE\"");
        
        Connection connection = DriverManager.getConnection(PostgreSqlDatabase.CONNECTION_STRING,
                PostgreSqlDatabase.USER_NAME, database.getPostgresPassword());
        
        List<LintWrapper> lints = executeToJsonAndConvertToLintList(options, connection);
        
        boolean lint1Detected = false;
        boolean lint2Detected = false;
        
        for (LintWrapper lint : lints) {
            if(LinterPrimaryKeyNotIntegerLikeType.class.getName().equals(lint.getId())){
                if("public.test_pk_char.id".equals(lint.getValue())){
                    Assert.assertEquals("Should be Integer like type or eventually char(1).", lint.getDescription());
                    Assert.assertEquals("high", lint.getSeverity());
                    lint1Detected = true;
                }
                else if("public.test_pk_timestamp.id".equals(lint.getValue())){
                    Assert.assertEquals("Should be Integer like type or eventually char(1).", lint.getDescription());
                    Assert.assertEquals("high", lint.getSeverity());
                    lint2Detected = true;
                }
                else{
                    Assert.fail("Not expected error detected :"+lint.getValue());
                }
            }
        }
        
        Assert.assertTrue("Some expected errors have not been detected.", lint1Detected && lint2Detected);
    }
    
}
