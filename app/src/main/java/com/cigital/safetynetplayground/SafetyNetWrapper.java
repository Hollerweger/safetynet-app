package com.cigital.safetynetplayground;

import android.content.Context;
import android.util.Log;

import com.cigital.safetynetplayground.Exceptions.AttestationFailedException;
import com.cigital.safetynetplayground.Exceptions.GAPIClientFailedException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;

/**
 * Needs to know where to get NONCE.
 * <p/>
 * Needs to know where to verify JWS.
 * <p/>
 * Requests JWS from Google.
 */
public class SafetyNetWrapper
{

    protected GoogleApiClient prepareGoogleApiClient(Context ctx)
    {
        return new GoogleApiClient.Builder(ctx)
                .addApi(SafetyNet.API)
                .build();
    }

    public String getJws(Context ctx, byte[] nonce) throws
                                                    AttestationFailedException,
                                                    GAPIClientFailedException
    {
        GoogleApiClient gApiClient = this.prepareGoogleApiClient(ctx);
        ConnectionResult cr = gApiClient.blockingConnect();
        // That does not necessarily work on every version, most common error is outdated
        // version of google play services
        if (!cr.isSuccess()) {
            Log.d("snet", String.valueOf(cr.getErrorCode()));
            throw new GAPIClientFailedException();
        }

        SafetyNetApi.AttestationResult attestationResult =
                SafetyNet.SafetyNetApi.attest(gApiClient, nonce).await();
        Status status = attestationResult.getStatus();
        if (status.isSuccess()) {
            return attestationResult.getJwsResult();
        } else {
            throw new AttestationFailedException();
        }
    }

    /**
     * This will be used for updating UI.
     */
    public interface SafetyNetCallback
    {
        void handleCallback(int code, String[] messages);
    }
}
