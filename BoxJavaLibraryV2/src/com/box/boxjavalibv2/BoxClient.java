package com.box.boxjavalibv2;

import org.apache.commons.lang.NotImplementedException;

import com.box.boxjavalibv2.authorization.OAuthAuthorization;
import com.box.boxjavalibv2.authorization.OAuthDataController;
import com.box.boxjavalibv2.authorization.OAuthDataController.OAuthTokenState;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.authorization.SharedLinkAuthorization;
import com.box.boxjavalibv2.dao.BoxBase;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.BoxResourceType;
import com.box.boxjavalibv2.events.OAuthEvent;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.interfaces.IAuthData;
import com.box.boxjavalibv2.interfaces.IAuthDataController;
import com.box.boxjavalibv2.interfaces.IAuthEvent;
import com.box.boxjavalibv2.interfaces.IAuthFlowListener;
import com.box.boxjavalibv2.interfaces.IAuthFlowMessage;
import com.box.boxjavalibv2.interfaces.IAuthFlowUI;
import com.box.boxjavalibv2.interfaces.IAuthSecureStorage;
import com.box.boxjavalibv2.interfaces.IBoxResourceHub;
import com.box.boxjavalibv2.jacksonparser.BoxResourceHub;
import com.box.boxjavalibv2.resourcemanagers.BoxCollaborationsManager;
import com.box.boxjavalibv2.resourcemanagers.BoxCommentsManager;
import com.box.boxjavalibv2.resourcemanagers.BoxEventsManager;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.box.boxjavalibv2.resourcemanagers.BoxFoldersManager;
import com.box.boxjavalibv2.resourcemanagers.BoxOAuthManager;
import com.box.boxjavalibv2.resourcemanagers.BoxResourceManager;
import com.box.boxjavalibv2.resourcemanagers.BoxSearchManager;
import com.box.boxjavalibv2.resourcemanagers.BoxSharedItemsManager;
import com.box.boxjavalibv2.resourcemanagers.BoxUsersManager;
import com.box.restclientv2.interfaces.IBoxConfig;
import com.box.restclientv2.interfaces.IBoxRESTClient;
import com.box.restclientv2.interfaces.IBoxRequestAuth;

/**
 * This is the main entrance of the sdk. The client contains all resource managers and also handles authentication. Make sure you call authenticate method
 * before making any api calls. you can use the resource managers to execute requests <b>synchronously</b> against the Box REST API(V2). Full details about the
 * Box API can be found at {@see <a href="http://developers.box.com/docs">http://developers.box.com/docs</a>} . You must have an OpenBox application with a
 * valid API key to use the Box API. All methods in this class are executed in the invoking thread, and therefore are NOT safe to execute in the UI thread of
 * your application. You should only use this class if you already have worker threads or AsyncTasks that you want to incorporate the Box API into.
 */
public class BoxClient extends BoxBase implements IAuthFlowListener {

    private final static boolean DEFAULT_AUTO_REFRESH = true;

    private final IAuthDataController authController;
    private final IBoxRequestAuth auth;

    private final IBoxResourceHub resourceHub;
    private final IBoxRESTClient restClient;

    private final BoxFilesManager filesManager;
    private final BoxFoldersManager foldersManager;
    private final BoxSearchManager searchManager;
    private final BoxEventsManager eventsManager;
    private final BoxCollaborationsManager collaborationsManager;
    private final BoxCommentsManager commentsManager;
    private final BoxUsersManager usersManager;
    private final BoxOAuthManager oauthManager;
    private IAuthFlowListener mAuthListener;

    public BoxClient(final String clientId, final String clientSecret) {
        resourceHub = createResourceHub();
        restClient = createRestClient();
        authController = createAuthDataController(clientId, clientSecret);
        auth = createAuthorization(authController);

        filesManager = new BoxFilesManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        foldersManager = new BoxFoldersManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        searchManager = new BoxSearchManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        eventsManager = new BoxEventsManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        collaborationsManager = new BoxCollaborationsManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        commentsManager = new BoxCommentsManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        usersManager = new BoxUsersManager(getConfig(), getResourceHub(), getAuth(), getRestClient());
        oauthManager = new BoxOAuthManager(getConfig(), getResourceHub(), getRestClient());
    }

    /**
     * Whether this client is authenticated.
     * 
     * @return
     */
    public boolean isAuthenticated() {
        try {
            return getAuthData() != null;
        }
        catch (AuthFatalFailureException e) {
            return false;
        }
    }

    /**
     * Makes OAuth auto refresh itself when token expires. By default, this is set to true. Note if autorefresh fails, it's not going to try refresh again.
     * 
     * @param autoRefresh
     */
    public void setAutoRefreshOAuth(boolean autoRefresh) {
        getOAuthDataController().setAutoRefreshOAuth(autoRefresh);
    }

