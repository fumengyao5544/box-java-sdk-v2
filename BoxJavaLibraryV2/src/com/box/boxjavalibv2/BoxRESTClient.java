package com.box.boxjavalibv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.box.boxjavalibv2.authorization.OAuthAuthorization;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxUnexpectedHttpStatusException;
import com.box.restclientv2.BoxBasicRestClient;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.interfaces.IBoxRequest;
import com.box.restclientv2.interfaces.IBoxRequestAuth;
import com.box.restclientv2.interfaces.IBoxResponse;
import com.box.restclientv2.interfaces.IBoxRestVisitor;
import com.box.restclientv2.responses.DefaultBoxResponse;

/**
 * API v2 client. By default, DefaultHttpClient is used as underlying http client. This takes visitors for requests and handles OAuth failures.
 */
public class BoxRESTClient extends BoxBasicRestClient {

    private static AtomicInteger apiSequenceId = new AtomicInteger(0);
    public final static String OAUTH_ERROR_HEADER = "error";
    public final static String OAUTH_INVALID_TOKEN = "invalid_token";
    public final static String WWW_AUTHENTICATE = "WWW-Authenticate";
    private final List<IBoxRestVisitor> visitors = new ArrayList<IBoxRestVisitor>();

    private int clientTimeOut = -1;

    /**
     * Accept a visitor to visit http request/response.
     * 
     * @param visitor
     *            visitor
     */
    public void acceptRestVisitor(final IBoxRestVisitor visitor) {
        visitors.add(visitor);
    }

    @Override
    public IBoxResponse execute(final IBoxRequest boxRequest) throws BoxRestException, AuthFatalFailureException {
        IBoxRequestAuth auth = boxRequest.getAuth();
        return execute(boxRequest, auth instanceof OAuthAuthorization);
    }

    /**
     * Execute a request.
     * 
     * @param boxRequest
     *            IBoxRequest
     * @param usingOAuth
     *            whether OAuth is used.
     * @return IBoxResponse
     * @throws BoxRestException
     *             exception
     * @throws AuthFatalFailureException
     */
    private IBoxResponse execute(final IBoxRequest boxRequest, final boolean usingOAuth) throws BoxRestException, AuthFatalFailureException {
        HttpUriRequest httpRequest = boxRequest.prepareRequest();
        HttpResponse response = null;

        int sequenceId = apiSequenceId.incrementAndGet();
        for (IBoxRestVisitor v : visitors) {
            v.visitRequestBeforeSend(httpRequest, sequenceId);
        }

        try {
            response = getResponse(httpRequest);
            for (IBoxRestVisitor v : visitors) {
                v.visitResponseUponReceiving(response, sequenceId);
            }

            if (usingOAuth && oauthExpired(response)) {
                try {
                    return handleOAuthTokenExpire((OAuthAuthorization) boxRequest.getAuth(), boxRequest);
                }
                catch (AuthFatalFailureException e) {
                    // Swallow the OAuthRefreshFailException here, the response will be parsed and caller will see the unauthorized error.
                    for (IBoxRestVisitor v : visitors) {
                        v.visitException(e, sequenceId);
                    }
                    throw e;
                }
            }
        }
        catch (IOException e) {
            handleException(e, sequenceId);
        }
        catch (BoxUnexpectedHttpStatusException e) {
            handleException(e, sequenceId);
        }

        DefaultBoxResponse boxResponse = new DefaultBoxResponse(response);
        boxResponse.setExpectedResponseCode(boxRequest.getExpectedResponseCode());
        return boxResponse;
    }

    public void setConnectionTimeOut(final int timeOut) {
        this.clientTimeOut = timeOut;
    }

    protected HttpResponse getResponse(HttpUriRequest request) throws ClientProtocolException, IOException {
        HttpClient client = getRawHttpClient();
        if (clientTimeOut > 0) {
            HttpParams params = client.getParams();
            HttpConnectionParams.setConnectionTimeout(params, clientTimeOut);
        }
        return client.execute(request);
    }

    private boolean oauthExpired(HttpResponse response) {
        if (HttpStatus.SC_UNAUTHORIZED != response.getStatusLine().getStatusCode()) {
            return false;
        }
        org.apache.http.Header header = response.getFirstHeader(WWW_AUTHENTICATE);
        if (header != null) {
            String authStr = header.getValue();
            String[] authStrs = authStr.split(",");
            for (String str : authStrs) {
                if (isInvalidTokenError(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInvalidTokenError(String str) {
        String[] parts = str.split("=");
        if (parts.length == 2 && parts[0] != null && parts[1] != null) {
            if (OAUTH_ERROR_HEADER.equalsIgnoreCase(parts[0].trim()) && OAUTH_INVALID_TOKEN.equalsIgnoreCase(parts[1].replace("\"", "").trim())) {
                return true;

            }
        }
        return false;
    }

    private void handleException(final Exception e, final int sequenceId) throws BoxRestException {
        for (IBoxRestVisitor v : visitors) {
            v.visitException(e, sequenceId);
        }
        throw new BoxRestException(e);
    }

    /**
     * Handle an OAuth failure.
     * 
     * @param auth
     *            auth
     * @param boxRequest
     *            request
     * @return response
     * @throws AuthFatalFailureException
     *             exception
     * @throws BoxRestException
     *             exception
     * @throws BoxUnexpectedHttpStatusException
     *             exception
     */
    private IBoxResponse handleOAuthTokenExpire(final OAuthAuthorization auth, final IBoxRequest boxRequest) throws AuthFatalFailureException,
        BoxRestException, BoxUnexpectedHttpStatusException {
        auth.refresh();
        return execute(boxRequest, true);
    }
}
