package register;

import db.ConnectionManager;
import db.StoredPassword;
import validation.RequestValidation;

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

  // The schema statements below are fixed, self-contained SQL constants. They are never assembled
  // at runtime and no request-supplied value is ever concatenated into them, so the SQL text these
  // statements execute cannot be influenced from outside the application.
  private static final String CREATE_ACCOUNTS_TABLE =
      "create table accounts (name varchar(32), password varchar(255))";

  private static final String CREATE_TASK_TABLE =
      "create table task (name varchar(32), thing varchar(60), priority integer, createDate varchar(80),primary key (createDate))";

  @Override
  public void init() throws ServletException {
    try {
      Connection connection = ConnectionManager.getConnection();
      Statement statement = connection.createStatement();
      statement.executeUpdate(CREATE_ACCOUNTS_TABLE);
      statement.executeUpdate(CREATE_TASK_TABLE);
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    }

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // The untrusted registration values are validated as they are read. The requested account name
    // must have the shape the accounts.name column stores, and the password and its confirmation
    // must both be present and of sane length (their contents are never restricted, trimmed or
    // truncated). Anything else is null here and fails closed to the registration error page
    // before it can reach the account lookup or the insert.
    String name = RequestValidation.accountName(request.getParameter("name"));
    String password = RequestValidation.password(request.getParameter("password"));
    String password2 = RequestValidation.password(request.getParameter("password2"));
    boolean exists = false;

    if (name == null || password == null || password2 == null || !confirmationMatches(password, password2)) {
      response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
      return;
    }

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



    if (exists) {
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

