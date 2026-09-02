package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by peeyushaggarwal on 9/7/16.
 */
public class ConnectionManager {
  private static final Connection connection;
  static {
    try {
      Class.forName("org.h2.Driver");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      // Password must be supplied via the DB_PASSWORD environment variable or the
      // db.password system property. The credential has been removed from source
      // control and MUST be rotated (revoked/reissued) because it existed in git history.
      String dbPassword = System.getenv("DB_PASSWORD");
      if (dbPassword == null) {
        dbPassword = System.getProperty("db.password", "");
      }
      connection = DriverManager.getConnection("jdbc:h2:mem:list;MODE=MYSQL", "sa", dbPassword);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
