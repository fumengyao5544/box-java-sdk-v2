package com.box.restclientv2.responseparsers;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.interfaces.IBoxResponse;
import com.box.restclientv2.interfaces.IBoxResponseParser;
import com.box.restclientv2.responses.DefaultBoxResponse;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is a wrapper class in order for <a href="http://jackson.codehaus.org/">Jackson JSON processor</a> to parse response JSON into the wrapped objects.
 */
@SuppressWarnings("rawtypes")
public class DefaultBoxJSONResponseParser implements IBoxResponseParser {

    private final Class objectClass;

    private final ObjectMapper mObjectMapper;

    /**
     * Constructor.
     * 
     * @param objectClass
     *            class of the wrapped object. Response from API will be parsed into this object, caller needs to make sure this class has fields with same
     *            names as the JSON part of API response. If class contains more fields than the JSON, those fields will be left null. If the class contains
     *            less fields than the JSON, the parsing will error out.
     * @param objectMapper
     *            ObjectMapper to be used when parsing.
     */
    public DefaultBoxJSONResponseParser(Class objectClass, ObjectMapper objectMapper) {
        this.objectClass = objectClass;
        this.mObjectMapper = objectMapper;
    }

    /**
     * Class of the wrapped object.
     * 
     * @return class of the wrapped object.
     */
    public Class getObjectClass() {
        return objectClass;
    }

    /**
     * By default, this only parses the JSON part into object. In case caller needs more information(i.e. information from headers) into this object, caller can
     * extend this class and implement the extraParses(HttpResponse httpResponse) method
     * 
     * @throws BoxRestException
     */
    @Override
    public Object parse(IBoxResponse response) throws BoxRestException {
        if (!(response instanceof DefaultBoxResponse)) {
            throw new BoxRestException("class mismatch, expected:" + DefaultBoxResponse.class.getName() + ";current:" + response.getClass().getName());
        }
        HttpResponse httpResponse = ((DefaultBoxResponse) response).getHttpResponse();

        InputStream in = null;
        try {
            in = httpResponse.getEntity().getContent();
            if (in == null) {
                return null;
            }
            else {
                return parseInputStream(in);
            }
        }
        catch (Exception e) {
            throw new BoxRestException(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Parse input stream.
     * 
     * @param in
     *            input stream.
     * @throws BoxRestException
     */
    @SuppressWarnings("unchecked")
    private Object parseInputStream(InputStream in) throws BoxRestException {

        JsonFactory jsonFactory = new JsonFactory();
        try {
            JsonParser jp = jsonFactory.createJsonParser(in);
            return mObjectMapper.readValue(jp, objectClass);
        }
        catch (Exception e) {
            throw new BoxRestException(e, e.getMessage());
        }
    }
}
