package be.robinj.distrohopper;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.android.vending.billing.IInAppBillingService;

/**
 * Created by robin on 19/03/17.
 */

public class IAPServiceConnection implements ServiceConnection
{
	private IInAppBillingService service;
	
	@Override
	public void onServiceDisconnected (ComponentName name)
	{
		this.service = null;
	}
	
	@Override
	public void onServiceConnected (ComponentName name, IBinder serv)
	{
		this.service = IInAppBillingService.Stub.asInterface (serv);
	}
}