    /**
     * Set connection time out.
     * 
     * @param timeOut
     */
    public void setConnectionTimeOut(final int timeOut) {
        ((BoxRESTClient) getRestClient()).setConnectionTimeOut(timeOut);
    }

    /**
     * Get the OAuthDataController that controls OAuth data.
     */
    public OAuthDataController getOAuthDataController() {
        return (OAuthDataController) authController;
    }

    public void addOAuthRefreshListener(OAuthRefreshListener listener) {
        getOAuthDataController().addOAuthRefreshListener(listener);
    }

    /**
     * Get the OAuth data.
     * 
     * @return
     * @throws AuthFatalFailureException
     */
    public BoxOAuthToken getAuthData() throws AuthFatalFailureException {
        return getOAuthDataController().getAuthData();
    }

    /**
     * Save auth in a customized secure storage.
     * 
     * @param storage
     * @throws AuthFatalFailureException
     */
    public void saveAuth(IAuthSecureStorage storage) throws AuthFatalFailureException {
        storage.saveAuth(getAuthData());
    }

    /**
     * Authenticate from the auth object stored in the secure storage.
     * 
     * @param storage
     */
    public void authenticateFromSecureStorage(IAuthSecureStorage storage) {
        authenticate(storage.getAuth());
    }

    /**
     * Get the BoxFilesManager, which can be used to make API calls on files endpoints. Note this files manager only work on the folders you own. if you are
     * trying to make api calls on a shared file (file shared to you via shared link), please use getSharedFilesManager().
     * 
     * @return the filesManager
     */
    public BoxFilesManager getFilesManager() {
        return filesManager;
    }

    /**
     * Get the OAuthManager, which can be used to make OAuth related api calls.
     * 
     * @return
     */
    public BoxOAuthManager getOAuthManager() {
        return oauthManager;
    }

    /**
     * Get Shared Items manager, which can be used to make API calls on shared item endpoints.
     * 
     * @param sharedLink
     *            shared link
     * @param password
     *            password
     * @return BoxSharedItemsManager
     */
    public BoxSharedItemsManager getSharedItemsManager(String sharedLink, String password) {
        return new BoxSharedItemsManager(getConfig(), getResourceHub(), getSharedItemAuth(sharedLink, password), getRestClient());
    }

    /**
     * Get the BoxFilesManager for shared items, which can be used to make API calls on files endpoints for a shared item.
     * 
     * @param sharedLink
     *            shared link.
     * @param password
     *            password of the shared link, use null if there is no password
     * @return BoxFilesManager
     */
    public BoxFilesManager getSharedFilesManager(String sharedLink, String password) {
        return new BoxFilesManager(getConfig(), getResourceHub(), getSharedItemAuth(sharedLink, password), getRestClient());
    }

    /**
     * Get the BoxFoldersManager for shared items, which can be used to make API calls on folders endpoints for a shared item.
     * 
     * @param sharedLink
     *            shared link.
     * @param password
     *            password of the shared link, use null if there is no password
     * @return BoxFoldersManager
     */
    public BoxFoldersManager getSharedFoldersManager(String sharedLink, String password) {
        return new BoxFoldersManager(getConfig(), getResourceHub(), getSharedItemAuth(sharedLink, password), getRestClient());
    }

    /**
     * Get the BoxCommentsManager for shared items, which can be used to make API calls on comments endpoints for a shared item.
     * 
     * @param sharedLink
     *            shared link.
     * @param password
     *            password of the shared link, use null if there is no password
     * @return BoxFoldersManager
     */
    public BoxCommentsManager getSharedCommentsManager(String sharedLink, String password) {
        return new BoxCommentsManager(getConfig(), getResourceHub(), getSharedItemAuth(sharedLink, password), getRestClient());
    }

