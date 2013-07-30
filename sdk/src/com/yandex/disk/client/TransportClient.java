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
import com.yandex.disk.client.exceptions.DuplicateFolderException;
import com.yandex.disk.client.exceptions.FileTooBigServerException;
import com.yandex.disk.client.exceptions.FilesLimitExceededServerException;
import com.yandex.disk.client.exceptions.IntermediateFolderNotExistException;
import com.yandex.disk.client.exceptions.ServerWebdavException;
import com.yandex.disk.client.exceptions.WebdavClientInitException;
import com.yandex.disk.client.exceptions.WebdavException;
import com.yandex.disk.client.exceptions.WebdavFileNotFoundException;
import com.yandex.disk.client.exceptions.WebdavNotAuthorizedException;
import com.yandex.disk.client.exceptions.WebdavUserNotInitialized;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TransportClient {

    private static final String TAG = "TransportClient";

    protected static URL serverURL;

    static {
        try {
            serverURL = new URL("https://webdav.yandex.ru:443");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String userAgent = "Webdav Android Client Example/1.0";
    protected static final String LOCATION_HEADER = "Location";
    protected static final String NO_REDIRECT_CONTEXT = "yandex.no-redirect";
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

    protected TransportClient(Context context, Credentials credentials, int timeout)
            throws WebdavClientInitException {
        this.context = context;
        this.creds = credentials;

        DefaultHttpClient httpClient = getNewHttpClient(userAgent, timeout);
        httpClient.setCookieStore(new BasicCookieStore());
        this.httpClient = httpClient;
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
            Log.e(TAG, "", ex);
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
        res.setHttpRequestRetryHandler(requestRetryHandler);
        res.setRedirectHandler(redirectHandler);
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

            return super.isRedirectRequested(httpResponse, httpContext);
        }
    };

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

    private static void logMethod(HttpRequestBase method) {
        logMethod(method, null);
    }

    private static void logMethod(HttpRequestBase method, String add) {
        Log.d(TAG, "logMethod(): "+method.getMethod()+": "+method.getURI()+(add != null ? " "+add : ""));
    }

    protected enum HashType {
        MD5, SHA256
    }

    protected static String makeHash(String string, HashType type) {
        try {
            MessageDigest digest = MessageDigest.getInstance(type.name());
            byte[] hash = digest.digest(string.getBytes());
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected void checkStatusCodes(HttpResponse response, String details)
            throws WebdavException, IOException {
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
                throw new WebdavException("Error (http code 412): "+details);
            case 413:
                Log.d(TAG, "Http code 413 (File too big): "+details);
                throw new FileTooBigServerException();
            case 507:
                Log.d(TAG, "Http code 507 (Insufficient Storage): "+details);
                throw new FilesLimitExceededServerException();
            default:
                if (statusCode >= 500 && statusCode < 600) {
                    Log.d(TAG, "Server error "+statusCode);
                    throw new ServerWebdavException("Server error while "+details);
                }
        }
    }

    protected void checkStatusCodesFinal(HttpResponse response, String details)
            throws WebdavException, IOException {
        checkStatusCodes(response, details);
        throw new WebdavException("Unknown error while "+details);
    }

    private static final String PROPFIND_REQUEST =
            "<?xml version='1.0' encoding='utf-8' ?>"+
                    "<d:propfind xmlns:d='DAV:'>"+
                    "<d:prop xmlns:m='urn:yandex:disk:meta'>"+
                    "<d:resourcetype/>"+
                    "<d:displayname/>"+
                    "<d:getcontentlength/>"+
                    "<d:getlastmodified/>"+
                    "<d:getetag/>"+
                    "<d:getcontenttype/>"+
                    "<m:alias_enabled/>"+
                    "<m:visible/>"+
                    "<m:shared/>"+
                    "<m:readonly/>"+
                    "<m:public_url/>"+
                    "</d:prop>"+
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
     * @see #getList(String, int, ListParsingHandler)
     */
    public void getList(String path, ListParsingHandler handler)
            throws WebdavException, IOException {
        getList(path, MAX_ITEMS_PER_PAGE, handler);
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
    public void getList(String path, int itemsPerPage, ListParsingHandler handler)
            throws WebdavException, IOException {
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
            }

            PropFind propFind = new PropFind(url);
            logMethod(propFind);
            creds.addAuthHeader(propFind);
            propFind.setHeader(WEBDAV_PROTO_DEPTH, "1");
            propFind.setEntity(new StringEntity(PROPFIND_REQUEST));

            HttpResponse response = httpClient.execute(propFind);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine != null) {
                int code = statusLine.getStatusCode();
                switch (code) {
                    case 207:
                        break;
                    case 404:
                        consumeContent(response);
                        throw new WebdavFileNotFoundException("Directory not found: "+path);
                    default:
                        consumeContent(response);
                        checkStatusCodesFinal(response, "PROPFIND "+path);
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
                throw new WebdavException(ex);
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
     * @param hash     MD5 hash
     * @return File size on the server. Possibly 0 if no unfinished uploadings
     * @throws IOException           I/O exceptions
     * @throws WebdavException       Webdav exceptions
     * @throws NumberFormatException Invalid size format from the server
     */
    public long headFile(File file, String dir, String destName, String hash)
            throws IOException, WebdavException, NumberFormatException {
        String url = getUrl()+encodeURL(dir+"/"+destName);
        HttpHead head = new HttpHead(url);
        logMethod(head, ", file "+file);
        creds.addAuthHeader(head);
        head.addHeader("Etag", hash);
        head.addHeader("Size", String.valueOf(file.length()));
        HttpResponse response = httpClient.execute(head);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            if (statusLine.getStatusCode() == 200) {
//                Log.d(TAG, "200 "+statusLine.getReasonPhrase()+" for file "+file.getAbsolutePath()+" in dir "+dir);
                Header[] headers = response.getHeaders("Content-Length");
                if (headers.length > 0) {
                    String contentLength = headers[0].getValue();
//                    Log.d(TAG, "Content-Length: "+contentLength);
                    return Long.valueOf(contentLength);
                } else {
                    return 0;
                }
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
            throws WebdavException, IOException {
        File file = new File(localPath);
        uploadFile(file, serverDir, file.getName(), makeHash(localPath, HashType.MD5), progressListener);
    }

    /**
     * Upload file
     *
     * @param file             Local file
     * @param dir              Server folder to upload
     * @param destFileName     File name on the server
     * @param hash             MD5 hash
     * @param progressListener Listener to show progress to application
     * @throws WebdavException Server exceptions
     * @throws IOException     I/O exceptions
     */
    public void uploadFile(File file, String dir, String destFileName, String hash, final ProgressListener progressListener)
            throws WebdavException, IOException {

        String destName = TextUtils.isEmpty(destFileName) ? file.getName() : destFileName;
        String url = getUrl()+encodeURL(dir+"/"+destName);
        Log.d(TAG, "uploadFile: put to "+getUrl()+dir+"/"+destName);

        HttpPut put = new HttpPut(url);
        creds.addAuthHeader(put);
        put.addHeader("Etag", hash);

        long uploadedSize;
        try {
            uploadedSize = headFile(file, dir, destFileName, hash);
        } catch (Throwable ex) {
            Log.w(TAG, "Uploading "+file.getAbsolutePath()+" to "+dir+" failed", ex);
            uploadedSize = 0;
        }
        if (uploadedSize > 0) {
            StringBuffer contentRange = new StringBuffer();
            contentRange.append("bytes ").append(uploadedSize).append("-").append(file.length()-1).append("/").append(file.length());
            Log.d(TAG, "Content-Range: "+contentRange);
            put.addHeader("Content-Range", contentRange.toString());
        }
        HttpEntity entity = new FileProgressHttpEntity(file, uploadedSize, progressListener);
        put.setEntity(entity);

        logMethod(put, ", file to upload "+file);
        HttpResponse response = httpClient.execute(put);
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
            throws WebdavException, IOException {
        String url = getUrl()+encodeURL(path);
        HttpGet get = new HttpGet(url);
        logMethod(get, " to "+saveTo);
        creds.addAuthHeader(get);
        HttpResponse httpResponse = httpClient.execute(get);
        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null) {
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 404) {
                consumeContent(httpResponse);
                throw new WebdavException("File not found "+url);
            } else if (statusCode >= 500 && statusCode < 600) {
                consumeContent(httpResponse);
                throw new ServerWebdavException("Error while downloading file "+url);
            }
        }

        HttpEntity response = httpResponse.getEntity();
        long contentLength = response.getContentLength();
        if (contentLength < 0) {
            // Transfer-Encoding: chunked
            contentLength = 0;
        }

        int count;
        long loaded = 0;
        InputStream content = response.getContent();
        FileOutputStream fos = new FileOutputStream(saveTo);
        try {
            final byte[] downloadBuffer = new byte[1024];
            while ((count = content.read(downloadBuffer)) != -1) {
                if (progressListener.hasCancelled()) {
                    Log.i(TAG, "Downloading "+path+" canceled");
                    saveTo.delete();
                    get.abort();
                    throw new CancelledDownloadException();
                }
                fos.write(downloadBuffer, 0, count);
                loaded += count;
                progressListener.updateProgress(loaded, contentLength);
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
                //it must be never happen
                throw new RuntimeException(e);
            }
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                // supress IOException on cancel
            }
            try {
                response.consumeContent();
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
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
            throws WebdavException, IOException {
        String url = getUrl()+encodeURL(dir);
        HttpMkcol mkcol = new HttpMkcol(url);
        logMethod(mkcol);
        creds.addAuthHeader(mkcol);
        HttpResponse response = httpClient.execute(mkcol);
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
                    throw new WebdavException("Folder '"+dir+"' creation error (http code 415)");
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
            throws WebdavException, IOException {
        String url = getUrl()+encodeURL(path);
        HttpDelete delete = new HttpDelete(url);
        logMethod(delete);
        creds.addAuthHeader(delete);
        HttpResponse response = httpClient.execute(delete);
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
                    checkStatusCodesFinal(response, "DELETE '"+path+"'");
            }
        }
    }

    public void move(String src, String dest)
            throws WebdavException, IOException {
        Move move = new Move(getUrl()+encodeURL(src));
        move.addHeader("Destination", encodeURL(dest));
        logMethod(move, "to "+encodeURL(dest));
        creds.addAuthHeader(move);
        HttpResponse response = httpClient.execute(move);
        consumeContent(response);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            switch (statusLine.getStatusCode()) {
                case 201:
                    Log.d(TAG, "Rename successfully completed");
                    return;
                case 404:
                    throw new WebdavFileNotFoundException("'"+src+"' not found");
                case 409:
                    throw new DuplicateFolderException("Folder "+dest+" already exist");
                default:
                    checkStatusCodesFinal(response, "MOVE '"+src+"' to '"+dest+"'");
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
        HttpResponse httpResponse = httpClient.execute(post, shareHttpContext);
        consumeContent(httpResponse);

        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null) {
            int code = statusLine.getStatusCode();
            if (code == 302) {
                Header[] locationHeaders = httpResponse.getHeaders(LOCATION_HEADER);
                if (locationHeaders.length == 1) {
                    String url = httpResponse.getHeaders(LOCATION_HEADER)[0].getValue();
                    Log.d(TAG, "publish: "+url);
                    return url;
                }
            }
        }
        checkStatusCodesFinal(httpResponse, "publish");
        return null;    // not happen
    }

    public void unpublish(String path)
            throws IOException, WebdavException {
        HttpPost post = new HttpPost(getUrl()+encodeURL(path)+"?unpublish");
        logMethod(post, "(unpublish)");
        creds.addAuthHeader(post);
        HttpResponse httpResponse = httpClient.execute(post);
        consumeContent(httpResponse);
        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine != null && statusLine.getStatusCode() == 200) {
            return;
        }
        checkStatusCodesFinal(httpResponse, "unpublish");
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
