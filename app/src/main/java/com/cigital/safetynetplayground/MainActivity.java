package com.cigital.safetynetplayground;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    protected int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected boolean showPopup()
    {
        final SpannableString str = new SpannableString(this.getText(R.string.actionPopupText));
        Linkify.addLinks(str, Linkify.WEB_URLS);

        AlertDialog popup = new AlertDialog.Builder(this)
                .setTitle(R.string.actionPopup)
                .setMessage(str)
                .setCancelable(true)
                .setNegativeButton("Web service source", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();

                        safelyOpenUrl(getString(R.string.urlCodeWeb));
                    }
                })
                .setPositiveButton("App source", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();

                        safelyOpenUrl(getString(R.string.urlCodeApp));
                    }
                })
                .create();

        popup.show();

        ((TextView) popup.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.actionPopup:
                return this.showPopup();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    protected boolean safelyOpenUrl(String url)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        PackageManager pm = this.getPackageManager();
        ComponentName cn = intent.resolveActivity(pm);
        if (cn != null) {
            startActivity(intent);
            return true;
        } else {
            String errorMsg = "Your device is not able to open URLs.";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.d("safetynet", errorMsg);
            return false;
        }
    }

    public void onClickVisitUs(View v)
    {
        safelyOpenUrl("https://www.cigital.com");
    }

    public void onBtnSubmitRequestClick(View v)
    {
        resetInterface();

        final int CODE_PROCEED = 1, CODE_ERROR = 2;
        WebServiceAbstraction webService =
                new WebServiceAbstraction("http://safetynet.cigital.com",
                                          new SafetyNetWrapper.SafetyNetCallback()
                                          {
                                              @Override
                                              public void handleCallback(int code, String[] messages)
                                              {
                                                  switch (code) {
                                                      case CODE_PROCEED:
                                                          updateProgress(messages);
                                                          break;
                                                      case CODE_ERROR:
                                                          displayError(messages);
                                                          break;
                                                  }
                                              }
                                          });
        try {
            webService.getDate(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void resetInterface()
    {
        this.currentStep = 0;
        resetProgressbar();
        resetIvResponse();
        setProgressComment("Press the button to begin...");
        setResponseText("");
    }

    protected void resetIvResponse()
    {
        ImageView imageView = (ImageView) findViewById(R.id.ivResponse);

        imageView.setImageResource(0);
    }

    protected void resetProgressbar()
    {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getProgressDrawable().setColorFilter(0xFF333333, PorterDuff.Mode.SRC_IN);
        setProgressValue(0);
    }

    protected void greenInterface()
    {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);

        ImageView imageView = (ImageView) findViewById(R.id.ivResponse);

        imageView.setImageResource(R.drawable.icon_ok);
    }

    protected void redInterface()
    {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        ImageView imageView = (ImageView) findViewById(R.id.ivResponse);

        imageView.setImageResource(R.drawable.icon_not_ok);
    }

    protected void displayError(String[] messages)
    {
        redInterface();
        setResponseText("Error: " + messages[0]);
        setProgressValue(100);
        setProgressComment(getProgressComment() + " failed.");
    }

    protected void updateProgress(String[] messages)
    {
        // track which step we are at
        switch (this.currentStep) {
            case 0:
                setProgressComment("Requesting nonce...");
                setProgressValue(20);
                break;
            case 1:
                setProgressComment("Requesting JWS...");
                setProgressValue(50);
                break;
            case 2:
                setProgressComment("Requesting API...");
                setProgressValue(80);
                break;
            case 3:
                setProgressComment("Request succeeded.");
                setProgressValue(100);
                greenInterface();
                setResponseText(messages[0]);
                break;
        }
        this.currentStep++;
    }

    public void setResponseText(String responseText)
    {
        TextView tvResponse = (TextView) findViewById(R.id.tvResponse);
        tvResponse.setText(responseText);
    }

    public String getProgressComment()
    {
        TextView tvProgressComment = (TextView) findViewById(R.id.tvProgressComment);

        return tvProgressComment.getText().toString();
    }

    public void setProgressComment(String progressComment)
    {
        TextView tvProgressComment = (TextView) findViewById(R.id.tvProgressComment);
        tvProgressComment.setText(progressComment);
    }

    public void setProgressValue(int value)
    {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(value);
    }
}
