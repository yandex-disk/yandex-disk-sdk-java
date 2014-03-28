/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.yandex.disk.client.exceptions.CancelledDownloadException;
import com.yandex.disk.client.exceptions.CancelledPropfindException;
import com.yandex.disk.client.exceptions.DownloadNoSpaceAvailableException;
import com.yandex.disk.client.exceptions.DuplicateFolderException;
import com.yandex.disk.client.exceptions.RemoteFileNotFoundException;
import com.yandex.disk.client.exceptions.FileModifiedException;
import com.yandex.disk.client.exceptions.FileNotModifiedException;
import com.yandex.disk.client.exceptions.FileTooBigServerException;
import com.yandex.disk.client.exceptions.FilesLimitExceededServerException;
import com.yandex.disk.client.exceptions.IntermediateFolderNotExistException;
import com.yandex.disk.client.exceptions.PreconditionFailedException;
import com.yandex.disk.client.exceptions.RangeNotSatisfiableException;
import com.yandex.disk.client.exceptions.ServerWebdavException;
import com.yandex.disk.client.exceptions.ServiceUnavailableWebdavException;
import com.yandex.disk.client.exceptions.UnknownServerWebdavException;
import com.yandex.disk.client.exceptions.UnsupportedMediaTypeException;
import com.yandex.disk.client.exceptions.WebdavClientInitException;
import com.yandex.disk.client.exceptions.WebdavException;
import com.yandex.disk.client.exceptions.WebdavFileNotFoundException;
import com.yandex.disk.client.exceptions.WebdavForbiddenException;
import com.yandex.disk.client.exceptions.WebdavInvalidUserException;
import com.yandex.disk.client.exceptions.WebdavNotAuthorizedException;
import com.yandex.disk.client.exceptions.WebdavUserNotInitialized;
import com.yandex.disk.client.exceptions.WebdavSharingForbiddenException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransportClient {

    private static final String TAG = "TransportClient";
    private static final String ATTR_ETAG_FROM_REDIRECT = "yandex.etag-from-redirect";

    protected static URL serverURL;

    static {
        try {
            serverURL = new URL("https://webdav.yandex.ru:443");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static final String userAgent = "Webdav Android Client Example/1.0";
    protected static final String LOCATION_HEADER = "Location";
    public static final String NO_REDIRECT_CONTEXT = "yandex.no-redirect";
    protected static final String WEBDAV_PROTO_DEPTH = "Depth";

    protected static final int NETWORK_TIMEOUT = 30000;
    protected static final int UPLOAD_NETWORK_TIMEOUT = NETWORK_TIMEOUT * 10;

    protected Context context;
    protected Credentials creds;
    protected final HttpClient httpClient;

    public static TransportClient getInstance(Context context, Credentials credentials)
            throws WebdavClientInitException {
        return new TransportClient(context, credentials, NETWORK_TIMEOUT);
    }

    public static TransportClient getUploadInstance(Context context, Credentials credentials)
            throws WebdavClientInitException {
        return new TransportClient(context, credentials, UPLOAD_NETWORK_TIMEOUT);
    }

    public TransportClient(Context context, Credentials credentials, DefaultHttpClient httpClient)
            throws WebdavClientInitException {
        this.context = context;
        this.creds = credentials;
        this.httpClient = httpClient;
        initHttpClient(httpClient);
    }

    protected TransportClient(Context context, Credentials credentials, int timeout)
            throws WebdavClientInitException {
        this(context, credentials, userAgent, timeout);
    }

    protected TransportClient(Context context, Credentials credentials, String userAgent, int timeout)
            throws WebdavClientInitException {
        this.context = context;
        this.creds = credentials;

        DefaultHttpClient httpClient = getNewHttpClient(userAgent, timeout);
        httpClient.setCookieStore(new BasicCookieStore());
        this.httpClient = httpClient;
        initHttpClient(httpClient);
    }

    public static void initHttpClient(DefaultHttpClient httpClient) {
        httpClient.setHttpRequestRetryHandler(requestRetryHandler);
        httpClient.setRedirectHandler(redirectHandler);
    }

    protected static DefaultHttpClient getNewHttpClient(String userAgent, int timeout)
            throws WebdavClientInitException {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        ConnManagerParams.setMaxTotalConnections(params, 1);

        SSLSocketFactory sf;
        try {
            sf = new SSLSocketFactoryWithTimeout(timeout);
        } catch (GeneralSecurityException ex) {
            Log.e(TAG, "getNewHttpClient", ex);
            throw new WebdavClientInitException();
        }
        sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", sf, 443));

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

        DefaultHttpClient res = new DefaultHttpClient(ccm, params);
        if (userAgent != null) {
            res.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
        }
        res.getParams().setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
        res.getParams().setParameter(CoreProtocolPNames.WAIT_FOR_CONTINUE, timeout);
        return res;
    }

    private static final HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
        @Override
        public boolean retryRequest(IOException ex, int count, HttpContext httpContext) {
            return false;
        }
    };

    private static final RedirectHandler redirectHandler = new DefaultRedirectHandler() {
        @Override
        public boolean isRedirectRequested(HttpResponse httpResponse, HttpContext httpContext) {
            Object noRedirect = httpContext.getAttribute(NO_REDIRECT_CONTEXT);
            if (noRedirect != null && (Boolean) noRedirect) {
                return false;
            }
            Header etagHeader = httpResponse.getFirstHeader("Etag");
            if (etagHeader != null) {
                httpContext.setAttribute(ATTR_ETAG_FROM_REDIRECT, etagHeader.getValue());
            }
            return super.isRedirectRequested(httpResponse, httpContext);
        }
    };

    protected HttpResponse executeRequest(HttpUriRequest request)
            throws IOException {
        return httpClient.execute(request, (HttpContext)null);
    }

    protected HttpResponse executeRequest(HttpUriRequest request, HttpContext httpContext)
            throws IOException {
        return httpClient.execute(request, httpContext);
    }

    public void shutdown() {
        httpClient.getConnectionManager().shutdown();
    }

    public static void shutdown(TransportClient client) {
        if (client != null) {
            client.shutdown();
        }
    }

    protected String getUrl() {
        return serverURL.toExternalForm();
    }

    protected static void consumeContent(HttpResponse response)
            throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity.consumeContent();
        }
    }

    public static String encodeURL(String url) {
        if (url == null) {
            return null;
        }
        String[] segments = url.split("/");
        StringBuilder sb = new StringBuilder(20);
        try {
            for (String segment : segments) {
                if (!"".equals(segment)) {
                    sb.append("/").append(URLEncoder.encode(segment, "UTF-8"));
                }
            }
            Log.d(TAG, "url encoded: "+sb.toString());
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Exception occured: "+e.getMessage());
        }
        return sb.toString().replace("+", "%20");
    }

    protected static void logMethod(HttpRequestBase method) {
        logMethod(method, null);
    }

    protected static void logMethod(HttpRequestBase method, String add) {
        Log.d(TAG, "logMethod(): "+method.getMethod()+": "+method.getURI()+(add != null ? " "+add : ""));
    }

    public enum HashType {
        MD5, SHA256
    }

    public static byte[] makeHashBytes(File file, HashType hashType)
            throws IOException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(hashType.name());
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            byte[] buf = new byte[8192];
            int count;
            while ((count = is.read(buf)) > 0) {
                digest.update(buf, 0, count);
            }
            return digest.digest();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static String makeHash(File file, HashType hashType)
            throws IOException {
        long time = System.currentTimeMillis();
        String hash = hash(makeHashBytes(file, hashType));
        Log.d(TAG, hashType.name()+": "+file.getAbsolutePath()+" hash="+hash+" time="+(System.currentTimeMillis()-time));
        return hash;
    }

    public static String hash(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            String n = Integer.toHexString(b & 0x000000FF);
            if (n.length() == 1) {
                out.append('0');
            }
            out.append(n);
        }
        return out.toString();
    }

    protected void checkStatusCodes(HttpResponse response, String details)
            throws WebdavNotAuthorizedException, WebdavUserNotInitialized, FileTooBigServerException,
            FilesLimitExceededServerException, ServerWebdavException, PreconditionFailedException, UnknownServerWebdavException {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        switch (statusCode) {
            case 401:
                Log.d(TAG, "Not authorized: "+statusLine.getReasonPhrase());
                throw new WebdavNotAuthorizedException(statusLine.getReasonPhrase() != null ? statusLine.getReasonPhrase() : "");
            case 403:
                Log.d(TAG, "User not initialized: "+statusLine.getReasonPhrase());
                throw new WebdavUserNotInitialized("Error (http code 403): "+details);
            case 412:
                Log.d(TAG, "Http code 412 (Precondition failed): "+details);
                throw new PreconditionFailedException("Error (http code 412): "+details);
            case 413:
                Log.d(TAG, "Http code 413 (File too big): "+details);
                throw new FileTooBigServerException();
            case 503:
                Log.d(TAG, "Http code 503 (Service Unavailable): "+details);
                throw new ServiceUnavailableWebdavException();
            case 507:
                Log.d(TAG, "Http code 507 (Insufficient Storage): "+details);
                throw new FilesLimitExceededServerException();
            default:
                if (statusCode >= 500 && statusCode < 600) {
                    Log.d(TAG, "Server error "+statusCode);
                    throw new ServerWebdavException("Server error while "+details);
                } else {
                    Log.d(TAG, "Unknown code "+statusCode);
                    throw new UnknownServerWebdavException("Server error while "+details);
                }
        }
    }

    private static final String PROPFIND_REQUEST =
            "<?xml version='1.0' encoding='utf-8' ?>" +
                    "<d:propfind xmlns:d='DAV:'>" +
                    "<d:prop xmlns:m='urn:yandex:disk:meta'>" +
                    "<d:resourcetype/>" +
                    "<d:displayname/>" +
                    "<d:getcontentlength/>" +
                    "<d:getlastmodified/>" +
                    "<d:getetag/>" +
                    "<d:getcontenttype/>" +
                    "<m:alias_enabled/>" +
                    "<m:visible/>" +
                    "<m:shared/>" +
                    "<m:readonly/>" +
                    "<m:public_url/>" +
                    "<m:etime/>" +
                    "<m:mediatype/>" +
                    "<m:mpfs_file_id/>" +
                    "<m:hasthumbnail/>" +
                    "</d:prop>" +
                    "</d:propfind>";

    private static final int MAX_ITEMS_PER_PAGE = Integer.MAX_VALUE;

    /**
     * Get folder listing
     *
     * @param path    Path to the folder
     * @param handler Parsing handler
     * @throws CancelledPropfindException Cancelled by user
     * @throws WebdavException            Server exceptions
     * @throws IOException                I/O exceptions
     * @see #getList(String, int, String, String, ListParsingHandler)
     */
    public void getList(String path, ListParsingHandler handler)
            throws IOException, PreconditionFailedException, UnknownServerWebdavException, WebdavFileNotFoundException,
            CancelledPropfindException, WebdavUserNotInitialized, ServerWebdavException, WebdavNotAuthorizedException,
            WebdavForbiddenException, WebdavInvalidUserException {
        getList(path, MAX_ITEMS_PER_PAGE, null, null, handler);
    }

    public void getList(String path, int itemsPerPage, ListParsingHandler handler)
            throws IOException, PreconditionFailedException, UnknownServerWebdavException, WebdavFileNotFoundException,
            CancelledPropfindException, WebdavUserNotInitialized, ServerWebdavException, WebdavNotAuthorizedException,
            WebdavForbiddenException, WebdavInvalidUserException {
        getList(path, itemsPerPage, null, null, handler);
    }

    /**
     * Get folder listing
     *
     * @param path         Path to the folder
     * @param itemsPerPage Size of one portion per request
     * @param handler      Parsing handler
     * @throws CancelledPropfindException Cancelled by user
     * @throws WebdavException            Server exceptions
     * @throws IOException                I/O exceptions
     */
    public void getList(String path, int itemsPerPage, String sortBy, String orderBy, ListParsingHandler handler)
            throws IOException, CancelledPropfindException, WebdavNotAuthorizedException, WebdavInvalidUserException,
            WebdavForbiddenException, WebdavFileNotFoundException, WebdavUserNotInitialized, UnknownServerWebdavException,
            PreconditionFailedException, ServerWebdavException {
        Log.d(TAG, "getList for "+path);

        boolean itemsFinished = false;
        int offset = 0;
        while (!itemsFinished) {

            if (handler.hasCancelled()) {
                throw new CancelledPropfindException();
            }

            String url = getUrl()+encodeURL(path);
            if (itemsPerPage != MAX_ITEMS_PER_PAGE) {
                url += "?offset="+offset+"&amount="+itemsPerPage;
                if (sortBy != null && orderBy != null) {
                    url += "&sort="+sortBy+"&order="+orderBy;
                }
            }

            PropFind propFind = new PropFind(url);
            logMethod(propFind);
            creds.addAuthHeader(propFind);
            propFind.setHeader(WEBDAV_PROTO_DEPTH, "1");
            HttpContext httpContext = handler.onCreateRequest(propFind, new StringEntity(PROPFIND_REQUEST));

            HttpResponse response = executeRequest(propFind, httpContext);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine != null) {
                int code = statusLine.getStatusCode();
                switch (code) {
                    case 207:
                        break;
                    case 401:
                        consumeContent(response);
                        throw new WebdavNotAuthorizedException(statusLine.getReasonPhrase() != null ? statusLine.getReasonPhrase() : "");
                    case 402:
                        consumeContent(response);
                        throw new WebdavInvalidUserException();
                    case 403:
                        consumeContent(response);
                        throw new WebdavForbiddenException();
                    case 404:
                        consumeContent(response);
                        throw new WebdavFileNotFoundException("Directory not found: "+path);
                    default:
                        consumeContent(response);
                        checkStatusCodes(response, "PROPFIND "+path);
                }
            }
            HttpEntity entity = response.getEntity();
            int countOnPage;
            try {
                ListParser parser = new ListParser(entity, handler);
                parser.parse();
                countOnPage = parser.getParsedCount();
                Log.d(TAG, "countOnPage="+countOnPage);
            } catch (XmlPullParserException ex) {
                throw new UnknownServerWebdavException(ex);
            } finally {
                consumeContent(response);
            }
            if (countOnPage != itemsPerPage) {
                itemsFinished = true;
            }
            offset += itemsPerPage;
        }
    }

    /**
     * Check unfinished upload
     *
     * @param file     File to upload
     * @param dir      Folder on the server
     * @param destName Name on the server
     * @param md5     MD5 hash
     * @return File size on the server. Possibly 0 if no unfinished uploadings
     * @throws IOException           I/O exceptions
     * @throws WebdavException       Webdav exceptions
     * @throws NumberFormatException Invalid size format from the server
     */
    public long headFile(File file, String dir, String destName, String md5, String sha256)
            throws IOException, NumberFormatException, WebdavUserNotInitialized, UnknownServerWebdavException, PreconditionFailedException,
            WebdavNotAuthorizedException, ServerWebdavException {
        String url = getUrl()+encodeURL(dir+"/"+destName);
        HttpHead head = new HttpHead(url);
        logMethod(head, ", file "+file);
        creds.addAuthHeader(head);
        head.addHeader("Etag", md5);
        if (sha256 != null) {
            head.addHeader("Sha256", sha256);
        }
        head.addHeader("Size", String.valueOf(file.length()));
        HttpResponse response = executeRequest(head);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            if (statusLine.getStatusCode() == 200) {
                Header[] headers = response.getHeaders("Content-Length");
                if (headers.length > 0) {
                    String contentLength = headers[0].getValue();
                    return Long.valueOf(contentLength);
                } else {
                    return 0;
                }
            } else if (statusCode == 409 || statusCode == 404 || statusCode == 412) {
                Log.d(TAG, statusLine+" for file "+file.getAbsolutePath()+" in dir "+dir);
                return 0;
            }
            checkStatusCodes(response, "HEAD "+url);
        }
        return 0;
    }

    /**
     * Upload file
     *
     * @param localPath        Local file path
     * @param serverDir        Server folder to upload
     * @param progressListener Listener to show progress to application
     * @throws WebdavException Server exeptions
     * @throws IOException     I/O exceptions
     */
    public void uploadFile(String localPath, String serverDir, ProgressListener progressListener)
            throws IOException, UnknownServerWebdavException, PreconditionFailedException,
            IntermediateFolderNotExistException, WebdavUserNotInitialized, ServerWebdavException, WebdavNotAuthorizedException {
        File file = new File(localPath);
        uploadFile(file, serverDir, file.getName(), makeHash(file, HashType.MD5), null, progressListener);
    }

    /**
     * Upload file
     *
     * @param file             Local file
     * @param dir              Server folder to upload
     * @param destFileName     File name on the server
     * @param md5             MD5 hash
     * @param progressListener Listener to show progress to application
     * @throws WebdavException Server exceptions
     * @throws IOException     I/O exceptions
     */
    public void uploadFile(File file, String dir, String destFileName, String md5, String sha256, final ProgressListener progressListener)
            throws IntermediateFolderNotExistException, IOException, WebdavUserNotInitialized, PreconditionFailedException,
            WebdavNotAuthorizedException, ServerWebdavException, UnknownServerWebdavException {

        String destName = TextUtils.isEmpty(destFileName) ? file.getName() : destFileName;
        String url = getUrl()+encodeURL(dir+"/"+destName);
        Log.d(TAG, "uploadFile: put to "+getUrl()+dir+"/"+destName);

        long uploadedSize;
        try {
            uploadedSize = headFile(file, dir, destName, md5, sha256);
        } catch (NumberFormatException ex) {
            Log.w(TAG, "Uploading "+file.getAbsolutePath()+" to "+dir+": HEAD failed", ex);
            uploadedSize = 0;
        }

        HttpPut put = new HttpPut(url);
        creds.addAuthHeader(put);
        put.addHeader("Etag", md5);

        if (sha256 != null) {
            Log.d(TAG, "Sha256: "+sha256);
            put.addHeader("Sha256", sha256);
        }

        if (uploadedSize > 0) {
            StringBuilder contentRange = new StringBuilder();
            contentRange.append("bytes ").append(uploadedSize).append("-").append(file.length()-1).append("/").append(file.length());
            Log.d(TAG, "Content-Range: "+contentRange);
            put.addHeader("Content-Range", contentRange.toString());
        }

        HttpEntity entity = new FileProgressHttpEntity(file, uploadedSize, progressListener);
        put.setEntity(entity);

        logMethod(put, ", file to upload "+file);
        HttpResponse response = executeRequest(put);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            consumeContent(response);
            switch (statusLine.getStatusCode()) {
                case 201:
                    Log.d(TAG, "File uploaded successfully: "+file);
                    return;
                case 409:
                    Log.d(TAG, "Parent not exist for dir "+dir);
                    throw new IntermediateFolderNotExistException("Parent folder not exists for '"+dir+"'");
                default:
                    checkStatusCodes(response, "PUT '"+file+"' to "+url);
            }
        }
    }

    public void downloadFile(String path, File saveTo, ProgressListener progressListener)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        downloadFile(path, saveTo, 0, 0, progressListener);
    }

    public void downloadFile(final String path, final File saveTo, final long length, final long fileSize, final ProgressListener progressListener)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        download(path, new DownloadListener() {
            @Override
            public long getLocalLength() {
                return length;
            }

            @Override
            public OutputStream getOutputStream(boolean append)
                    throws FileNotFoundException {
                return new FileOutputStream(saveTo, append);
            }

            @Override
            public void updateProgress(long loaded, long total) {
                progressListener.updateProgress(loaded, total);
            }

            @Override
            public boolean hasCancelled() {
                return progressListener.hasCancelled();
            }
        });
    }

    public byte[] download(final String path)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        download(path, new DownloadListener() {
            @Override
            public OutputStream getOutputStream(boolean append)
                    throws IOException {
                return outputStream;
            }
        });
        return outputStream.toByteArray();
    }

    public byte[] downloadUrl(final String url)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        downloadUrl(url, new DownloadListener() {
            @Override
            public OutputStream getOutputStream(boolean append)
                    throws IOException {
                return outputStream;
            }
        });
        return outputStream.toByteArray();
    }

    public void download(String path, DownloadListener downloadListener)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        downloadUrl(getUrl()+encodeURL(path), downloadListener);
    }

    public void downloadPreview(String path, DownloadListener downloadListener)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, DownloadNoSpaceAvailableException, RemoteFileNotFoundException, RangeNotSatisfiableException, FileModifiedException {
        downloadUrl(getUrl()+path, downloadListener);
    }

    private void downloadUrl(String url, DownloadListener downloadListener)
            throws IOException, WebdavUserNotInitialized, PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException,
            CancelledDownloadException, UnknownServerWebdavException, FileNotModifiedException, RemoteFileNotFoundException, DownloadNoSpaceAvailableException, RangeNotSatisfiableException, FileModifiedException {
        HttpGet get = new HttpGet(url);
        logMethod(get);
        creds.addAuthHeader(get);

        long length = downloadListener.getLocalLength();
        String ifTag = "If-None-Match";
        if (length >= 0) {
            ifTag = "If-Range";
            StringBuilder contentRange = new StringBuilder();
            contentRange.append("bytes=").append(length).append("-");
            Log.d(TAG, "Range: "+contentRange);
            get.addHeader("Range", contentRange.toString());
        }

        String etag = downloadListener.getETag();
        if (etag != null) {
            Log.d(TAG, ifTag+": "+etag);
            get.addHeader(ifTag, etag);
        }

        boolean partialContent = false;
        BasicHttpContext httpContext = new BasicHttpContext();
        HttpResponse httpResponse = executeRequest(get, httpContext);
        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            switch (statusCode) {
                case 200:
                    // OK
                    break;
                case 206:
                    partialContent = true;
                    break;
                case 304:
                    consumeContent(httpResponse);
                    throw new FileNotModifiedException();
                case 404:
                    consumeContent(httpResponse);
                    throw new RemoteFileNotFoundException("error while downloading file "+url);
                case 416:
                    consumeContent(httpResponse);
                    throw new RangeNotSatisfiableException("error while downloading file "+url);
                default:
                    checkStatusCodes(httpResponse, "GET '"+url+"'");
                    break;
            }
        }

        HttpEntity response = httpResponse.getEntity();
        long contentLength = response.getContentLength();
        Log.d(TAG, "download: contentLength="+contentLength);

        long loaded;
        if (partialContent) {
            ContentRangeResponse contentRangeResponse = parseContentRangeHeader(httpResponse.getLastHeader("Content-Range"));
            Log.d(TAG, "download: contentRangeResponse="+contentRangeResponse);
            if (contentRangeResponse != null) {
                loaded = contentRangeResponse.getStart();
                contentLength = contentRangeResponse.getSize();
            } else {
                loaded = length;
            }
        } else {
            loaded = 0;
            if (contentLength < 0) {
                contentLength = 0;
            }
        }

        String serverEtag = (String) httpContext.getAttribute(ATTR_ETAG_FROM_REDIRECT);
        if (!partialContent) {
            downloadListener.setEtag(serverEtag);
        } else {
            if (serverEtag != null && !serverEtag.equals(etag)) {
                response.consumeContent();
                throw new FileModifiedException("file changed, new etag is '" + serverEtag  +"'");
            } else {
                //Etag hasn't changed
            }
        }
        downloadListener.setStartPosition(loaded);
        downloadListener.setContentLength(contentLength);

        int count;
        InputStream content = response.getContent();
        OutputStream fos = downloadListener.getOutputStream(partialContent);
        try {
            final byte[] downloadBuffer = new byte[1024];
            while ((count = content.read(downloadBuffer)) != -1) {
                if (downloadListener.hasCancelled()) {
                    Log.i(TAG, "Downloading "+url+" canceled");
                    get.abort();
                    throw new CancelledDownloadException();
                }
                fos.write(downloadBuffer, 0, count);
                loaded += count;
                downloadListener.updateProgress(loaded, contentLength);
            }
        } catch (CancelledDownloadException ex) {
            throw ex;
        } catch (Exception e) {
            Log.w(TAG, e);
            get.abort();
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                // never happen
                throw new RuntimeException(e);
            }
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                // nothing
            }
            try {
                response.consumeContent();
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    private static Pattern CONTENT_RANGE_HEADER_PATTERN = Pattern.compile("bytes\\D+(\\d+)-\\d+/(\\d+)");

    private ContentRangeResponse parseContentRangeHeader(Header header) {
        if (header == null) {
            return null;
        }
        Log.d(TAG, header.getName()+": "+header.getValue());
        Matcher matcher = CONTENT_RANGE_HEADER_PATTERN.matcher(header.getValue());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ContentRangeResponse(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
        } catch (IllegalStateException ex) {
            Log.d(TAG, "parseContentRangeHeader: "+header, ex);
            return null;
        } catch (NumberFormatException ex) {
            Log.d(TAG, "parseContentRangeHeader: "+header, ex);
            return null;
        }
    }

    /**
     * http://api.yandex.ru/disk/doc/dg/reference/preview.xml
     */
    public enum PreviewSize {
        XXXS, XXS, XS, S, M, L, XL, XXL, XXXL
    }

    private static final String PREVIEW_ARG = "preview";
    private static final String SIZE_ARG = "size";

    private static void checkPath(String path)
            throws IllegalArgumentException {
        if (path == null || path.contains("?")) {
            throw new IllegalArgumentException();
        }
    }

    public static String makePreviewPath(String path, PreviewSize size)
            throws IllegalArgumentException {
        checkPath(path);
        if (size == null) {
            throw new IllegalArgumentException();
        }
        return path+"?"+PREVIEW_ARG+"&"+SIZE_ARG+"="+size.name();
    }

    public static String makePreviewPath(String path, int size)
            throws IllegalArgumentException {
        checkPath(path);
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        return path+"?"+PREVIEW_ARG+"&"+SIZE_ARG+"="+size;
    }

    public static String makePreviewPath(String path, int sizeX, int sizeY) {
        checkPath(path);
        if (sizeX <= 0 && sizeY <= 0) {
            throw new IllegalArgumentException();
        }
        StringBuilder size = new StringBuilder();
        if (sizeX > 0) {
            size.append(sizeX);
        }
        size.append("x");
        if (sizeY > 0) {
            size.append(sizeY);
        }
        return path+"?"+PREVIEW_ARG+"&"+SIZE_ARG+"="+size;
    }

    /**
     * Make folder
     *
     * @param dir Path to create
     * @throws WebdavException Server exceptions
     * @throws IOException     I/O exceptions
     * @see <a href="http://www.webdav.org/specs/rfc4918.html#METHOD_MKCOL">RFC 4918</a>
     */
    public void makeFolder(String dir)
            throws IOException, DuplicateFolderException, IntermediateFolderNotExistException, WebdavUserNotInitialized, PreconditionFailedException,
            WebdavNotAuthorizedException, ServerWebdavException, UnsupportedMediaTypeException, UnknownServerWebdavException {
        String url = getUrl()+encodeURL(dir);
        HttpMkcol mkcol = new HttpMkcol(url);
        logMethod(mkcol);
        creds.addAuthHeader(mkcol);
        HttpResponse response = executeRequest(mkcol);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            switch (statusCode) {
                case 201:
                    Log.d(TAG, "Folder created successfully");
                    return;
                case 405:
                    throw new DuplicateFolderException("Folder '"+dir+"' already exists");
                case 409:
                    throw new IntermediateFolderNotExistException("Parent folder not exists for '"+dir+"'");
                case 415:
                    throw new UnsupportedMediaTypeException("Folder '"+dir+"' creation error (http code 415)");
                default:
                    checkStatusCodes(response, "MKCOL '"+dir+"'");
            }
        }
    }

    /**
     * Delete
     *
     * @param path Path to delete
     * @throws WebdavException Server exceptions
     * @throws IOException     I/O exceptions
     */
    public void delete(String path)
            throws IOException, WebdavFileNotFoundException, WebdavUserNotInitialized, UnknownServerWebdavException,
            PreconditionFailedException, WebdavNotAuthorizedException, ServerWebdavException {
        String url = getUrl()+encodeURL(path);
        HttpDelete delete = new HttpDelete(url);
        logMethod(delete);
        creds.addAuthHeader(delete);
        HttpResponse response = executeRequest(delete);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            switch (statusLine.getStatusCode()) {
                case 200:
                    Log.d(TAG, "Delete successfully completed");
                    return;
                case 404:
                    throw new WebdavFileNotFoundException("'"+path+"' cannot be deleted");
                default:
                    checkStatusCodes(response, "DELETE '"+path+"'");
            }
        }
    }

    public void move(String src, String dest)
            throws WebdavException, IOException {
        Move move = new Move(getUrl()+encodeURL(src));
        move.setHeader("Destination", encodeURL(dest));
        move.setHeader("Overwrite", "F");
        logMethod(move, "to "+encodeURL(dest));
        creds.addAuthHeader(move);
        HttpResponse response = executeRequest(move);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            switch (statusCode) {
                case 201:
                    Log.d(TAG, "Rename successfully completed");
                    return;
                case 202:
                case 207:
                    Log.d(TAG, "HTTP code "+statusCode+": "+statusLine);
                    return;
                case 404:
                    throw new WebdavFileNotFoundException("'"+src+"' not found");
                case 409:
                    throw new DuplicateFolderException("File or folder "+dest+" already exist");
                default:
                    checkStatusCodes(response, "MOVE '"+src+"' to '"+dest+"'");
            }
        }
    }

    public String publish(String path)
            throws IOException, WebdavException {
//        String path = item.getFullName();
        HttpPost post = new HttpPost(getUrl()+encodeURL(path)+"?publish");
        logMethod(post, "(publish)");
        creds.addAuthHeader(post);

        HttpContext shareHttpContext = new BasicHttpContext();
        shareHttpContext.setAttribute(NO_REDIRECT_CONTEXT, true);
        HttpResponse httpResponse = executeRequest(post, shareHttpContext);
        consumeContent(httpResponse);

        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            switch (statusCode){
                case 302:
                    Header[] locationHeaders = httpResponse.getHeaders(LOCATION_HEADER);
                    if (locationHeaders.length == 1) {
                        String url = httpResponse.getHeaders(LOCATION_HEADER)[0].getValue();
                        Log.d(TAG, "publish: "+url);
                        return url;
                    }
                    checkStatusCodes(httpResponse, "publish");
                    break;
                case 403:
                    throw new WebdavSharingForbiddenException("Folder "+path+" can't be shared");
                default:
                    checkStatusCodes(httpResponse, "publish");
            }
        }
        return null;    // not happen
    }

    public void unpublish(String path)
            throws IOException, WebdavException {
        HttpPost post = new HttpPost(getUrl()+encodeURL(path)+"?unpublish");
        logMethod(post, "(unpublish)");
        creds.addAuthHeader(post);
        HttpResponse httpResponse = executeRequest(post);
        consumeContent(httpResponse);
        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null && statusLine.getStatusCode() == 200) {
            return;
        }
        checkStatusCodes(httpResponse, "unpublish");
    }

    public static class PropFind extends HttpPost {
        public PropFind() {
        }

        public PropFind(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "PROPFIND";
        }
    }

    private static class HttpMkcol extends HttpPut {
        public HttpMkcol(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "MKCOL";
        }
    }

    private static class Move extends HttpPut {
        public Move(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "MOVE";
        }
    }
}
