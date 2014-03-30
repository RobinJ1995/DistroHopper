package be.robinj.ubuntu;

import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by robin on 28/10/13.
 */
public class DebugBenchmark
{
    private List<Long> checkpoints = new ArrayList<Long>();

    public DebugBenchmark ()
    {
        this.checkpoints.add (System.currentTimeMillis ());
    }

    @JavascriptInterface
    public void checkpoint ()
    {
        this.checkpoints.add (System.currentTimeMillis ());
    }

    @JavascriptInterface
    public List<Long> end ()
    {
        this.checkpoints.add (System.currentTimeMillis ());

        return this.checkpoints;
    }
}