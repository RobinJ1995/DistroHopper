package be.robinj.ubuntu;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.analytics.tracking.android.EasyTracker;

public class MainActivity extends Activity
{
    private WebView webView;
    private GestureDetector gestureDetector;
    public static SharedPreferences prefs;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (savedInstanceState == null)
//            getFragmentManager ().beginTransaction ().add (R.id.container, new PlaceholderFragment ()).commit ();

        this.webView = (WebView) this.findViewById (R.id.web);
        WebSettings webSettings = this.webView.getSettings ();
        MainActivity.prefs = this.getPreferences (MODE_PRIVATE);
        JsInterface jsInterface = new JsInterface (this, this.webView, MainActivity.prefs);
        this.gestureDetector = new GestureDetector (this, new GestureListenerExt (this));

        webSettings.setJavaScriptEnabled (true);

        this.webView.setWebViewClient(new WebViewClient ());
        this.webView.setWebChromeClient(new WebChromeClient ());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) //DEBUG//
            WebView.setWebContentsDebuggingEnabled (true);
        this.webView.setOnLongClickListener (new LongPressListenerExt (this));
        this.webView.addJavascriptInterface (jsInterface, "android");
        this.webView.loadUrl ("file:///android_asset/main.htm");
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public void onStart ()
    {
        super.onStart ();

        EasyTracker.getInstance (this).activityStart (this);
    }

    @Override
    public void onStop ()
    {
        super.onStop ();

        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    public boolean dispatchTouchEvent (MotionEvent e)
    {
        super.dispatchTouchEvent (e);

        return this.gestureDetector.onTouchEvent(e);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        try
        {
            if (item.getItemId () == R.id.menuPreferences)
            {
                WebView web = (WebView) findViewById (R.id.web);
                web.loadUrl ("javascript:openPreferences ();");

                return true;
            }
            else if (item.getItemId () == R.id.menuSettings)
            {
                this.openAndroidSettings ();

                return true;
            }
            /*else if (item.getItemId () == R.id.menuDeveloper)
            {
                this.openInBrowser ("http://www.robinj.be/");

                return true;
            }*/
            else
            {
                return super.onOptionsItemSelected (item);
            }
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            this.runJs ("event_backButtonPressed ();");

            return true;
        }
        else
        {
            return super.onKeyDown (keyCode, event);
        }
    }

    public void openInBrowser (String url)
    {
        Intent browserIntent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
        startActivity (browserIntent);
    }

    public void runJs (String script)
    {
        this.webView.loadUrl ("javascript:" + script);
    }

    public void openAndroidSettings ()
    {
        Intent intent = new Intent ();
        intent.setClassName ("com.android.settings", "com.android.settings.Settings");
        startActivity (intent);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    /*public static class PlaceholderFragment extends Fragment
    {

        public PlaceholderFragment ()
        {
        }

        @Override
        public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View rootView = inflater.inflate (R.layout.fragment_main, container, false);

            return rootView;
        }
    }*/
}
