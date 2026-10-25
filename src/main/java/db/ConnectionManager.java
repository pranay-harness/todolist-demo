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
      connection = DriverManager.getConnection("jdbc:h2:mem:list;MODE=MYSQL", "sa", ""); // H2 demo database with default credentials
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized Connection getConnection() {
    return connection;
  }
}
