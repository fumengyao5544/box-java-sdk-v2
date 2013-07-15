package com.box.boxjavalibv2.authorization;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.interfaces.IAuthDataController;
import com.box.boxjavalibv2.requests.requestobjects.BoxOAuthRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;

/**
 * This is the data controller for OAuth, it handles token auto refresh.
 */
public class OAuthDataController implements IAuthDataController {

    public static enum OAuthTokenState {
        PRE_CREATION, AVAILABLE, REFRESHING, FAIL,
    }

    /**
     * Time to wait for lock.
     */
    private static final int WAIT = 100;
    /**
     * Default timeout waiting for lock.
     */
    private static final int WAIT_TIME_OUT = 60000;

    private final BoxClient mClient;
    private final String mClientId;
    private final String mClientSecret;
    private String mDeviceId = null;
    private String mDeviceName = null;
    private BoxOAuthToken mOAuthToken;
    private OAuthTokenState mTokenState = OAuthTokenState.PRE_CREATION;
    private boolean mAutoRefresh;
    private int mWaitTimeOut = WAIT_TIME_OUT;

    private volatile boolean locked = false;

    private OAuthRefreshListener refreshListener;

    /**
     * Constructor.
     * 
     * @param config
     *            BoxConfig
     * @param resourceHub
     *            resource hub
     * @par restClient rest client to make api calls.
     * @param clientId
     *            client id
     * @param clientSecret
     *            client secret
     * 
     */
    public OAuthDataController(BoxClient boxClient, final String clientId, final String clientSecret, final boolean autoRefresh) {
        this.mClient = boxClient;
        this.mClientId = clientId;
        this.mClientSecret = clientSecret;
        this.mAutoRefresh = autoRefresh;
    }

    /**
     * Makes OAuth auto refresh itself when token expires. Note if autorefresh fails, it's not going to try refresh again.
     * 
     * @param autoRefresh
     */
    public void setAutoRefreshOAuth(boolean autoRefresh) {
        mAutoRefresh = autoRefresh;
    }

    /**
     * Set the timeout for threads waiting for OAuth token refresh.
     * 
     * @param timeout
     */
    public void setWaitTimeOut(int timeout) {
        this.mWaitTimeOut = timeout;
    }

    /**
     * @return the mScheme
     */
    public String getScheme() {
        return mClient.getConfig().getOAuthUrlScheme();
    }

    /**
     * @return the mAuthority
     */
    public String getAuthority() {
        return mClient.getConfig().getOAuthUrlAuthority();
    }

    /**
     * @return the OAuth url path.
     */
    public String getUrlPath() {
        return mClient.getConfig().getOAuthWebUrlPath();
    }

    /**
     * @return the mClientId
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @return the mClientSecret
     */
    public String getClientSecret() {
        return mClientSecret;
    }

    public void setOAuthData(BoxOAuthToken token) {
        this.mOAuthToken = token;
    }

    /**
     * Set device id. This is optional.
     * 
     * @param deviceId
     *            device id
     */
    public void setDeviceId(final String deviceId) {
        this.mDeviceId = deviceId;
    }

    /**
     * Set device name. Optional.
     * 
     * @param deviceName
     *            device name
     */
    public void setDeviceName(final String deviceName) {
        this.mDeviceName = deviceName;
    }

    /**
     * @return the TokenState
     */
    public OAuthTokenState getTokenState() {
        return mTokenState;
    }

    /**
     * @param tokenState
     *            the mTokenState to set
     */
    public void setTokenState(OAuthTokenState tokenState) {
        this.mTokenState = tokenState;
    }

    /**
     * Initialize the controller.
     */
    public void initialize() {
        setTokenState(OAuthTokenState.AVAILABLE);
        unlock();
    }

    /**
     * get OAuthData.
     * 
     * @return OAuthData
     * @throws AuthFatalFailureException
     */
    @Override
    public BoxOAuthToken getAuthData() throws AuthFatalFailureException {
        int[] num = new int[] {0};
        return getAuthData(num);
    }

    /**
     * Refresh the OAuth.
     * 
     * @throws AuthFatalFailureException
     *             exception
     */
    @Override
    public void refresh() throws AuthFatalFailureException {
        if (!getAndSetLock(true)) {
            getAuthData();
        }
        else {
            if (getTokenState() == OAuthTokenState.FAIL || !mAutoRefresh) {
                setTokenState(OAuthTokenState.FAIL);
                throw new AuthFatalFailureException(true);
            }
            else {
                doRefresh();
            }
        }
    }

    /**
     * Get OAuthData, counting number of retries, in case of too many retries, throw.
     * 
     * @param numRetry
     *            array holding number of retries.
     * @return
     * @throws AuthFatalFailureException
     */
    private BoxOAuthToken getAuthData(int[] numRetry) throws AuthFatalFailureException {
        int num = numRetry[0];
        if (num * WAIT > mWaitTimeOut) {
            throw new AuthFatalFailureException();
        }

        if (getAndSetLock(false)) {
            return mOAuthToken;
        }
        else {
            if (getTokenState() == OAuthTokenState.FAIL) {
                throw new AuthFatalFailureException();
            }
            doWait();
            numRetry[0]++;
            return getAuthData(numRetry);
        }
    }

    /**
     * Get the lock, optionally lock the lock after getting the lock.
     * 
     * @param doLock
     *            whether want to lock after getting the lock.
     * @return
     */
    synchronized public boolean getAndSetLock(boolean doLock) {
        boolean lockRetrieved = false;
        if (doLock) {
            if (locked) {
                lockRetrieved = false;
            }
            else {
                locked = true;
                lockRetrieved = true;
            }
        }
        else {
            lockRetrieved = !locked;
        }
        return lockRetrieved;
    }

    /**
     * Unlock the OAuth lock.
     */
    private void unlock() {
        locked = false;
    }

    /**
     * Refresh the OAuth.
     * 
     * @throws BoxRestException
     *             exception
     * @throws BoxServerException
     *             exception
     * @throws AuthFatalFailureException
     *             exception
     */
    private void doRefresh() throws AuthFatalFailureException {
        setTokenState(OAuthTokenState.REFRESHING);
        try {
            mOAuthToken = mClient.getOAuthManager().refreshOAuth(
                (BoxOAuthRequestObject) BoxOAuthRequestObject.refreshOAuthRequestObject(mOAuthToken.getRefreshToken(), mClientId, mClientSecret)
                    .addQueryParam("device_id", mDeviceId).addQueryParam("device_name", mDeviceName));
            setTokenState(OAuthTokenState.AVAILABLE);
            if (refreshListener != null) {
                refreshListener.onRefresh(mOAuthToken);
            }

            unlock();
        }
        catch (Exception e) {
            setTokenState(OAuthTokenState.FAIL);
            throw new AuthFatalFailureException(true);
        }
    }

    /**
     * Convenient method for wait.
     */
    private void doWait() {
        try {
            Thread.sleep(WAIT);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addOAuthRefreshListener(OAuthRefreshListener listener) {
        this.refreshListener = listener;
    }

}
