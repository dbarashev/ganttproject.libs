package com.bardsoftware.server.auth.gae;

import com.bardsoftware.server.AppUrlService;
import com.bardsoftware.server.AppUrlServiceProperties;
import com.bardsoftware.server.auth.AuthServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthServletImpl extends HttpServlet {
  private final AuthServlet myImpl;
  private final AppUrlService myUrlService;
  protected AuthServletImpl(boolean devMode) {
    myUrlService = new AppUrlServiceProperties(devMode);
    myImpl = new AuthServlet(devMode, new PrincipalExtentImpl(), new AppCapabilitiesServiceImpl(), myUrlService);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    myImpl.doGet(new HttpImpl(req, resp, myUrlService));
  }
}
