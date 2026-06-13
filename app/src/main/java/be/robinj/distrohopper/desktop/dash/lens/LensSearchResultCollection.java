package be.robinj.distrohopper.desktop.dash.lens;

import java.util.List;

/**
 * Created by robin on 5/11/14.
 */
public class LensSearchResultCollection
{
	private Lens lens;
	private String name;
	private List<LensSearchResult> results;
	private Exception ex;

	public LensSearchResultCollection (Lens lens, List<LensSearchResult> results)
	{
		this.lens = lens;
		this.results = results;
	}

	/** A collection with its own section title instead of the lens's name. */
	public LensSearchResultCollection (Lens lens, String name, List<LensSearchResult> results)
	{
		this (lens, results);

		this.name = name;
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

	public String getName ()
	{
		return this.name != null ? this.name : this.lens.getName ();
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
