package be.robinj.ubuntu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by robin on 7/31/14.
 */
public class QueueExt<T> implements Iterable<T>
{
	private short size;
	private List<T> items;

	public QueueExt (short size)
	{
		this.size = size;
		this.items = new ArrayList<T> ();
	}

	public void shift (T obj)
	{
		if (this.items.contains (obj))
		{
			this.items.remove (obj);
			this.trim ();
		}

		this.items.add (obj);

		while (this.items.size () > this.size)
			this.items.remove (this.size);
	}

	public short size ()
	{
		return this.size;
	}

	public short count ()
	{
		return (short) this.items.size ();
	}

	@Override
	public Iterator<T> iterator ()
	{
		return this.items.iterator ();
	}

	private void trim () // Remove indexes that have no value  //
	{
		List<T> trimmed = new ArrayList<T> ();

		for (T obj : this.items)
			trimmed.add (obj);

		this.items = trimmed;
	}
}
