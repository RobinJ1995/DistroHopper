package be.robinj.ubuntu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.webkit.JavascriptInterface;

public class ImageExt
{
	private Drawable drawable;
	protected Context context;

	public ImageExt (Context context)
	{
		this.context = context;
	}

	public ImageExt (Context context, Drawable image)
	{
		this.context = context;
		this.drawable = image;
	}

    @JavascriptInterface
	public Drawable get ()
	{
		return this.drawable;
	}

    @JavascriptInterface
	public String getBase64 ()
	{
		return this.getBase64 (100);
	}

    @JavascriptInterface
	public String getBase64 (int quality) //BOTTLENECK//
	{
		BitmapDrawable bitmapDrawable = (BitmapDrawable) this.get ();
		Bitmap bitmap = bitmapDrawable.getBitmap ();
		ByteArrayOutputStream stream = new ByteArrayOutputStream ();
		bitmap.compress (CompressFormat.PNG, quality, stream);
		
		byte[] bytes = stream.toByteArray ();
		String base64 = Base64.encodeToString (bytes, Base64.DEFAULT);
		
		return base64;
	}

    @JavascriptInterface
    public int[] getAverageColour ()
    {
        Drawable image = this.get ();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) image;
        Bitmap bitmap = bitmapDrawable.getBitmap ();

        int[] colours = new int[6];
        colours[0] = bitmap.getPixel (bitmap.getWidth () / 2, bitmap.getHeight () / 2);
        colours[1] = colours[0];
        colours[2] = bitmap.getPixel (bitmap.getWidth () / 3, bitmap.getHeight () / 3);
        colours[3] = bitmap.getPixel ((bitmap.getWidth () / 3) * 2, (bitmap.getHeight () / 3) * 2);
        colours[4] = bitmap.getPixel (bitmap.getWidth () / 3, (bitmap.getHeight () / 3) * 2);
        colours[5] = bitmap.getPixel ((bitmap.getWidth () / 3) * 2, bitmap.getHeight () / 3);

        int[] average = new int[3];
        for (int i = 0; i < colours.length; i++)
        {
            int[] rgb = ImageExt.colourToRgb (colours[i]);

            if (rgb[3] > 40)
            {
                average[0] += rgb[0];
                average[1] += rgb[1];
                average[2] += rgb[2];
            }
        }

        average[0] /= colours.length;
        average[1] /= colours.length;
        average[2] /= colours.length;

        return average;
    }

    @JavascriptInterface
    public String getAverageColourRgb ()
    {
        int[] avg = this.getAverageColour ();

        StringBuilder str = new StringBuilder ();
        str.append (avg[0]).append (",").append (avg[1]).append (",").append (avg[2]);

        return str.toString ();
    }

    @JavascriptInterface
	public String getPath ()
	{
		return this.getPath (Integer.toString (this.drawable.hashCode ()));
	}

    @JavascriptInterface
	public String getPath (String filename)
	{
		return this.getPath (filename, 100);
	}

    @JavascriptInterface
	public String getPath (String filename, int quality)
	{
		return this.getPath (filename, quality, CompressFormat.PNG);
	}

    @JavascriptInterface
	public String getPath (String filename, int quality, CompressFormat format)
	{
        try
        {
            Drawable drawable = this.get ();

            File file = new File (this.context.getCacheDir (), new StringBuilder (filename).append (".").append (format.name ()).toString ());
            String path = file.toString ();

            if (! file.exists ())
            {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap ();
                FileOutputStream outStream = null;
                outStream = new FileOutputStream(path);
                bitmap.compress (format, quality, outStream);
            }

            return path;
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }
	}
	
	static int[] colourToRgb (int colour)
	{
		return new int[] { Color.red (colour), Color.green (colour), Color.blue (colour), Color.alpha (colour) };
	}
}
