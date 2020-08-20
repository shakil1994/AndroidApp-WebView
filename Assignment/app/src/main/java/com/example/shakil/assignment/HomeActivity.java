package com.example.shakil.assignment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    WebView webView;
    //private String webUrl = "https://www.poribaar.com";
    private String webUrl = "https://poribaar.com/ecommerce/";

    //AlertDialog dialog;
    ProgressBar progressBarWeb;

    RelativeLayout relativeLayout;
    Button btnNoInternetConnection;
    SwipeRefreshLayout swipeRefreshLayout;

    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;

    // the same for Android 5.0 methods only
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        webView = findViewById(R.id.webview);
        relativeLayout = findViewById(R.id.relativeLayout);
        btnNoInternetConnection = findViewById(R.id.btnNoInternetConnection);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        progressBarWeb = findViewById(R.id.progressBar);

        swipeRefreshLayout.setColorSchemeColors(Color.BLUE,Color.YELLOW,Color.GREEN);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        Dexter.withActivity(this).withPermissions(new String[]{
                Manifest.permission.CALL_PHONE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }).withListener(new MultiplePermissionsListener() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (savedInstanceState != null) {
                    webView.restoreState(savedInstanceState);
                } else {
                    checkConnection();

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setLoadsImagesAutomatically(true);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setAllowFileAccessFromFileURLs(true);
                    webSettings.setAllowUniversalAccessFromFileURLs(true);
                    webSettings.setPluginState(WebSettings.PluginState.ON);
                    webSettings.setMediaPlaybackRequiresUserGesture(false);
                    webSettings.setAllowFileAccess(true);
                    webSettings.setDomStorageEnabled(true);
                    webSettings.setAppCacheEnabled(true);
                    webSettings.setLoadsImagesAutomatically(true);
                    webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    webSettings.setUseWideViewPort(true);
                    webSettings.setLoadWithOverviewMode(true);
                    webView.loadUrl(webUrl);
                }

                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        swipeRefreshLayout.setRefreshing(false);
                        super.onPageFinished(view, url);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        view.loadUrl(webUrl);
                        return true;
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        handler.proceed(); // Ignore SSL certificate errors
                    }
                });

                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {
                        /*dialog.show();
                        if (newProgress == 100){
                            dialog.dismiss();
                        }*/
                        progressBarWeb.setVisibility(View.VISIBLE);
                        progressBarWeb.setProgress(newProgress);
                        //setTitle("Loading...");
                        if (newProgress == 100) {
                            progressBarWeb.setVisibility(View.GONE);
                        }
                        super.onProgressChanged(view, newProgress);
                    }

                    // for Lollipop, all in one
                    public boolean onShowFileChooser(
                            WebView webView, ValueCallback<Uri[]> filePathCallback,
                            WebChromeClient.FileChooserParams fileChooserParams) {
                        if (mFilePathCallback != null) {
                            mFilePathCallback.onReceiveValue(null);
                        }
                        mFilePathCallback = filePathCallback;

                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                            // create the file where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                            } catch (IOException ex) {
                                // Error occurred while creating the File
                                Log.e(TAG, "Unable to create Image File", ex);
                            }

                            // continue only if the file was successfully created
                            if (photoFile != null) {
                                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                        Uri.fromFile(photoFile));
                            } else {
                                takePictureIntent = null;
                            }
                        }

                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType("image/*");

                        Intent[] intentArray;
                        if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Choose Image");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                        return true;
                    }

                    // creating image files (Lollipop only)
                    private File createImageFile() throws IOException {

                        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

                        if (!imageStorageDir.exists()) {
                            imageStorageDir.mkdirs();
                        }

                        // create an image file name
                        imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                        return imageStorageDir;
                    }

                    // openFileChooser for Android 3.0+
                    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                        mUploadMessage = uploadMsg;

                        try {
                            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

                            if (!imageStorageDir.exists()) {
                                imageStorageDir.mkdirs();
                            }

                            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                            mCapturedImageURI = Uri.fromFile(file); // save to the private variable

                            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                            // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                            i.addCategory(Intent.CATEGORY_OPENABLE);
                            i.setType("image/*");

                            Intent chooserIntent = Intent.createChooser(i, "Choose Image");
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                        } catch (Exception e) {
                            Toast.makeText(getBaseContext(), "Camera Exception:" + e, Toast.LENGTH_LONG).show();
                        }

                    }

                    // openFileChooser for Android < 3.0
                    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                        openFileChooser(uploadMsg, "");
                    }

                    // openFileChooser for other Android versions
                    /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
                       https://code.google.com/p/android/issues/detail?id=62220
                       however newer versions of KitKat fixed it on some devices */
                    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                        openFileChooser(uploadMsg, acceptType);
                    }
                });

                btnNoInternetConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        checkConnection();
                    }
                });
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();

        btnNoInternetConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkConnection();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }

        } // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // if there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } // end of code for Lollipop only
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Exit Dialog")
                    .setIcon(R.drawable.icon)
                    .setCancelable(false)
                    .setMessage("Are you sure you want to Exit?")
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setNeutralButton(R.string.minimize, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                            startMain.addCategory(Intent.CATEGORY_HOME);
                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(startMain);
                        }
                    })
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    public void checkConnection() {

        ConnectivityManager connectivityManager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isConnected()) {
            webView.loadUrl(webUrl);
            webView.setVisibility(View.VISIBLE);
            relativeLayout.setVisibility(View.GONE);
        } else if (mobileNetwork.isConnected()) {
            webView.loadUrl(webUrl);
            webView.setVisibility(View.VISIBLE);
            relativeLayout.setVisibility(View.GONE);
        } else {
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
        }
    }
}
