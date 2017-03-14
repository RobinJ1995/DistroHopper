package be.robinj.distrohopper.unity.dash.lens;

import java.util.List;

/**
 * Created by robin on 5/11/14.
 */
public class LensSearchResultCollection
{
	private Lens lens;
	private List<LensSearchResult> results;
	private Exception ex;

	public LensSearchResultCollection (Lens lens, List<LensSearchResult> results)
	{
		this.lens = lens;
		this.results = results;
	}

	public LensSearchResultCollection (Lens lens, Exception ex)
	{
		this.lens = lens;
		this.ex = ex;
	}

	public Lens getLens ()
	{
		return this.lens;
	}

	public List<LensSearchResult> getResults ()
	{
		return this.results;
	}

	public Exception getException ()
	{
		return this.ex;
	}
}
