package be.robinj.distrohopper.widgets;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import be.robinj.distrohopper.R;

/**
 * Created by robin on 3/12/14.
 */
public class WidgetsContainer extends RelativeLayout
{
	public WidgetsContainer (Context context, AttributeSet attrs)
	{
		super (context, attrs);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		inflater.inflate (R.layout.widgets_container, this, true);
	}
}
