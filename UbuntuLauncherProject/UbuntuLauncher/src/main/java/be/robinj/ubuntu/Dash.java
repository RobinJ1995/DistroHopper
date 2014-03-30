package be.robinj.ubuntu;

/**
 * Created by robin on 10/19/13.
 */
public class Dash
{
    private DashApps dashApps;

    public Dash (AppManager manager)
    {
        this.dashApps = new DashApps (manager);
    }

    public DashApps dashApps ()
    {
        return this.dashApps;
    }
}
