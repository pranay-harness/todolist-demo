package register;

import db.ConnectionManager;
import db.StoredPassword;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class Register
 */


public class Register extends HttpServlet {
  @Override
  public void init() throws ServletException {
    try {
      Connection connection = ConnectionManager.getConnection();
      Statement statement = connection.createStatement();
      statement.executeUpdate("create table accounts (name varchar(32)," + " password varchar(255))");
      statement
          .executeUpdate("create table task (name varchar(32)," + " thing varchar(60), priority integer, createDate varchar(80),primary key (createDate))");
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    }

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    String name = request.getParameter("name");
    String password = request.getParameter("password");
    String password2 = request.getParameter("password2");
    boolean exists = false;


    try {
      Connection connection = ConnectionManager.getConnection();

      // The account name is bound as a parameter so it can never become part of the SQL text.
      String queryString = "select name from accounts where name = ?";
      PreparedStatement statement = connection.prepareStatement(queryString);
      statement.setString(1, name);

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next())
        exists = true;

      resultSet.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    } catch (Exception e) {
      System.err.println("ERROR: failed to load HSQLDB JDBC driver.FUCK!");
      e.printStackTrace(System.out);
    }



    if (password == null || password.isEmpty() || name == null || name.isEmpty()
        || !confirmationMatches(password, password2)) {
      response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
    } else if (exists) {
      response.sendRedirect(request.getContextPath() + "/userExists.jsp");
    } else {
      try {
        Connection connection = ConnectionManager.getConnection();
        PreparedStatement statement = connection.prepareStatement("insert into accounts(name,password) values(?, ?)");

        statement.setString(1, name);
        // The cleartext password is never persisted: only a salted, iterated one-way hash of it is
        // stored, so the table cannot be used to recover the password it was created from.
        statement.setString(2, StoredPassword.hash(password));

        statement.executeUpdate();

        statement.close();
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      } catch (Exception e) {
        System.err.println("ERROR: failed to load HSQLDB JDBC driver.FUCK!");
        e.printStackTrace(System.out);
      }
      response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
  }

  /**
   * Returns true when the chosen password and its confirmation are identical. The two values are
   * compared in constant time so the check cannot be used to learn the password character by
   * character. A missing confirmation never matches.
   */
  private static boolean confirmationMatches(String password, String confirmation) {
    if (confirmation == null) {
      return false;
    }
    return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
        confirmation.getBytes(StandardCharsets.UTF_8));
  }
}

