package be.robinj.distrohopper.desktop.dash.lens;

import android.view.View;
import android.widget.AdapterView;

/**
 * Created by robin on 8/21/14.
 */
public class LensSearchResultClickListener implements AdapterView.OnItemClickListener
{
	private Lens lens;

	public LensSearchResultClickListener (Lens lens)
	{
		this.lens = lens;
	}

	@Override
	public void onItemClick (AdapterView<?> parent, View view, int position, long id)
	{
		LensSearchResult result = (LensSearchResult) view.getTag ();
		String url = result.getUrl ();
		Object obj = result.getObj ();

		this.lens.onClick (url, obj);
	}
}
