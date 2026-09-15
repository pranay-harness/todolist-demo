package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by peeyushaggarwal on 9/7/16.
 */
public class ConnectionManager {
  /** System property / environment variable names used to supply the datasource settings. */
  private static final String URL_PROPERTY = "todolist.db.url";
  private static final String URL_ENV = "TODOLIST_DB_URL";
  private static final String USER_PROPERTY = "todolist.db.user";
  private static final String USER_ENV = "TODOLIST_DB_USER";
  private static final String PASSWORD_PROPERTY = "todolist.db.password";
  private static final String PASSWORD_ENV = "TODOLIST_DB_PASSWORD";

  /** Default embedded, in-memory datasource used when nothing is configured (no credentials). */
  private static final String DEFAULT_URL = "jdbc:h2:mem:list;MODE=MYSQL";
  private static final String DEFAULT_USER = "sa";

  private static final Connection connection;
  static {
    try {
      Class.forName("org.h2.Driver");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      // Credentials are never hard-coded here: they are read from a system property or the
      // environment (populate them from your KMS / secrets manager at deploy time) so that
      // they can be audited and rotated outside of source control.
      Properties connectionProperties = new Properties();
      connectionProperties.setProperty("user", resolve(USER_PROPERTY, USER_ENV, DEFAULT_USER));
      connectionProperties.setProperty("password", resolve(PASSWORD_PROPERTY, PASSWORD_ENV, ""));
      connection =
          DriverManager.getConnection(resolve(URL_PROPERTY, URL_ENV, DEFAULT_URL), connectionProperties);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the configured value for the given system property, falling back to the environment
   * variable and finally to the supplied default for the embedded in-memory database.
   */
  private static String resolve(String propertyName, String environmentName, String defaultValue) {
    String value = System.getProperty(propertyName);
    if (value == null || value.isEmpty()) {
      value = System.getenv(environmentName);
    }
    return (value == null) ? defaultValue : value;
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
