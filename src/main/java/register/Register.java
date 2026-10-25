package register;

import db.ConnectionManager;
import util.PasswordUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
      statement.executeUpdate("create table accounts (name varchar(32) UNIQUE NOT NULL, password varchar(255), salt varchar(255))");
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

    if (password == null || password.isEmpty() || name == null || name.isEmpty() || !password.equals(password2)) {
      response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
    } else {
      try {
        Connection connection = ConnectionManager.getConnection();
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        PreparedStatement statement = connection.prepareStatement("insert into accounts(name,password,salt) values(?, ?, ?)");

        statement.setString(1, name);
        statement.setString(2, hashedPassword);
        statement.setString(3, salt);

        statement.executeUpdate();

        statement.close();
        response.sendRedirect(request.getContextPath() + "/login.jsp");
      } catch (SQLException e) {
        // Check if this is a unique constraint violation (account name already exists)
        if (e.getMessage() != null && (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique"))) {
          response.sendRedirect(request.getContextPath() + "/userExists.jsp");
        } else {
          e.printStackTrace(System.out);
          response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
        }
      } catch (Exception e) {
        System.err.println("ERROR: failed to load HSQLDB JDBC driver.FUCK!");
        e.printStackTrace(System.out);
        response.sendRedirect(request.getContextPath() + "/wrongRegister.jsp");
      }
    }
  }
}

