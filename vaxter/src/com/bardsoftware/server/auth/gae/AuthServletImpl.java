package com.bardsoftware.server.auth.gae;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bardsoftware.server.auth.AuthServlet;

public class AuthServletImpl extends HttpServlet {
  private final AuthServlet myImpl;
  
  protected AuthServletImpl(boolean devMode) {
    myImpl = new AuthServlet(devMode, new PrincipalExtentImpl(), new AppCapabilitiesServiceImpl());
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    myImpl.doGet(new HttpImpl(req, resp));
  }
}
