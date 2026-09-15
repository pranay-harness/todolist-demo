package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by peeyushaggarwal on 9/7/16.
 */
public class ConnectionManager {
  private static final String URL_PROPERTY = "todolist.db.url";
  private static final String USER_PROPERTY = "todolist.db.user";
  private static final String PASSWORD_PROPERTY = "todolist.db.password";

  private static final String URL_ENV = "TODOLIST_DB_URL";
  private static final String USER_ENV = "TODOLIST_DB_USER";
  private static final String PASSWORD_ENV = "TODOLIST_DB_PASSWORD";

  /** Default local/dev target: embedded in-memory H2, discarded when the JVM exits. */
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
      // Credentials are supplied at runtime via system properties / environment
      // variables (KMS, container secret or CI variable) instead of being
      // hard-coded here. With nothing configured we fall back to the embedded
      // in-memory H2 instance, which has no password to protect.
      connection = DriverManager.getConnection(
          setting(URL_PROPERTY, URL_ENV, DEFAULT_URL),
          setting(USER_PROPERTY, USER_ENV, DEFAULT_USER),
          setting(PASSWORD_PROPERTY, PASSWORD_ENV, ""));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static String setting(String propertyName, String environmentName, String defaultValue) {
    String value = System.getProperty(propertyName);
    if (value == null || value.isEmpty()) {
      value = System.getenv(environmentName);
    }
    return (value == null || value.isEmpty()) ? defaultValue : value;
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
