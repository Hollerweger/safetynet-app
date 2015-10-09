package com.cigital.safetynetplayground;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import com.cigital.safetynetplayground.Exceptions.AttestationFailedException;
import com.cigital.safetynetplayground.Exceptions.GAPIClientFailedException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class WebServiceRequestTask extends AsyncTask<Context, Object, Void>
{
    protected SafetyNetWrapper.SafetyNetCallback callback;
    protected SafetyNetWrapper wrapper;
    protected String uri;

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public void setWrapper(SafetyNetWrapper wrapper)
    {
        this.wrapper = wrapper;
    }

    public void setCallback(SafetyNetWrapper.SafetyNetCallback callback)
    {
        this.callback = callback;
    }

    protected void noWebService(Exception e)
    {
        publishError(e, "could not open a connection to the web service.");
    }

    protected void runtimeError(Exception e)
    {
        publishError(e, "runtime error. Try again.");
    }

    protected void publishMessage(String msg)
    {
        publishProgress(1, new String[]{msg});
    }

    protected void publishError(Exception e, String errorMsg)
    {
        e.printStackTrace();
        publishProgress(2, new String[]{errorMsg});
    }

    @Override
    protected Void doInBackground(Context... args)
    {
        byte[] nonce;
        String responseData;

        // Enable cookies to work with php session
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        // Advance nonce status message
        publishProgress(1, null);

        // Retrieve nonce from web service
        @SuppressWarnings("UnusedAssignment") URL queryUrl = null;
        try {
            queryUrl = new URL(this.uri + "/api/getnonce");
        } catch (MalformedURLException e) {
            this.runtimeError(e);
            return null;
        }
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) queryUrl.openConnection();
        } catch (IOException e) {
            this.noWebService(e);
            return null;
        }
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            this.runtimeError(e);
            return null;
        }
        connection.setRequestProperty("Content-length", "0");
        try {
            connection.connect();
        } catch (IOException e) {
            this.noWebService(e);
            return null;
        }

        // In production this would require more error handling, but for PoC we don't care
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
            responseData = reader.readLine();
        } catch (IOException e) {
            publishError(e, "failed to retrieve nonce.");
            return null;
        }
        nonce = Base64.decode(responseData, Base64.DEFAULT);
        publishMessage(responseData);

        // Advance JWS status message
        String jws;
        try {
            jws = wrapper.getJws(args[0], nonce);
            publishMessage(null);
        } catch (GAPIClientFailedException e) {
            publishError(e, "Google Play Services not available on the device or is out of date.");
            return null;
        } catch (AttestationFailedException e) {
            publishError(e, "failed connection to Google Play Services.");
            return null;
        }

        // Set up the request
        try {
            // The actual API call we are attempting to make
            queryUrl = new URL(this.uri + "/api/getgift");
        } catch (MalformedURLException e) {
            this.runtimeError(e);
            return null;
        }
        try {
            connection = (HttpURLConnection) queryUrl.openConnection();
        } catch (IOException e) {
            this.noWebService(e);
            return null;
        }
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            this.runtimeError(e);
            return null;
        }
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);

        // Build POST parameters
        Uri.Builder postBuilder = new Uri.Builder()
                .appendQueryParameter("jws", jws);
        String queryString = postBuilder.build().getEncodedQuery();

        // Write parameters to request
        @SuppressWarnings("UnusedAssignment") OutputStream os = null;
        try {
            os = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(queryString);
            writer.flush();
            writer.close();
            os.close();
        } catch (IOException e) {
            this.runtimeError(e);
            return null;
        }

        // Execute the request
        try {
            connection.connect();
        } catch (IOException e) {
            this.noWebService(e);
            return null;
        }

        try {
            if (connection.getResponseCode() == 500) {
                // Our implementation will return 500 if verification fails
                publishError(new Exception(), "API call verification failed.\r\nThis means that this device did not pass SafetyNet checks or you forgot to update digests on your server.");
                return null;
            } else if (connection.getResponseCode() != 200) {
                // Anything but 500 and 200 is not something our web service should return
                publishError(new Exception(), "unknown error with the API.");
                return null;
            }
        } catch (IOException e) {
            this.runtimeError(e);
            return null;
        }

        // Process the response, again, we don't care about error handling in PoC
        @SuppressWarnings("UnusedAssignment") BufferedReader reader =
                null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            responseData = reader.readLine();
            publishMessage(responseData + "\r\nThis means that your device has successfully passed SafetyNet checks.");
        } catch (IOException e) {
            e.printStackTrace();
            this.runtimeError(e);
        }
        return null;
    }

    protected void onProgressUpdate(Object... args)
    {
        int code = (int) args[0];
        String[] messages = (String[]) args[1];
        callback.handleCallback(code, messages);
    }
}
