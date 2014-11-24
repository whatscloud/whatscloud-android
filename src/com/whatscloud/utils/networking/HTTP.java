package com.whatscloud.utils.networking;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.util.List;

public class HTTP
{
    static boolean mInitialized;

    public static HttpContext mLocalContext;
    public static BasicCookieStore mCookieContainer;

    public static void initializeContext()
    {
        //---------------------------------
        // Prevent re-initialization
        //---------------------------------

        mInitialized = true;

        //---------------------------------
        // Create singleton http context
        //---------------------------------

        mLocalContext = new BasicHttpContext();

        //---------------------------------
        // Set up context with cookies
        //---------------------------------

        mLocalContext.setAttribute(ClientContext.COOKIE_STORE, mCookieContainer);
    }

    public static String get(String url)
    {
        //---------------------------------
        // Initialize cookie store
        //---------------------------------

        if ( !mInitialized )
        {
            initializeContext();
        }

        //---------------------------------
        // Get custom http client
        //---------------------------------

        HttpClient client = getHTTPClient();

        //---------------------------------
        // Create Get request
        //---------------------------------

        HttpGet getRequest = new HttpGet( url );

        //---------------------------------
        // Add custom user agent
        //---------------------------------

        getRequest.addHeader("User-Agent", com.whatscloud.config.networking.HTTP.USER_AGENT);

        //---------------------------------
        // Temporary variable for response
        //---------------------------------

        String response = "";

        try
        {
            //---------------------------------
            // Execute the request
            //---------------------------------

            HttpResponse getResponse = client.execute(getRequest, mLocalContext);

            //-------------------------------------
            // Convert the Bytes read to a string
            //-------------------------------------

            response = EntityUtils.toString(getResponse.getEntity());
        }
        catch (Exception exc)
        {
            //---------------------------------
            // Return empty string
            //---------------------------------

            return "";
        }

        //---------------------------------
        // Dispose of connection manager
        //---------------------------------

        client.getConnectionManager().shutdown();

        //---------------------------------
        // Return string response
        //---------------------------------

        return response;
    }

    public static String post(String url, List<NameValuePair> postData)
    {
        //----------------------------------------
        // Create a new default HttpClient
        //----------------------------------------

        HttpClient client = getHTTPClient();

        //-------------------------------------
        // Create a new post request
        //-------------------------------------

        HttpPost postRequest = new HttpPost( url );

        //------------------------------
        // Use our personal user-agent
        //------------------------------

        postRequest.addHeader("User-Agent", com.whatscloud.config.networking.HTTP.USER_AGENT);

        //------------------------------
        // Response variable
        //------------------------------

        String response = "";

        try
        {
            //------------------------------
            // Set data encoding as UTF-8
            //------------------------------

            postRequest.setEntity(new UrlEncodedFormEntity(postData, "UTF-8"));

            //----------------------------
            // Execute HTTP Post Request
            //----------------------------

            HttpResponse postResponse = client.execute(postRequest, mLocalContext);

            //-------------------------------------
            // Convert the Bytes read to a string
            //-------------------------------------

            response = EntityUtils.toString(postResponse.getEntity());
        }
        catch (Exception exc)
        {
            //---------------------------------
            // Return nothing
            //---------------------------------

            return "";
        }

        //-------------------------------------
        // Dispose the client connection
        //-------------------------------------

        client.getConnectionManager().shutdown();

        //-------------------------------------
        // Return response string
        //-------------------------------------

        return response;
    }

    public static DefaultHttpClient getHTTPClient()
    {
        try
        {
            //---------------------------------
            // Create HTTP params object
            //---------------------------------

            HttpParams parameters = new BasicHttpParams();

            //---------------------------------
            // Set to HTTP 1.1
            //---------------------------------

            HttpProtocolParams.setVersion(parameters, HttpVersion.HTTP_1_1);

            //---------------------------------
            // Create scheme registry
            //---------------------------------

            SchemeRegistry registry = new SchemeRegistry();

            //---------------------------------
            // Allow http/https requests
            //---------------------------------

            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

            //---------------------------------
            // Create thread-safe manager
            //---------------------------------

            ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(parameters, registry);

            //---------------------------------
            // Return customized manager
            //---------------------------------

            return new DefaultHttpClient(connectionManager, parameters);
        }
        catch (Exception exc)
        {
            //---------------------------------
            // Failed? Return default client
            //---------------------------------

            return new DefaultHttpClient();
        }
    }
}