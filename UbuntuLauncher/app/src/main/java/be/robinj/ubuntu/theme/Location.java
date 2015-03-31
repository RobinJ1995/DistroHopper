package be.robinj.ubuntu.theme;

/**
 * Created by robin on 21/01/15.
 */
public enum Location
{
	NONE (-1), TOP (0), RIGHT (1), BOTTOM (2), LEFT (3);

	private int value;

	private Location (int value)
	{
		this.value = value;
	}
}
