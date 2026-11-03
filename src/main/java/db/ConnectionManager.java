package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by peeyushaggarwal on 9/7/16.
 *
 * <p>The JDBC url, user and password are read from the environment (or from an equally
 * named system property) instead of being written into the source, because credentials
 * committed to a repository are readable by everyone with access to it and cannot be
 * rotated without a code change, which is Use of Hardcoded Credentials (CWE-798).</p>
 *
 * <p>Configuration keys, all optional for local runs:</p>
 * <ul>
 *   <li>{@code TODOLIST_DB_URL} - JDBC url, defaults to the embedded in-memory H2
 *       database the app ships with.</li>
 *   <li>{@code TODOLIST_DB_USER} - database user, defaults to the H2 built-in
 *       {@code sa} account of that throw-away in-memory database.</li>
 *   <li>{@code TODOLIST_DB_PASSWORD} - database password. Required whenever
 *       {@code TODOLIST_DB_URL} points at anything other than the embedded default: the
 *       connection then fails closed rather than trying an empty password.</li>
 * </ul>
 */
public class ConnectionManager {

  private static final String URL_KEY = "TODOLIST_DB_URL";
  private static final String USER_KEY = "TODOLIST_DB_USER";
  private static final String PASSWORD_KEY = "TODOLIST_DB_PASSWORD";

  /**
   * Embedded H2 database that is created empty inside this JVM on every start, so it
   * holds no data worth protecting and needs no deployment-specific credential.
   */
  private static final String DEFAULT_URL = "jdbc:h2:mem:list;MODE=MYSQL";
  private static final String DEFAULT_USER = "sa";

  private static final Connection connection;
  static {
    try {
      Class.forName("org.h2.Driver");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String url = setting(URL_KEY, DEFAULT_URL);
    String user = setting(USER_KEY, DEFAULT_USER);
    String password = databasePassword(url);
    try {
      connection = DriverManager.getConnection(url, user, password);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads a configuration value from the environment, then from the system properties.
   *
   * @param name key to look up
   * @param defaultValue value to use when the key is absent or blank, may be null
   * @return the configured value, or {@code defaultValue}
   */
  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.length() == 0) {
      value = System.getProperty(name);
    }
    if (value == null || value.length() == 0) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Resolves the database password for the configured url.
   *
   * @param url JDBC url the connection is opened against
   * @return the configured password, or the empty password of the embedded default
   * @throws IllegalStateException when a non-default database is configured without a
   *     password, so a misconfigured deployment fails closed instead of connecting with
   *     a guessable credential
   */
  private static String databasePassword(String url) {
    String configured = setting(PASSWORD_KEY, null);
    if (configured != null) {
      return configured;
    }
    if (DEFAULT_URL.equals(url)) {
      return "";
    }
    throw new IllegalStateException(PASSWORD_KEY + " must be set when " + URL_KEY
        + " points at a database other than the embedded in-memory default");
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