    /**
     * A generic way to get a resourceManager with shared link auth. Currently only supports file, folder and comment endpoints.
     * 
     * @param type
     * @param sharedLink
     * @param password
     * @return
     */
    public BoxResourceManager getResourceManagerWithSharedLinkAuth(BoxResourceType type, String sharedLink, String password) {
        switch (type) {
            case FILE:
                return getSharedFilesManager(sharedLink, password);
            case FOLDER:
                return getSharedFoldersManager(sharedLink, password);
            case COMMENT:
                return getSharedCommentsManager(sharedLink, password);
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * @return the BoxFoldersManager, which can be used to make API calls on folders endpoints. Note this folders manager only work on the folders you own. if
     *         you are trying to make api calls on a shared folder (folder shared to you via shared link), please use getSharedFoldersManager().
     */
    public BoxFoldersManager getFoldersManager() {
        return foldersManager;
    }

    /**
     * @return BoxSearchManager through which searches can be performed.
     */
    public BoxSearchManager getSearchManager() {
        return searchManager;
    }

    /**
     * 
     * @return BoxEventsManager through which the Box Events API can be queried.
     */
    public BoxEventsManager getEventsManager() {
        return eventsManager;
    }

    /**
     * @return the collaborationsManager
     */
    public BoxCollaborationsManager getCollaborationsManager() {
        return collaborationsManager;
    }

    /**
     * @return the commentsManager
     */
    public BoxCommentsManager getCommentsManager() {
        return commentsManager;
    }

    /**
     * @return the usersManager
     */
    public BoxUsersManager getUsersManager() {
        return usersManager;
    }

    /**
     * Get authenticated using a Auth object, this could be a previously stored data.
     * 
     * @param token
     */
    public void authenticate(IAuthData authData) {
        getOAuthDataController().setOAuthData((BoxOAuthToken) authData);
    }

    /**
     * Get authenticated. Note authentication is done asynchronously and may not finish right after this method. The authentication result should be notified to
     * the IAuthFlowListener parameter.
     * 
     * @param authFlowUI
     *            UI for the auth(OAuth) flow.
     * @param autoRefreshOAuth
     *            whether the OAuth token should be auto refreshed when it expires. Note only set this to true when you are making api calls in a single thread
     *            fashion, multi-thread auto-refreshing at a same time could cause problem.
     * @param listener
     *            listener listening to the auth flow events.
     */
    public void authenticate(IAuthFlowUI authFlowUI, boolean autoRefreshOAuth, IAuthFlowListener listener) {
        this.mAuthListener = listener;
        authFlowUI.authenticate(this);
    }

    @Override
    public void onAuthFlowEvent(IAuthEvent event, IAuthFlowMessage message) {
        OAuthEvent oe = (OAuthEvent) event;
        if (oe == OAuthEvent.OAUTH_CREATED) {
            ((OAuthAuthorization) getAuth()).setOAuthData(getOAuthTokenFromMessage(message));
        }
        if (mAuthListener != null) {
            mAuthListener.onAuthFlowEvent(event, message);
        }
    }

    /**
     * Check authentication state.
     * 
     * @return authentication state
     */
    public OAuthTokenState getAuthState() {
        return getOAuthDataController().getTokenState();
    }

    /**
     * Get config.
     * 
     * @return config
     */
    public IBoxConfig getConfig() {
        return BoxConfig.getInstance();
    }

    /**
     * Create a resource hub, which directs the Jackson JSON processor to parse api responses into different objects.
     * 
     * @return IBoxResourceHub
     */
    protected IBoxResourceHub createResourceHub() {
        return new BoxResourceHub();
    }

    /**
     * Get resource hub.
     * 
     * @return Resource hub
     */
    public IBoxResourceHub getResourceHub() {
        return this.resourceHub;
    }

    /**
     * Get rest client.
     * 
     * @return
     */
    protected IBoxRESTClient getRestClient() {
        return this.restClient;
    }

    /**
     * Create a REST client to make api calls.
     * 
     * @return IBoxRESTClient
     */
    protected IBoxRESTClient createRestClient() {
        return new BoxRESTClient();
    }

    /**
     * Get the authorization needed for shared items.
     * 
     * @param sharedLink
     *            shared link
     * @param password
     *            password(use null if no password at all)
     * @return IBoxRequestAuth
     */
    public IBoxRequestAuth getSharedItemAuth(String sharedLink, String password) {
        return new SharedLinkAuthorization((OAuthAuthorization) getAuth(), sharedLink, password);
    }

    public IAuthDataController createAuthDataController(final String clientId, final String clientSecret) {
        return new OAuthDataController(this, clientId, clientSecret, DEFAULT_AUTO_REFRESH);
    }

    public IBoxRequestAuth createAuthorization(IAuthDataController controller) {
        return new OAuthAuthorization((OAuthDataController) authController);
    }

    /**
     * Get the auth object used to make api calls.
     * 
     * @return
     */
    public IBoxRequestAuth getAuth() {
        return auth;
    }

    @Override
    public void onAuthFlowMessage(IAuthFlowMessage message) {
        if (mAuthListener != null) {
            mAuthListener.onAuthFlowMessage(message);
        }
    }

    @Override
    public void onAuthFlowException(Exception e) {
        if (mAuthListener != null) {
            mAuthListener.onAuthFlowException(e);
        }
    }

    protected BoxOAuthToken getOAuthTokenFromMessage(IAuthFlowMessage message) {
        return (BoxOAuthToken) message.getData();
    }
}
