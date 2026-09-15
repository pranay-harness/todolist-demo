package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by peeyushaggarwal on 9/7/16.
 */
public class ConnectionManager {
  /** Embedded in-memory database used when no external database is configured. */
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
      connection = DriverManager.getConnection(databaseUrl(), connectionProperties());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized Connection getConnection() {
    return connection;
  }

  private static String databaseUrl() {
    String url = setting("DB_URL", "db.url");
    return url == null ? DEFAULT_URL : url;
  }

  private static Properties connectionProperties() {
    Properties properties = new Properties();
    String user = setting("DB_USER", "db.user");
    properties.setProperty("user", user == null ? DEFAULT_USER : user);
    // The database password is never stored in code: it is supplied at runtime from the
    // DB_PASSWORD environment variable (or the db.password system property), which should be
    // populated from a KMS / secrets manager. When it is not supplied the embedded in-memory
    // H2 database is opened without a password, so local runs keep working unchanged.
    String password = setting("DB_PASSWORD", "db.password");
    if (password != null) {
      properties.setProperty("password", password);
    }
    return properties;
  }

  private static String setting(String environmentVariable, String systemProperty) {
    String value = System.getenv(environmentVariable);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(systemProperty);
    }
    if (value == null || value.isEmpty()) {
      return null;
    }
    return value;
  }
}
