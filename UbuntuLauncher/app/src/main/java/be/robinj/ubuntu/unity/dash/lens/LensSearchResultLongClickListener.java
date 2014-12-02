package be.robinj.ubuntu.unity.dash.lens;

import android.view.View;
import android.widget.AdapterView;

/**
 * Created by robin on 8/21/14.
 */
public class LensSearchResultLongClickListener implements AdapterView.OnItemLongClickListener
{
	private Lens lens;

	public LensSearchResultLongClickListener (Lens lens)
	{
		this.lens = lens;
	}

	@Override
	public boolean onItemLongClick (AdapterView<?> parent, View view, int position, long id)
	{
		LensSearchResult result = (LensSearchResult) view.getTag ();
		String url = result.getUrl ();
		Object obj = result.getObj ();

		this.lens.onLongClick (url, obj);

		return true;
	}
}
