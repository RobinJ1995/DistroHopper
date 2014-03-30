package be.robinj.ubuntu;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

public class SystemSettings
{
	private Context context;
	private WifiManager wifi;
	private BluetoothAdapter bluetooth;
	private AudioManager sound;
	
	public SystemSettings (Context context)
	{
		this.context = context;
		
		this.wifi = (WifiManager) this.context.getSystemService (Context.WIFI_SERVICE);
		this.bluetooth = BluetoothAdapter.getDefaultAdapter ();
		this.sound = (AudioManager) this.context.getSystemService (Context.AUDIO_SERVICE);
	}

    @JavascriptInterface
	public boolean getWifi ()
	{
		return this.wifi.isWifiEnabled ();
	}

    @JavascriptInterface
	public boolean setWifi (boolean state)
	{
		return this.wifi.setWifiEnabled (state);
	}

    @JavascriptInterface
	public boolean getBluetooth ()
	{
		if (this.bluetooth == null)
			return false;
		
		return this.bluetooth.isEnabled ();
	}

    @JavascriptInterface
	public boolean setBluetooth (boolean state)
	{
		if (this.bluetooth == null)
			return false;
		
		if (state)
			return this.bluetooth.enable ();
		else
			return this.bluetooth.disable ();
	}

    @JavascriptInterface
	public boolean getSound ()
	{
		return (this.sound.getRingerMode () == AudioManager.RINGER_MODE_NORMAL);
	}

    @JavascriptInterface
	public boolean setSound (boolean state)
	{
		this.sound.setRingerMode (state ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_VIBRATE);
		
		return this.getSound ();
	}

    @JavascriptInterface
	public boolean getAirplaneMode ()
	{
		return (Settings.System.getInt (this.context.getContentResolver (), Settings.System.AIRPLANE_MODE_ON, 0) == 1);
	}

    @JavascriptInterface
	public boolean setAirplaneMode (boolean state)
	{
		return Settings.System.putInt (this.context.getContentResolver (), Settings.System.AIRPLANE_MODE_ON, state ? 1 : 0);
	}
}
