package com.cigital.safetynetplayground;

import android.content.Context;

import java.io.IOException;

public class WebServiceAbstraction
{
    protected String uri;
    protected SafetyNetWrapper safetyNetWrapper;
    protected SafetyNetWrapper.SafetyNetCallback safetyNetCallback;

    public WebServiceAbstraction(String uri, SafetyNetWrapper.SafetyNetCallback safetyNetCallback)
    {
        this.uri = uri;
        this.safetyNetCallback = safetyNetCallback;
        this.safetyNetWrapper = new SafetyNetWrapper();
    }

    public void getDate(Context ctx) throws
                                     IOException
    {
        WebServiceRequestTask requestDateTask = new WebServiceRequestTask();
        requestDateTask.setCallback(safetyNetCallback);
        requestDateTask.setWrapper(safetyNetWrapper);
        requestDateTask.setUri(this.uri);
        requestDateTask.execute(ctx);
    }
}
