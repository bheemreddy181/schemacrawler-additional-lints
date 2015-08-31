/**
 * 
 */
package io.github.mbarre.schemacrawler.test.tool.linter;

import io.github.mbarre.schemacrawler.test.utils.PostgreSqlDatabase;
import io.github.mbarre.schemacrawler.tool.linter.LinterJsonContent;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.io.output.StringBuilderWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.tools.executable.Executable;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.lint.LinterRegistry;
import schemacrawler.tools.options.OutputOptions;
import schemacrawler.tools.options.TextOutputFormat;


/**
 * Check columns with JSON content but not JSON or JSONB type
 * @author mbarre
 * @since 1.0.1
 */
public class LinterJsonContentTest {

	private static final String CHANGE_LOG_JSON_CHECK = "src/test/db/liquibase/jsonContentCheck/db.changelog.xml";
	private Logger LOGGER = LoggerFactory.getLogger(LinterJsonContentTest.class);
	private static PostgreSqlDatabase database;

	@BeforeClass
	public static void  init(){
		database = new PostgreSqlDatabase();
		database.setUp(CHANGE_LOG_JSON_CHECK);
	}

	@Test
	public void testLint() throws Exception{

		final LinterRegistry registry = new LinterRegistry();
		Assert.assertTrue(registry.hasLinter(LinterJsonContent.class.getName()));

		final SchemaCrawlerOptions options = new SchemaCrawlerOptions();
		// Set what details are required in the schema - this affects the
		// time taken to crawl the schema
		options.setSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
		options.setTableNamePattern("test_json");
		
		Connection connection = DriverManager.getConnection(PostgreSqlDatabase.CONNECTION_STRING, 
				PostgreSqlDatabase.USER_NAME, database.getPostgresPassword());
		
		final Executable executable = new SchemaCrawlerExecutable("lint");
		
		try (StringBuilderWriter out = new StringBuilderWriter()) {
			OutputOptions outputOptions = new OutputOptions(TextOutputFormat.json,out);
			executable.setOutputOptions(outputOptions);
			executable.setSchemaCrawlerOptions(options);
			executable.execute(connection);

			Assert.assertNotNull(out.toString());
			JSONObject json = new JSONObject(out.toString().substring(1, out.toString().length()-1)) ;
			Assert.assertNotNull(json.getJSONObject("table_lints"));
			Assert.assertEquals("test_json", json.getJSONObject("table_lints").getString("name"));

			JSONArray lints = json.getJSONObject("table_lints").getJSONArray("lints");
                        // only get ours... not the native lint ()
                        lints.remove(1);
			Assert.assertNotNull(lints);
			Assert.assertEquals(1,lints.length());
			Assert.assertEquals(LinterJsonContent.class.getName(), lints.getJSONObject(0).getString("id"));
			Assert.assertEquals("public.test_json.content", lints.getJSONObject(0).getString("value").trim());
			Assert.assertEquals("should be JSON or JSONB type.", lints.getJSONObject(0).getString("description").trim());
			Assert.assertEquals("high", lints.getJSONObject(0).getString("severity").trim());
		}

	}

}
