package db;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;

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

  /**
   * Default local/dev target: embedded in-memory H2, private to this JVM and
   * discarded when it exits. This process creates that database, so it is
   * created with a password generated at start-up rather than with none.
   */
  private static final String DEFAULT_URL = "jdbc:h2:mem:list;MODE=MYSQL";
  private static final String DEFAULT_USER = "sa";

  /** URL prefix of an in-process H2 database that no other process can reach. */
  private static final String EMBEDDED_URL_PREFIX = "jdbc:h2:mem:";

  private static final Connection connection;
  static {
    try {
      Class.forName("org.h2.Driver");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Credentials are supplied at runtime via system properties / environment
    // variables (KMS, container secret or CI variable) instead of being
    // hard-coded here.
    String url = setting(URL_PROPERTY, URL_ENV, DEFAULT_URL);
    String user = setting(USER_PROPERTY, USER_ENV, DEFAULT_USER);
    String password = setting(PASSWORD_PROPERTY, PASSWORD_ENV, null);
    if (password == null) {
      if (!isPrivateEmbeddedDatabase(url)) {
        // Fail closed: never authenticate to a database server we do not own
        // with a missing or empty password.
        throw new IllegalStateException("A database password must be supplied through the "
            + PASSWORD_PROPERTY + " system property or the " + PASSWORD_ENV
            + " environment variable when " + URL_PROPERTY + " targets a database server.");
      }
      // Zero-config local/CI run: this connection creates the embedded database,
      // so protect it with a fresh random password instead of leaving it open.
      password = generatedPassword();
    }
    try {
      connection = DriverManager.getConnection(url, user, password);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isPrivateEmbeddedDatabase(String url) {
    String normalized = url.toLowerCase(Locale.ROOT);
    // AUTO_SERVER publishes the in-memory database on the network, where an
    // unauthenticated session would be remotely reachable.
    return normalized.startsWith(EMBEDDED_URL_PREFIX) && !normalized.contains("auto_server");
  }

  private static String generatedPassword() {
    byte[] material = new byte[32];
    new SecureRandom().nextBytes(material);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(material);
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
