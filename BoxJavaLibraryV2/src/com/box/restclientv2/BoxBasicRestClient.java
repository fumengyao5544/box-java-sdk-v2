package com.box.restclientv2;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.interfaces.IBoxRESTClient;
import com.box.restclientv2.interfaces.IBoxRequest;
import com.box.restclientv2.interfaces.IBoxResponse;
import com.box.restclientv2.responses.DefaultBoxResponse;

/**
 * Basic implementation of the {@link IBoxRESTClient}.
 */
public class BoxBasicRestClient implements IBoxRESTClient {

    public HttpClient getRawHttpClient() {
        return new DefaultHttpClient();
    }

    /**
     * Constructor.
     */
    public BoxBasicRestClient() {
    }

    @Override
    public IBoxResponse execute(final IBoxRequest boxRequest) throws BoxRestException, AuthFatalFailureException {
        HttpUriRequest httpRequest = boxRequest.prepareRequest();
        HttpResponse response;

        try {
            response = getRawHttpClient().execute(httpRequest);
        }
        catch (Exception e) {
            throw new BoxRestException(e);
        }
        DefaultBoxResponse boxResponse = new DefaultBoxResponse(response);
        boxResponse.setExpectedResponseCode(boxRequest.getExpectedResponseCode());
        return boxResponse;
    }

}
