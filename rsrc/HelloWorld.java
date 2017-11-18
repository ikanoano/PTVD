package example;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/HelloWorld")
public class HelloWorld extends HttpServlet {
  private static final long serialVersionUID = 1L;
  /**
   * @see HttpServlet#HttpServlet()
   */
  public HelloWorld() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.getWriter().write("Hello,World");
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private HelloWorld f1(){ return new HelloWorld(); }
  private HelloWorld f2(){ return new HelloWorld(); }
  private HelloWorld f3(){ return new HelloWorld(); }
  private HelloWorld f4(){ return f1().f2().f3();}

}
