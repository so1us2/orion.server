/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.authentication.oauth.google;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.eclipse.orion.server.authentication.oauth.OAuthConsumer;
import org.eclipse.orion.server.authentication.oauth.OAuthException;
import org.eclipse.orion.server.authentication.oauth.OAuthParams;
import org.eclipse.orion.server.authentication.oauth.OAuthTokenResponse;
import org.eclipse.orion.server.core.PreferenceHelper;

/**
 * Google specific OAuthParams containing all information related to google
 * oauth requests and responses.
 * @author Aidan Redpath
 *
 */
public class GoogleOAuthParams extends OAuthParams {

	private static final OAuthProviderType PROVIDER_TYPE = OAuthProviderType.GOOGLE;

	private static final String RESPONSE_TYPE = "code";

	private static final String SCOPE = "openid email";

	private static final String OPEN_ID_PARAMETER = "openid.realm";

	private static final GrantType GRANT_TYPE = GrantType.AUTHORIZATION_CODE;

	private static final String CLIENT_KEY = "orion.oauth.google.client";

	private static final String CLIENT_SECRET = "orion.oauth.google.secret";

	private static final Class<? extends OAuthAccessTokenResponse> TOKEN_RESPONSE_CLASS = OAuthTokenResponse.class;

	private String client_key = null;
	private String client_secret = null;

	public GoogleOAuthParams(HttpServletRequest req, boolean login) throws OAuthException {
		super(req, login);
	}

	public String getProviderName() {
		return PROVIDER_TYPE.getProviderName();
	}

	public String getAuthzEndpoint() {
		return PROVIDER_TYPE.getAuthzEndpoint();
	}

	public String getTokenEndpoint() {
		return PROVIDER_TYPE.getTokenEndpoint();
	}

	public String getClientKey() throws OAuthException {
		if(client_key == null) {
			client_key = PreferenceHelper.getString(CLIENT_KEY);
		}
		return client_key;
	}

	public String getClientSecret() throws OAuthException {
		if(client_secret == null) {
			client_secret = PreferenceHelper.getString(CLIENT_SECRET);
		}
		return client_secret;
	}

	public String getResponseType() {
		return RESPONSE_TYPE;
	}

	public String getScope() {
		return SCOPE;
	}

	public GrantType getGrantType() {
		return GRANT_TYPE;
	}

	public Class<? extends OAuthAccessTokenResponse> getTokenResponseClass() {
		return TOKEN_RESPONSE_CLASS;
	}

	public OAuthConsumer getNewOAuthConsumer(OAuthAccessTokenResponse oauthAccessTokenResponse) throws OAuthException {
		return new GoogleOAuthConsumer(oauthAccessTokenResponse, getRedirect());
	}

	public void addAdditionsParams(AuthenticationRequestBuilder requestBuiler) throws OAuthException {
		try {
			URL currentURL = getCurrentURL();
			// Add realm for openId 2.0 migration
			String realm = new URL(currentURL.getProtocol(), currentURL.getHost(), currentURL.getPort(), "").toString();
			requestBuiler.setParameter(OPEN_ID_PARAMETER, realm);
		} catch (MalformedURLException e) {
			throw new OAuthException("An Error occured while building the request URL");
		}
	}
}
