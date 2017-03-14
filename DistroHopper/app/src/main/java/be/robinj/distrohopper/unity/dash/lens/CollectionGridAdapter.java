package be.robinj.distrohopper.unity.dash.lens;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/21/14.
 */
public class CollectionGridAdapter extends ArrayAdapter<LensSearchResultCollection>
{
	public CollectionGridAdapter (Context context, List<LensSearchResultCollection> coll)
	{
		super (context, R.layout.widget_dash_lens_result_collection, coll);
	}

	@Override
	public View getView (int position, View view, ViewGroup parent)
	{
		LensSearchResultCollection coll = this.getItem (position);
		boolean show = true;

		if (view == null)
			view = LayoutInflater.from (this.getContext ()).inflate (R.layout.widget_dash_lens_result_collection, parent, false);

		TextView tvLabel = (TextView) view.findViewById (R.id.tvLabel);
		GridView gvResults = (GridView) view.findViewById (R.id.gvResults);

		tvLabel.setText (coll.getLens ().getName ());
		tvLabel.setTextColor (view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_colour));
		tvLabel.setShadowLayer (5, 2, 2, view.getResources ().getColor (HomeActivity.theme.dash_applauncher_text_shadow_colour));

		List<LensSearchResult> results = coll.getResults ();
		if (results == null)
		{
			results = new ArrayList<LensSearchResult> ();
			Exception ex = coll.getException ();

			if (ex != null)
			{
				if (ex instanceof UnknownHostException || ex instanceof SocketException)
				{
					ConnectivityManager connectivityManager = (ConnectivityManager) this.getContext ().getSystemService (Context.CONNECTIVITY_SERVICE);
					NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo ();

					if (! (networkInfo != null && networkInfo.isConnected ()))
						show = false;
				}

				LensSearchResult error = new LensSearchResult (this.getContext (), ex.getClass ().getSimpleName (), "error://" + ex.getMessage (), this.getContext ().getResources ().getDrawable (R.drawable.dash_search_lens_error));

				results.add (error);
			}
		}

		if (show)
		{
			gvResults.setAdapter (new GridAdapter (this.getContext (), results));
			gvResults.setOnItemClickListener (new LensSearchResultClickListener (coll.getLens ()));
			gvResults.setOnItemLongClickListener (new LensSearchResultLongClickListener (coll.getLens ()));

			view.setVisibility (View.VISIBLE);
		}
		else
		{
			view.setVisibility (View.GONE);
		}

		view.setTag (coll);

		return view;
	}
}
