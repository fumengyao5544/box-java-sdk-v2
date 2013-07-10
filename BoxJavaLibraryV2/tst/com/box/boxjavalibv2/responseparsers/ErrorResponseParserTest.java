package com.box.boxjavalibv2.responseparsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.box.boxjavalibv2.dao.BoxServerError;
import com.box.boxjavalibv2.jacksonparser.BoxResourceHub;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.responses.DefaultBoxResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorResponseParserTest {

    private BoxServerError error;
    private DefaultBoxResponse boxResponse;
    private HttpResponse response;
    private HttpEntity entity;
    private InputStream inputStream;
    private StatusLine statusLine;
    private final int statusCode = HttpStatus.SC_FORBIDDEN;

    @Before
    public void setUp() {
        error = new BoxServerError();
        error.setHttpStatusCode(HttpStatus.SC_FORBIDDEN);
        boxResponse = EasyMock.createMock(DefaultBoxResponse.class);
        response = EasyMock.createMock(BasicHttpResponse.class);
        entity = EasyMock.createMock(StringEntity.class);
        statusLine = EasyMock.createMock(StatusLine.class);
    }

    @Test
    public void testCanParseBoxServerError() throws BoxRestException, IllegalStateException, IOException {
        ObjectMapper mapper = (new BoxResourceHub()).getObjectMapper();
        EasyMock.reset(boxResponse, response, entity);
        inputStream = new ByteArrayInputStream(error.toJSONString(mapper).getBytes());
        EasyMock.expect(boxResponse.getHttpResponse()).andReturn(response);
        EasyMock.expect(response.getEntity()).andReturn(entity);
        EasyMock.expect(entity.getContent()).andReturn(inputStream);

        EasyMock.expect(boxResponse.getHttpResponse()).andReturn(response);
        EasyMock.expect(response.getStatusLine()).andReturn(statusLine);
        EasyMock.expect(statusLine.getStatusCode()).andReturn(statusCode);

        EasyMock.replay(boxResponse, response, entity, statusLine);
        ErrorResponseParser parser = new ErrorResponseParser(mapper);
        Object object = parser.parse(boxResponse);
        Assert.assertEquals(BoxServerError.class, object.getClass());

        Assert.assertEquals(error.toJSONString(mapper), ((BoxServerError) object).toJSONString(mapper));
        EasyMock.verify(boxResponse, response, entity, statusLine);
    }
}
