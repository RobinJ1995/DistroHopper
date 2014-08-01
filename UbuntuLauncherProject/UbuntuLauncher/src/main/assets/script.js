var avgColour;
var runningJellyBean = false;
var dashOpened = false;
var appOpened = false;

var dashAppInfoPinned = false;
var dashAppInfoIndex = -1;

var hoverAppIndex = -1;
var hoverAppPinned = false;

var dashSearchPattern = '';

$(document).ready
(
	function ()
	{
		try
		{
			runningJellyBean = android.isRunningJellyBean ();
			
			if (runningJellyBean)
				$('body').addClass ('versionJb');
			else
				$('body').addClass ('versionIcs');
			
			$('div#desktop div#wallpaper').css ('background-image', 'url(' + android.getWallpaper ().getPath () + ')');
			avgColour = android.getWallpaper ().getAverageColourRgb ();
			
			// var launcherColour = android.getWallpaperAverageColourDarker ();
			var launcherColour = avgColour;
			var style = '.chameleonic { background-color: rgba(' + avgColour + ',0.6) !important; } .chameleonicLight { background-color: rgba(' + launcherColour + ',0.2) !important; } .chameleonicVeryLight { background-color: rgba(' + avgColour + ',0.1) !important; } .dashOpened div.unity.panel{ background-color: rgba(' + launcherColour + ',0.2) !important; background-image: none; } .dashOpened div.unity.launcher { background-color: rgba(' + launcherColour + ',0.2) !important; }';
			$('html head').append ('<style type="text/css">' + style + '</style>');
			
			if (android.hasAsyncGetInstalledAppsCompleted ()) // The task might have completed before the DOM was loaded //
				asyncGetInstalledAppsCompleted ();
			if (android.hasAsyncLoadPinnedAppsCompleted ())
				asyncLoadPinnedAppsCompleted ();
		}
		catch (ex)
		{
			handleException (ex);
		}
		
		/*# System #*/
		$('#exception').on ('click',
			function ()
			{
				alert ("Oops, it seems like an error occured! If you don't mind helping me (the developer) out, please send an e-mail to android-dev@robinj.be. Include when this error occured, which device you have, and what the red bar at the bottom of the screen tells you. This information will help me fix the problem.");
			}
		);
		
		$('div.windowClose').on ('click',
			function ()
			{
				try
				{
					$('div.window div.windowPreferences').hide (0);
					$('div.window').hide (0);
					
					setAppOpened (false);
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		/*# Dash #*/
		$('div.launcherIcon.bfb').on ('click',
			function ()
			{
				try
				{
					if (! getDashOpened ())
						openDash ();
					else
						closeDash ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('div.dashRibbon img').on ('click',
			function ()
			{
				try
				{
					if ($(this).hasClass ('ribbonRecent'))
						openDashRecent ();
					else if ($(this).hasClass ('ribbonApps'))
						openDashApps ();
					else if ($(this).hasClass ('ribbonFiles'))
						openDashFiles ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('div.dashClose').on ('click',
			function ()
			{
				try
				{
					closeDash ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.appList').on ('click', '.appLauncher',
			function (e)
			{
				try
				{
					var id = parseInt ($(this).attr ('data-index'));
					launchApp (id);
				}
				catch (ex)
				{
					handleException (ex);
				}
				
			}
		);
		
		$('.recentApps').on ('click', '.appLauncher',
			function (e)
			{
				try
				{
					var id = parseInt ($(this).attr ('data-index'));
					launchRecentApp (id);
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.appList').on ('touchstart', '.appLauncher',
			function (e)
			{
				try
				{
					hoverAppIndex = parseInt ($(this).attr ('data-index'));
					hoverAppPinned = false;
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.launcherApps').on ('touchstart', '.appLauncher',
			function (e)
			{
				try
				{
					hoverAppIndex = parseInt ($(this).attr ('data-index'));
					hoverAppPinned = true;
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.unity.launcher .launcherApps').on ('click', '.appLauncher',
			function (e)
			{
				try
				{
					android.launchPinnedApp (parseInt ($(this).attr ('data-index')));
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.dashSearch input').focus // keyUp no longer detects backspace because of a bug in Android //
		(
			function (e)
			{
				try
				{
					startDashSearch ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.dashSearch input').blur
		(
			function (e)
			{
				try
				{
					dashSearchPattern = '';
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		$('.dashAppInfo').on ('click', '.appInfoBack',
			function (e)
			{
				try
				{
					openDashApps ();
				}
				catch (ex)
				{
					handleException (ex);
				}
				
			}
		);
		
		$('.dashAppInfo').on ('click', '.appInfoPin',
			function (e)
			{
				try
				{
					if (! dashAppInfoPinned)
					{
						dashAppInfoIndex = android.pinApp (dashAppInfoIndex);
						dashAppInfoPinned = true;
					
						openDashAppInfo (dashAppInfoIndex, dashAppInfoPinned);
					}
					else
					{
						android.unpinApp (dashAppInfoIndex);
						dashAppInfoPinned = false;
					
						$('.dashAppInfo .appInfoBack').trigger ('click');
					}
				
					refreshPinnedApps ();
				}
				catch (ex)
				{
					handleException (ex);
				}
				
			}
		);
		
		$('.dashAppInfo').on ('click', '.appInfoLaunch',
			function (e)
			{
				try
				{
					if (dashAppInfoPinned)
						android.launchPinnedApp (parseInt (dashAppInfoIndex));
					else
						android.launchApp (parseInt (dashAppInfoIndex));
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		/*# Preferences #*/
		$('div.launcherIcon.launchPreferences').on ('click',
			function ()
			{
				try
				{
					android.openMenu ();
					//openPreferences ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
		
		/*# Panel #*/
		$('.indicator.powerCog').on ('click',
			function ()
			{
				try
				{
					android.openMenu ();
				}
				catch (ex)
				{
					handleException (ex);
				}
			}
		);
	}
);

function handleException (ex)
{
	try
	{
		$('#exception').hide (0).html (ex).show (0);
	}
	catch (e)
	{
		alert ('Unhandled exception: ' + ex);
	}
}

/*# Events #*/
function event_backButtonPressed ()
{
	try
	{
		if (dashOpened)
			closeDash ();
		else if (appOpened)
			closeEverything ();
		else
			android.appQuit ();
	
	}
	catch (ex)
	{
		handleException (ex)
	}
}

function event_longPress ()
{
	try
	{
		if (typeof hoverAppIndex !== 'undefined')
		{
			if (! $('body').hasClass ('dashOpened'))
				openDash (false);
			
			openDashAppInfo (hoverAppIndex, hoverAppPinned);
		}
	}
	catch (ex)
	{
		handleException (ex)
	}
}

/*# Other stuff #*/

function closeEverything ()
{
	$('div.windowClose').trigger ('click');
	closeDash ();
}

function openDash (shouldOpenDashRecent)
{
	if (typeof shouldOpenDashRecent === 'undefined')
		shouldOpenDashRecent = true;
	
	$('body').addClass ('dashOpened');
	setDashOpened (true);
	
	if (shouldOpenDashRecent)
		openDashRecent ();
}

function openDashRecent ()
{
	$('div.dashContent div.dashPage').hide (0);
	
	$('div.dashRibbon img').removeClass ('activeRibbonItem');
	$('div.dashRibbon img.ribbonRecent').addClass ('activeRibbonItem');
	
	$('div.dashPage').removeClass ('activeDash');
	$('div.dashPage.dashRecent').addClass ('activeDash');
	
	var html = android.getInstalledAppsHtml ();
	
	$('div.dashPage.dashRecent div.appList').html (html);
	$('div.dashContent div.dashPage.dashRecent').show (0);
}

function openDashApps ()
{
	$('div.dashContent div.dashPage').hide (0);
	
	$('div.dashRibbon img').removeClass ('activeRibbonItem');
	$('div.dashRibbon img.ribbonApps').addClass ('activeRibbonItem');
	
	$('div.dashPage').removeClass ('activeDash');
	$('div.dashPage.dashApps').addClass ('activeDash');
	
	var html = android.getInstalledAppsHtml ();
	
	$('div.dashPage.dashApps div.appList').html (html);
	$('div.dashContent div.dashPage.dashApps').show (0);
}

function openDashFiles ()
{
	$('div.dashContent div.dashPage').hide (0);
	
	$('div.dashRibbon img').removeClass ('activeRibbonItem');
	$('div.dashRibbon img.ribbonFiles').addClass ('activeRibbonItem');
	
	$('div.dashPage').removeClass ('activeDash');
	$('div.dashPage.dashFiles').addClass ('activeDash');
	
	$('div.dashPage.dashFiles div.fileList').html (getHomeDirectoryListString ());
	$('div.dashContent div.dashPage.dashFiles').show (0);
}

function openDashSearchResults ()
{
	if (! $('div.dashPage.dashSearchResults').hasClass ('activeDash'))
	{
		$('div.dashContent div.dashPage').hide (0);
	
		$('div.dashRibbon img').removeClass ('activeRibbonItem');
	
		$('div.dashPage').removeClass ('activeDash');
		$('div.dashPage.dashSearchResults').addClass ('activeDash');
	
		$('div.dashContent div.dashPage.dashSearchResults').show (0);
	}
}

function openDashAppInfo (index, pinned)
{
	if (typeof pinned === 'undefined')
		pinned = false;
	
	$('div.dashContent div.dashPage').hide (0);
	
	$('div.dashRibbon img').removeClass ('activeRibbonItem');
	$('div.dashRibbon img.ribbonApps').addClass ('activeRibbonItem');
	
	$('div.dashPage').removeClass ('activeDash');
	$('div.dashPage.dashAppInfo').addClass ('activeDash');
	
	$('.dashAppInfo .appInfoPin').html (pinned ? 'Remove from Launcher' : 'Pin to Launcher');
	
	dashAppInfoPinned = pinned;
	dashAppInfoIndex = parseInt (index);
	
	// Without parseInt (), index would always be 0 on the Java side //
	if (pinned)
		var info = android.getPinnedAppInfoHtml (parseInt (index));
	else
		var info = android.getInstalledAppInfoHtml (parseInt (index));
	
	$('div.dashPage.dashAppInfo .appInfo').html (info);
	$('div.dashContent div.dashPage.dashAppInfo').show (0);
}

function closeDash ()
{
	$('div.dashApps').hide (0,
		function ()
		{
 			$('body').removeClass ('dashOpened');
			$('div.dashContent div.dashPage').hide (0);
			setDashOpened (false);
		}
	);
}

function launchApp (i)
{
	try
	{
		android.launchApp (i);
		updateRecentApps ();
	}
	catch (ex)
	{
		handleException (ex);
	}
}

function launchRecentApp (i)
{
	try
	{
		android.launchRecentApp (i);
		updateRecentApps ();
	}
	catch (ex)
	{
		handleException (ex);
	}
}

function addRecentAppToLauncher (id, showInLauncher)
{
	if (typeof showInLauncher === 'undefined')
		showInLauncher = true;
	
	var launcherId = android.addRecentToLauncher (id);
	var label = android.getRecentApplicationLabel (id);
	var icon = android.getRecentApplicationIconFile (id);
	var colour = android.getRecentApplicationIconAverageColour (id);
	$('div.unity.launcher div.launcherApps').append ('<div class="launcherIcon appLauncher" style="background-color: rgba(' + colour + ',0.8);" id="appLauncher' + launcherId + '" onClick="launchFromLauncher (' + launcherId + ');"><img src="' + icon + '" alt="' + label + '" /></div>');
	
	if (showInLauncher)
		$('#appLauncher' + launcherId).show (0);
}

function openPreferences ()
{
	try
	{
		closeEverything ();
		
		$('div.window').show (0);
		$('div.window div.windowPreferences').show (0);
		
		setAppOpened (true);
	}
	catch (ex)
	{
		handleException (ex);
	}
}

function setAppOpened (val)
{
	appOpened = val;
}

function setDashOpened (val)
{
	dashOpened = val;
}

function getAppOpened ()
{
	return appOpened;
}

function getDashOpened ()
{
	return dashOpened;
}

function refreshPinnedApps ()
{
	$('div.unity.launcher div.launcherApps').html (android.getPinnedAppsHtml ('launcherIcon'));
}

function updateRecentApps ()
{
	//TODO//
	return void 0;
}


function startDashSearch ()
{
	window.setTimeout (dashSearchIteration, 100);
}

function dashSearchIteration ()
{
	var newVal = $('.dashSearch input').val ();
	
	if (newVal != dashSearchPattern)
	{
		dashSearchPattern = newVal;
		dashSearch (newVal);
	}
	
	if ($('.dashSearch input').is (':focus'))
		window.setTimeout (dashSearchIteration, 50);
}

function dashSearch (pattern)
{
	openDashSearchResults ();
	
	var results = android.searchApps (pattern);
	
	var selector = '';
	for (var i = 0; i < results.size (); i++)
	{
		if (i > 0)
			selector += ', '
		selector += '.dashRecent .appList .appLauncher[data-index=' + results.getString (i) + ']';
	}
	
	var $apps = $(selector).clone ();
	$('.dashSearchResults .appList').html ($apps);
}

function asyncGetInstalledAppsCompleted ()
{
	$('.launcherIcon.launcherSpinner').hide ();
	$('.launcherIcon.bfb').show ();
}

function asyncLoadPinnedAppsCompleted ()
{
	refreshPinnedApps ();
}