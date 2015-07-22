package be.robinj.ubuntu;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.dev.Debug;

/**
 * Created by robin on 8/22/14.
 */
public class Image
{
	private Drawable drawable;

	public Image (Drawable drawable)
	{
		this.drawable = drawable;
	}

	public Image (Bitmap bitmap)
	{
		BitmapDrawable bmd = new BitmapDrawable (bitmap);
		this.drawable = bmd;
	}

	public Drawable getDrawable ()
	{
		return drawable;
	}

	public void setDrawable (Drawable drawable)
	{
		this.drawable = drawable;
	}

	public int getAverageColour (boolean advanced, boolean useHsv, int alpha)
	{
		Debug.assertCondition (this.drawable != null);

		BitmapDrawable bmd = (BitmapDrawable) this.getDrawable ();
		Bitmap bm = bmd.getBitmap ();

		int width = bm.getWidth ();
		int height = bm.getHeight ();

		int[] colours;
		if (advanced)
		{
			List<Integer> lColours = new ArrayList<Integer> ();

			int[] posMiddle = new int[]
			{
				width / 2,
				height / 2
			};

			for (float i = 0.05F; i < 1.0F; i *= 1.25F) // 0% = middle // 100% = outer edge //
			{
				for (int j = 0; j < 4; j++) // 0 = up // 1 = right // 2 = down // 3 = left //
				{
					float x = (float) posMiddle[0];
					float y = (float) posMiddle[1];

					switch (j)
					{
						case 0: // up //
							y -= y * i;
							break;
						case 1: // up right //
							y -= (y * i) * 0.75F;
							x += (x * i) * 0.75F;
							break;
						case 2: // right //
							x += x * i;
							break;
						case 3: // down right //
							y += (y * i) * 0.75F;
							x += (x * i) * 0.75F;
							break;
						case 4: // down //
							y += y * i;
							break;
						case 5: // down left //
							y += (y * i) * 0.75F;
							x -= (x * i) * 0.75F;
							break;
						case 6: // left //
							x -= x * i;
							break;
						case 7: // up left //
							y -= (y * i) * 0.75F;
							x -= (x * i) * 0.75F;
							break;
					}

					int colour = bm.getPixel ((int) x, (int) y);
					lColours.add (colour);
				}
			}

			colours = new int[lColours.size ()];
			for (int i = 0; i < colours.length; i++)
				colours[i] = lColours.get (i);
		}
		else
		{
			colours = new int[5];
			colours[0] = bm.getPixel (width / 2, height / 2); // | - | //
			//colours[1] = colours[0]; // - // Middle counts twice //
			colours[1] = bm.getPixel (width / 3, height / 3); // |'  | //
			colours[2] = bm.getPixel ((width / 3) * 2, (height / 3) * 2); // |  .| //
			colours[3] = bm.getPixel (width / 3, (height / 3) * 2); // |.  | //
			colours[4] = bm.getPixel ((width / 3) * 2, height / 3); // |  '| //
		}

		int result;

		if (useHsv)
		{
			float[] perHue = new float[360];

			int samplesPassed = 0;
			int samplesDropped = 0;
			int samplesDroppedSaturation = 0;
			int samplesDroppedValue = 0;
			int samplesDroppedAlpha = 0;

			for (int i = 0; i < colours.length; i++)
			{
				float[] hsv = new float[3];
				Color.colorToHSV (colours[i], hsv); // 0 = Hue [0..360] // 1 = Saturation [0..1] // 2 = Value [0..1] //
				int cAlpha = Color.alpha (colours[i]);

				float hue = hsv[0];
				float saturation = hsv[1];
				float value = hsv[2];

				if (saturation < 0.2F || (saturation < 0.4F && value < 0.4F))
				{
					samplesDroppedSaturation++;
				}
				else if (value < 0.3F) // || (value < 0.4F && saturation < 0.4F)) // But that's already in the previous statement //
				{
					samplesDroppedValue++;
				}
				else if (cAlpha <= 40)
				{
					samplesDroppedAlpha++;
				}
				else
				{
					perHue[(int) Math.floor ((double) hue)]++;
					samplesPassed++;
				}
			}

			samplesDropped += (samplesDroppedSaturation + samplesDroppedValue + samplesDroppedAlpha);

			if (samplesPassed == 0)
			{
				if (samplesDroppedSaturation >= (samplesDropped - 1)) // Black-and-white //
				{
					int white = 0;
					int grey = 0;
					int black = 0;

					for (int i = 0; i < colours.length; i++)
					{
						float[] hsv = new float[3];
						Color.colorToHSV (colours[i], hsv);
						int cAlpha = Color.alpha (colours[i]);

						float value = hsv[2];

						if (cAlpha > 40)
						{
							if (value > 0.8F)
								white++;
							else if (value < 0.2F)
								black++;
							else
								grey++;
						}
					}

					if ((grey > white && grey > black) || white == black)
						return Color.argb (alpha, 150, 150, 150);
					else if (white >= grey && white >= black)
						return Color.argb (alpha, 240, 240, 240);
					else
						return Color.argb (alpha, 50, 50, 50);
				}
			}

			int hueGroupSize = 5;
			int start = 0;

			int[] hueGroups = new int[360 / hueGroupSize];
			for (int i = 0; i < hueGroups.length; i++)
			{
				int start2 = start;

				for (int j = 0; j < hueGroupSize; j++)
				{
					if (perHue[start2 + j] > 0)
						hueGroups[i]++;

					start++;
				}
			}

			int mostUses = -1;
			int mostUsedHue = -1;
			for (int i = 0; i < hueGroups.length; i++)
			{
				if (hueGroups[i] > mostUses)
				{
					mostUses = hueGroups[i];
					mostUsedHue = (i * hueGroupSize) - (hueGroupSize / 2);
				}
			}

			result = Color.HSVToColor (alpha, new float[] { mostUsedHue, 0.9F, 1.0F });
		}
		else
		{
			int[] average = new int[3];
			int total = 0; // Dropping transparent pixels, so the amount of colours I added up isn't necessarily the same as colours.length //

			for (int i = 0; i < colours.length; i++)
			{
				int[] rgb = new int[]
				{
					Color.red (colours[i]),
					Color.green (colours[i]),
					Color.blue (colours[i]),
					Color.alpha (colours[i])
				};

				if (rgb[3] > 40) // Don't care about pixels which are more than ~85% transparent //
				{
					total++;

					average[0] += rgb[0];
					average[1] += rgb[1];
					average[2] += rgb[2];
				}
			}

			if (total <= 0)
				return Color.argb (alpha, 40, 40, 40);

			average[0] /= total;
			average[1] /= total;
			average[2] /= total;

			result = Color.argb (alpha, average[0], average[1], average[2]);
		}

		return result;
	}
}
