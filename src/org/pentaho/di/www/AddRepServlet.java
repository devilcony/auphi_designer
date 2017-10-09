package org.pentaho.di.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AddRepServlet extends BaseHttpServlet implements
		CarteServletInterface {
	
	private static final long serialVersionUID = -3110318147950638718L;
	
	public static final String CONTEXT_PATH = "/kettle/addRep";
	
	public AddRepServlet() {
	}

	public AddRepServlet(TransformationMap transformationMap) {
	    super(transformationMap);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    if (isJettyMode() && !request.getRequestURI().startsWith(CONTEXT_PATH))
	      return;
	    
	    repBean.setRepositoryName(request.getParameter("repName")==null?"":request.getParameter("repName"));
	    repBean.setVersion(request.getParameter("repVersion")==null?"":request.getParameter("repVersion"));
	    repBean.setDbType(request.getParameter("repDbType")==null?"":request.getParameter("repDbType"));
	    repBean.setDbHost(request.getParameter("repDbHost")==null?"":request.getParameter("repDbHost"));
	    repBean.setDbPort(request.getParameter("repDbPort")==null?"":request.getParameter("repDbPort"));
	    repBean.setDbName(request.getParameter("repDbName")==null?"":request.getParameter("repDbName"));
	    repBean.setUserName(request.getParameter("repDbUsername")==null?"":request.getParameter("repDbUsername"));
	    repBean.setPassword(request.getParameter("repDbPassword")==null?"":request.getParameter("repDbPassword"));
	    repBean.setDbAccess("Native");
	    repBean.setRepositoryID(0);
	    
	    return;
	}
	
	public String toString() {
	    return "Add Repository";
	}

	public String getService() {
	    return CONTEXT_PATH + " (" + toString() + ")";
	}
	
}
