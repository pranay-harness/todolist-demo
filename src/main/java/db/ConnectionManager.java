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
      String url = resolve(URL_PROPERTY, URL_ENV);
      String user = resolve(USER_PROPERTY, USER_ENV);
      String password = resolve(PASSWORD_PROPERTY, PASSWORD_ENV);

      if (url == null) {
        // Nothing is configured, so use the throw-away embedded in-memory database. It is created
        // inside this JVM only and is not reachable by any other process, so the app can still be
        // started locally without provisioning credentials.
        url = DEFAULT_URL;
        if (user == null) {
          user = DEFAULT_USER;
        }
        if (password == null) {
          password = "";
        }
      } else if (user == null || password == null || password.isEmpty()) {
        // A datasource was explicitly pointed at something other than the in-JVM default, so it
        // must be authenticated. Fail closed rather than silently opening an unauthenticated
        // connection to a real database (CWE-306).
        throw new IllegalStateException(
            "A datasource is configured through " + URL_PROPERTY + " / " + URL_ENV
                + " but no credentials were supplied. Provide " + USER_PROPERTY + " / " + USER_ENV
                + " and a non-empty " + PASSWORD_PROPERTY + " / " + PASSWORD_ENV
                + " from your secrets manager before starting the application.");
      }

      Properties connectionProperties = new Properties();
      connectionProperties.setProperty("user", user);
      connectionProperties.setProperty("password", password);
      connection = DriverManager.getConnection(url, connectionProperties);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the configured value for the given system property, falling back to the environment
   * variable, or {@code null} when neither of them supplies a value.
   */
  private static String resolve(String propertyName, String environmentName) {
    String value = System.getProperty(propertyName);
    if (value == null || value.isEmpty()) {
      value = System.getenv(environmentName);
    }
    return value;
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
