package register;

import db.ConnectionManager;
import validation.RequestValidator;

import java.io.IOException;
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

  /**
   * Schema bootstrap DDL. These are complete, immutable compile-time constants: the statement text
   * is never assembled from anything at runtime, so no caller-supplied value can alter the command
   * that is sent to the database. DDL cannot bind identifiers or type definitions as
   * {@link PreparedStatement} parameters, so the safe form here is fixed SQL rather than a
   * parameterized query.
   */
  private static final String CREATE_ACCOUNTS_TABLE =
      "create table accounts (name varchar(32), password varchar(32))";
  private static final String CREATE_TASK_TABLE =
      "create table task (name varchar(32), thing varchar(60), priority integer, createDate varchar(80),primary key (createDate))";

  @Override
  public void init() throws ServletException {
    Connection connection = ConnectionManager.getConnection();
    // try-with-resources so the statement is released even when the database rejects the DDL.
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(CREATE_ACCOUNTS_TABLE);
      statement.executeUpdate(CREATE_TASK_TABLE);
    } catch (SQLException e) {
      // On a warm database the tables already exist and the first create fails; that is expected
      // and must not prevent the servlet from starting, exactly as before.
      e.printStackTrace(System.out);
    }

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    String name = request.getParameter("name");
    String password = request.getParameter("password");
    String password2 = request.getParameter("password2");
    boolean exists = false;

    // Validate the submitted account details at the trust boundary, before they are used to query
    // the database or to create an account. The allowlists match the accounts table column widths,
    // and the confirmation still has to equal the password, so empty or mismatched passwords are
    // rejected on the same page as before.
    if (!RequestValidator.isAccountName(name)
        || !RequestValidator.isPassword(password)
        || !RequestValidator.isPassword(password2)
        || !password.equals(password2)) {
      response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
      return;
    }

    try {
      Connection connection = ConnectionManager.getConnection();

      // The "is this name already taken" check binds the submitted name as a parameter so it is
      // never concatenated into the SQL text, and the database filters the row server-side.
      try (PreparedStatement statement = connection.prepareStatement("select name from accounts where name = ?")) {
        statement.setString(1, name);

        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            if (resultSet.getString(1).equals(name))
              exists = true;
          }
        }
      }
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
        statement.setString(2, password);

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
}

