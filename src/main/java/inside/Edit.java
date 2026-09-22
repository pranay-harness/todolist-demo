package inside;

import db.ConnectionManager;
import security.RedirectValidator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class Edit
 */

public class Edit extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String date = request.getParameter("date");
    //		System.out.println("the parameter is " + date);
    HttpSession session = request.getSession(false);
    String name = (String) session.getAttribute("name");
    //		System.out.println("the name is " + date);
    String priority = request.getParameter("priority");
    String task = request.getParameter("task");
    if (task == null || priority == null || task.isEmpty() || priority.isEmpty()) {
      //			System.out.println("nimei!");
      // Validate that the date parameter is safe before using it in the redirect URL
      if (date != null && RedirectValidator.isValidUrlParameter(date)) {
        String link = "/inside/showEditTask.jsp?date=" + date;
        //			System.out.println("the link is " + link);
        if (RedirectValidator.isValidRedirectUrl(link)) {
          response.sendRedirect(link);
        } else {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect URL");
        }
      } else {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date parameter");
      }
    } else {
      //			System.out.println("gan!");

      try {
        Connection connection = ConnectionManager.getConnection();
        String queryString = "update task set thing = ?, priority = ? where createDate = ? and name = ?";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, task);
        statement.setString(2, priority);
        statement.setString(3, date);
        statement.setString(4, name);
        System.out.println("task is " + task);
        System.out.println("p is " + priority);
        System.out.println("time is " + date);
        System.out.println("name is " + name);
        statement.executeUpdate();
        statement.close();
        String redirectUrl = "/inside/display";
        if (RedirectValidator.isValidRedirectUrl(redirectUrl)) {
          response.sendRedirect(redirectUrl);
        } else {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect URL");
        }
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      }
    }
  }

}
