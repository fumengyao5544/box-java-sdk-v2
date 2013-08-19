package com.box.boxjavalibv2.requests.requestobjects;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

import com.box.boxjavalibv2.httpentities.MultipartEntityWithProgressListener;
import com.box.boxjavalibv2.interfaces.IFileTransferListener;
import com.box.boxjavalibv2.jsonentities.MapJSONStringEntity;
import com.box.boxjavalibv2.utils.Constants;
import com.box.boxjavalibv2.utils.ISO8601DateParser;
import com.box.restclientv2.exceptions.BoxRestException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BoxFileUploadRequestObject extends BoxDefaultRequestObject {

    private static final String METADATA = "metadata";
    private MultipartEntityWithProgressListener entity = null;

    private static final String KEY_PARENT = "parent";
    private static final String KEY_NAME = "name";
    private static final String KEY_FILE_NAME = "filename";
    private static final String KEY_CONTENT_CREATED_AT = "content_modified_at";
    private static final String KEY_CONTENT_MODIFIED_AT = "content_modified_at";

    private BoxFileUploadRequestObject() {
    }

    /**
     * BoxFileUploadRequestObject for upload file request. Note: for uploading a new version of the file, please use uploadNewVersionRequestObject().
     * 
     * @param parentId
     *            id of the parent folder
     * @param fileName
     *            name of the file
     * @param file
     *            file to be uploaded
     * @return BoxFileUploadRequestObject
     * @throws BoxRestException
     */
    public static BoxFileUploadRequestObject uploadFileRequestObject(final String parentId, final String fileName, final File file) throws BoxRestException {
        return (new BoxFileUploadRequestObject()).setMultipartMIME(getNewFileMultipartEntity(parentId, fileName, file));
    }

    /**
     * BoxFileUploadRequestObject for upload file request. Note: for uploading a new version of the file, please use uploadNewVersionRequestObject().
     * 
     * @param parentId
     *            id of the parent folder
     * @param fileName
     *            name of the file
     * @param inputStream
     *            InputStream of the file to be uploaded
     * @return BoxFileUploadRequestObject
     * @throws BoxRestException
     */
    public static BoxFileUploadRequestObject uploadFileRequestObject(final String parentId, final String fileName, final InputStream inputStream)
        throws BoxRestException {
        return (new BoxFileUploadRequestObject()).setMultipartMIME(getNewFileMultipartEntity(parentId, inputStream, fileName));
    }

    /**
     * BoxFileUploadRequestObject for upload a new version of a file.
     * 
     * @param name
     *            name of the file
     * @param file
     *            file to be uploaded
     * @return BoxFileUploadRequestObject
     * @throws BoxRestException
     *             exception
     */
    public static BoxFileUploadRequestObject uploadNewVersionRequestObject(final String name, final File file) throws BoxRestException {
        return (new BoxFileUploadRequestObject()).setMultipartMIME(getNewVersionMultipartEntity(name, file));
    }

    /**
     * BoxFileUploadRequestObject for upload a new version of a file.
     * 
     * @param name
     *            name of the file
     * @param inputStream
     *            input stream of the file uploaded.
     * @return BoxFileUploadRequestObject
     * @throws BoxRestException
     *             exception
     */
    public static BoxFileUploadRequestObject uploadNewVersionRequestObject(final String name, final InputStream inputStream) throws BoxRestException {
        return (new BoxFileUploadRequestObject()).setMultipartMIME(getNewVersionMultipartEntity(name, inputStream));
    }

    public BoxFileUploadRequestObject setMultipartMIME(final MultipartEntityWithProgressListener mime) throws BoxRestException {
        entity = mime;
        return this;
    }

    /**
     * Set upload listener.
     * 
     * @param listener
     *            upload listener
     */
    public BoxFileUploadRequestObject setListener(IFileTransferListener listener) {
        entity.setListener(listener);
        return this;
    }

    /**
     * Set the content MD5 in the request. This is used in upload file request, it can make sure that the file is not corrupted in transit. In case of the sha1
     * is different than the sha1 calculated on server, request is going to fail.
     * 
     * @param sha1
     *            sha1
     */
    public BoxFileUploadRequestObject setContentMD5(String sha1) {
        addHeader(Constants.CONTENT_MD5, sha1);
        return this;
    }

    /**
     * Set the time that the file was locally created.
     * 
     * @param createdAt
     * @return
     */
    public BoxFileUploadRequestObject setLocalFileCreatedAt(Date createdAt) {
        try {
            entity.addPart(KEY_CONTENT_CREATED_AT, new StringBody(ISO8601DateParser.toString(createdAt)));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Set the time that the file was locally last modified.
     * 
     * @param createdAt
     * @return
     */
    public BoxFileUploadRequestObject setLocalFileLastModifiedAt(Date modifiedAt) {
        try {
            entity.addPart(KEY_CONTENT_MODIFIED_AT, new StringBody(ISO8601DateParser.toString(modifiedAt)));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * This is for upload new version request only. Set the If-Match header can be included to ensure that client only overwrites the file if it knows about the
     * latest version.
     * 
     * @param etag
     *            etag
     * @return BoxFileUploadRequestObject
     */
    public BoxFileUploadRequestObject setIfMatch(String etag) {
        addHeader(Constants.IF_MATCH, etag);
        return this;
    }

    @Override
    public HttpEntity getEntity() {
        entity.prepareParts();
        return entity;
    }

    private static MultipartEntityWithProgressListener getNewFileMultipartEntity(final String parentId, final InputStream inputStream, final String fileName)
        throws BoxRestException {
        MultipartEntityWithProgressListener me = new MultipartEntityWithProgressListener(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            me.addPart(Constants.FOLDER_ID, new StringBody(parentId));
            String date = ISO8601DateParser.toString(new Date());
            if (me.getPart(KEY_CONTENT_CREATED_AT) == null) {
                me.addPart(KEY_CONTENT_CREATED_AT, new StringBody(date));
            }
            if (me.getPart(KEY_CONTENT_MODIFIED_AT) == null) {
                me.addPart(KEY_CONTENT_MODIFIED_AT, new StringBody(date));
            }
        }
        catch (UnsupportedEncodingException e1) {
            throw new BoxRestException(e1);
        }
        me.addPart(fileName, new InputStreamBody(inputStream, fileName));

        return me;
    }

    private static MultipartEntityWithProgressListener getNewFileMultipartEntity(final String parentId, final String name, final File file)
        throws BoxRestException {
        MultipartEntityWithProgressListener me = new MultipartEntityWithProgressListener(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            me.addPart(Constants.FOLDER_ID, new StringBody(parentId));
            me.addPart(KEY_FILE_NAME, new FileBody(file, KEY_FILE_NAME, "", CharEncoding.UTF_8));
            me.addPart(METADATA, getMetadataBody(parentId, name));
            String date = ISO8601DateParser.toString(new Date(file.lastModified()));
            if (me.getPart(KEY_CONTENT_CREATED_AT) == null) {
                me.addPart(KEY_CONTENT_CREATED_AT, new StringBody(date));
            }
            if (me.getPart(KEY_CONTENT_MODIFIED_AT) == null) {
                me.addPart(KEY_CONTENT_MODIFIED_AT, new StringBody(date));
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new BoxRestException(e);
        }

        return me;
    }

    private static StringBody getMetadataBody(String parentId, String name) throws UnsupportedEncodingException, BoxRestException {
        MapJSONStringEntity parentEntity = new MapJSONStringEntity();
        parentEntity.put(Constants.ID, parentId);

        MapJSONStringEntity entity = new MapJSONStringEntity();
        entity.put(KEY_PARENT, parentEntity);
        entity.put(KEY_NAME, name);
        return new StringBody(entity.toJSONString(new ObjectMapper()), Charset.forName(CharEncoding.UTF_8));
    }

    private static MultipartEntityWithProgressListener getNewVersionMultipartEntity(final String name, final File file) {
        MultipartEntityWithProgressListener me = new MultipartEntityWithProgressListener(HttpMultipartMode.BROWSER_COMPATIBLE);
        me.addPart(name, new FileBody(file, name, "", CharEncoding.UTF_8));

        if (me.getPart(KEY_CONTENT_MODIFIED_AT) == null) {
            try {
                me.addPart(KEY_CONTENT_MODIFIED_AT, new StringBody(ISO8601DateParser.toString(new Date(file.lastModified()))));
            }
            catch (UnsupportedEncodingException e) {
            }
        }
        return me;
    }

    private static MultipartEntityWithProgressListener getNewVersionMultipartEntity(final String name, final InputStream inputStream) {
        MultipartEntityWithProgressListener me = new MultipartEntityWithProgressListener(HttpMultipartMode.BROWSER_COMPATIBLE);
        me.addPart(name, new InputStreamBody(inputStream, name));

        if (me.getPart(KEY_CONTENT_MODIFIED_AT) == null) {
            try {
                me.addPart(KEY_CONTENT_MODIFIED_AT, new StringBody(ISO8601DateParser.toString(new Date())));
            }
            catch (UnsupportedEncodingException e) {
            }
        }
        return me;
    }
}
