/*
 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
 */

package org.eclipse.orion.server.environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.orion.server.core.LogHelper;
import org.eclipse.orion.server.core.OrionConfiguration;
import org.eclipse.orion.server.core.PreferenceHelper;
import org.eclipse.orion.server.core.metastore.ProjectInfo;
import org.eclipse.orion.server.servlets.OrionServlet;

/**
 * Servlet to handle all execution requests.
 *
 * @author mwlodarczyk
 */
public final class ExecutionServlet extends OrionServlet {

	private static final long serialVersionUID = 5345085797302519095L;

	private static final String COMMAND = "command"; //$NON-NLS-1$
	private static final String CONFIG_EXECUTION_ENABLED = "plugin.execution.enabled"; //$NON-NLS-1$
	private static final String CONFIG_FILE_NAME = "execution.conf"; //$NON-NLS-1$

	@Override	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		traceRequest(req);
		if(!"true".equals(PreferenceHelper.getString(CONFIG_EXECUTION_ENABLED, ""))) { //$NON-NLS-1$ //$NON-NLS-2$
			displayError(req, resp, "Execution environment is disabled.");
			return;
		}

		String commandType = req.getParameter(COMMAND);
		String userID = req.getRemoteUser();
		String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo(); //$NON-NLS-1$
		IPath path = new Path(pathInfo);
		IFileStore configFileStore = null, argFileStore = null;

		// the path's form is /workspaceName/projectName[/filePath]&command=(...)
		if (!ShellEnvironment.CANCEL.equals(commandType)) {
			try {
				IFileStore project = getProjectStore(path);
				configFileStore = project.getChild(CONFIG_FILE_NAME);
				argFileStore = project.getFileStore(path.removeFirstSegments(2));
			} catch (CoreException e) {
				LogHelper.log(e);
				displayError(req, resp, e.getMessage());
				return;
			} catch (InvalidPathException e) {
				LogHelper.log(e);
				displayError(req, resp, e.getMessage());
				return;
			}
		}

		// get the project configuration
		ExecutionConfiguration config = new ExecutionConfiguration();
		if (configFileStore != null) {
			try {
				File configFile = configFileStore.toLocalFile(0, null);
				if (configFile.isFile()) {
					config = new ExecutionConfiguration(configFile);
					resp.getWriter().println("Configuration file loaded.\n");
				} else {
					resp.getWriter().println("Using default configuration.\n");
				}
			} catch (CoreException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// perform the security filtering
		if(!ShellEnvironment.CANCEL.equals(commandType)) {
			try {
				File argFile = argFileStore.toLocalFile(0, null);
				if(!argFile.isFile()) {
					displayError(req, resp, "The file does not exist.");
					return;
				}
				String filterResult = PythonSecurityFilter.getInstance().doFilter(argFile);
				if(!filterResult.isEmpty()) {
					displayError(req, resp, filterResult);
					return;
				}
			} catch (CoreException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// execute and print
		List<String> out = ShellEnvironmentRegistry.getEnvironmentForUser(userID).execute(commandType, argFileStore, config);
		for (String line: out) {
			resp.getWriter().println(line);
		}
	}

	/**
	 * Retrieves file store from the path.
	 *
	 * @param path
	 *            Taken from the GET request.
	 * @return File store object pointing to a file in an existing project.
	 * @throws CoreException
	 * @throws InvalidPathException
	 */
	private IFileStore getProjectStore(IPath path) throws CoreException, InvalidPathException {
		String wrongProjectMsg = "Path should be of form /workspaceName/projectName[/filePath]&command=(...)";
		if (path.segmentCount() <= 1) {
			throw new InvalidPathException(path.toString(), wrongProjectMsg);
		} else {
			ProjectInfo projectInfo = OrionConfiguration.getMetaStore().readProject(path.segment(0), path.segment(1));			
			if (projectInfo == null) {
				throw new InvalidPathException(path.toString(), wrongProjectMsg);
			} else {	
				return projectInfo.getProjectStore();
			}
		} 
	}
	
	private void displayError(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		resp.getWriter().println(msg);
		resp.flushBuffer();
	}
	
	public ExecutionServlet() {
		super();
	}
}