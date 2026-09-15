package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
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
      String url = databaseUrl();
      connection = DriverManager.getConnection(url, connectionProperties(url));
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

  private static Properties connectionProperties(String url) {
    Properties properties = new Properties();
    String user = setting("DB_USER", "db.user");
    // Credentials are never stored in code: they are supplied at runtime from the DB_USER /
    // DB_PASSWORD environment variables (or the db.user / db.password system properties), which
    // should be populated from a KMS / secrets manager.
    String password = setting("DB_PASSWORD", "db.password");
    if (!isProcessPrivateEmbeddedDatabase(url)) {
      // Fail closed: any database that is not the process-private in-memory instance can be
      // reached from outside this JVM, so it must never be opened anonymously or with the
      // built-in default account and an empty password.
      requireCredential(user, "DB_USER", "db.user");
      requireCredential(password, "DB_PASSWORD", "db.password");
    }
    properties.setProperty("user", user == null ? DEFAULT_USER : user);
    if (password != null) {
      properties.setProperty("password", password);
    }
    return properties;
  }

  /**
   * Returns true only for the embedded H2 in-memory database, which lives inside this JVM, is
   * discarded when the process exits and cannot be reached over the network. Anything else
   * (a file-backed database, an H2 server, MySQL, ...) is treated as external and requires
   * authentication. An in-memory URL that opens a listening server (AUTO_SERVER) or names a
   * remote host is deliberately not considered process-private.
   */
  private static boolean isProcessPrivateEmbeddedDatabase(String url) {
    String normalized = url.toLowerCase(Locale.ENGLISH);
    return normalized.startsWith("jdbc:h2:mem:")
        && !normalized.contains("//")
        && !normalized.contains("auto_server");
  }

  private static void requireCredential(String value, String environmentVariable,
      String systemProperty) {
    if (value == null) {
      // The URL is intentionally left out of the message: it may itself carry credentials.
      throw new IllegalStateException("An external database is configured but no credentials were"
          + " supplied: refusing to open a database connection without authentication. Set the "
          + environmentVariable + " environment variable (or the " + systemProperty
          + " system property) from your secrets manager.");
    }
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
