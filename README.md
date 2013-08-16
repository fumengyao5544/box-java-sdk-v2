Box Java SDK
=============

Building
--------

### Eclipse

To build from Eclipse, simply import the project into your workspace
as an existing project.

### Ant

The easiest way of building with Ant is by running `ant` from the
BoxJavaLibraryV2 directory. This will output a JAR to
`dist/debug/BoxJavaLibraryV2.jar`. You can see a full list of additional targets
by running `ant -p`.

	$ ant -p

	Main targets:

	clean    Removes any built files.
	debug    Performs a debug build.
	release  Performs a release build.
	test     Performs a debug build and then runs tests.
	Default target: debug

### Gradle (Experimental)

There is also experimental support for Gradle, allowing you to use the SDK with
Android Studio. You must have [Gradle 1.6](http://www.gradle.org/downloads)
installed.

Running `gradle build` will build the SDK and run its tests. A JAR will be
placed in `build/libs/BoxJavaLibraryV2-1.0.jar`. Alternatively, you can run
`gradle install` which will install the SDK to you local Maven repository. It
can then be referenced from other projects with the dependency string
`com.box.boxjavalibv2:BoxJavaLibraryV2:1.0`.

**Note for Android users:** You might get a warning that says "WARNING:
Dependency commons-logging :commons-logging:1.1.1 is ignored for the default
configuration as it may be conflicting with the internal version provided by
Android." This is expected and shouldn't affect your build.

API Calls Quickstart
--------------------

### Hello World

You can find a hello world example [here][hello-world].

### Authenticate

Authenticate the client with OAuth. The SDK does not currently provide an
authentication UI out of the box.  You can use your own UI to get the
authentication code. After you have auth code, you can use the SDK OAuthManager
to get a BoxOAuthToken:

```java
boxClient.authenticate(boxOAuthToken);
```

Our sdk auto refreshes OAuth access token when it expires. You will want to listen to the refresh events and update your stored token after refreshing.
```java
boxClient.addOAuthRefreshListener(OAuthRefreshListener listener) {
    new OAuthRefreshListener() {
        @Override
        public void onRefresh(IAuthData newAuthData) {
            // TODO: save the auth data.
        }						       
    }
}
```

After you exit the app and return back, you can use the stored oauth data to authenticate:
```java
boxClient.authenticate(loadStoredAuthData);
``` 


For more details please see the [hello world example][hello-world].

### Get Default File Info

```java
BoxFile boxFile = boxClient.getFilesManager().getFile(fileId, null);
```

### Get Additional File Info

Get default file info plus its description and SHA1.

```java
BoxDefaultRequestObject requestObj =
  (new BoxDefaultRequestObject()).addField(BoxFile.FIELD_SHA1);
		.addField(BoxFile.FIELD_DESCRIPTION);
BoxFile boxFile = boxClient.getFilesManager().getFile(fileId, requestObj);
```

### Get Folder Children

Get 30 child items, starting from the 20th one, requiring etag, description, and
name to be included.

```java
BoxFolderRequestObject requestObj = 
	BoxFolderRequestObject.getFolderItemsRequestObject(30, 20)
		.addField(BoxFolder.FIELD_NAME)
		.addField(BoxFolder.FIELD_DESCRIPTION)
		.addField(BoxFolder.FIELD_ETAG);
BoxCollection collection = 
	boxClient.getFoldersManager().getFolderItems(folderId, requestObj);
```

### Upload a New File

```java
BoxFileUploadRequestObject requestObj = 
	BoxFileUploadRequestObject.uploadFileRequestObject(parent, "name", file);
BoxFile bFile = boxClient.getFilesManager().uploadFile(requestObj);
```

### Upload a File with a Progress Listener

```java
BoxFileUploadRequestObject requestObj = 
	BoxFileUploadRequestObject.uploadFileRequestObject(parent, "name", file)
		.setListener(listener));
BoxFile bFile = boxClient.getFilesManager().uploadFile(requestObj);
```

### Download a File

```java
boxClient.getFilesManager().downloadFile(fileId, null);
```

### Delete a File

Delete a file, but only if the etag matches.

```java
BoxFileRequestObject requestObj =
	BoxFileRequestObject.deleteFileRequestObject().setIfMatch(etag);
boxClient.deleteFile(fileId, requestObj);
```

### Configure raw httpclient (e.g., set proxy)
You need to override the createRestClient() method in BoxClient so that it returns a configured rest client.
```java
BoxClient client = new BoxClient(clientId, clientSecret) {
    @Override
    public IBoxRESTClient createRestClient() {
        return new BoxRESTClient() {
            @Override
            public HttpClient getRawHttpClient() {
                HttpClient client = super.getRawHttpClient();
                // Now do the configure settings.
                HttpHost proxy = new HttpHost("{proxy ip/url}",{proxy port}, "{proxy scheme, e.g. http}";
                client.getParams().setParameter(ConnRouteNames.DEFAULT_PROXY, proxy);
                return client; 
            }
        }
    }
};

```


Known Issues
------------
The SDK does not currently provide an OAuth UI.

[hello-world]: https://github.com/box/box-java-sdk-private/wiki/HelloWorld
