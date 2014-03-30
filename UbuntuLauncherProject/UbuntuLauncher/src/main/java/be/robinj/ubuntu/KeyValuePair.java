package be.robinj.ubuntu;

/**
 * Created by Robin on 26/01/14.
 */
public class KeyValuePair
{
    private ArrayListExt<String> keys = new ArrayListExt<String> ();
    private ArrayListExt<String> values = new ArrayListExt<String> ();

    public String get (String key)
    {
        int index = this.keys.indexOf (key);
        return this.values.get (index);
    }

    public void set (String key, String value)
    {
        if (this.keys.contains (key))
        {
            this.values.set (this.keys.indexOf (key), value);
        }
        else
        {
            this.keys.add (key);
            this.values.add (value);
        }
    }

    public boolean exists (String key)
    {
        return (this.keys.contains (key));
    }
}
