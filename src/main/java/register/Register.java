package register;

import db.ConnectionManager;

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
  @Override
  public void init() throws ServletException {
    try {
      Connection connection = ConnectionManager.getConnection();
      Statement statement = connection.createStatement();
      statement.executeUpdate("create table accounts (name varchar(32), password varchar(32))");
      statement.executeUpdate("create table task (name varchar(32), thing varchar(60), priority integer, createDate varchar(80),primary key (createDate))");
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    }

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    String name = request.getParameter("name");
    String password = request.getParameter("password");
    String password2 = request.getParameter("password2");

    if (name == null || name.isEmpty() || password == null || password.isEmpty() || !password.equals(password2)) {
      response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
      return;
    }

    boolean exists = false;

    try {
      Connection connection = ConnectionManager.getConnection();

      PreparedStatement statement = connection.prepareStatement(
          "select name from accounts where name = ?");
      statement.setString(1, name);

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        exists = true;
      }
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

