var dashOpened = false;
var galleryBusy = false;

$(document).ready
(
	function ()
	{
		toggleDash ();
		
		$(document).keypress
		(
			function (key)
			{
				if (key.which == 0) // Why are all the Fx keys and the super key 0? >.<
				{
					toggleDash ();
				}
			}
		);
		
		$('div.launchericon.bfb').click
		(
			function ()
			{
				toggleDash ();
			}
		);
		
		$('img.ribbonicon').click
		(
			function ()
			{
				goToPage ($(this).attr ('id'));
			}
		);
		
		$('div.dash.page#screenshots img.screenshot.thumbnail').click
		(
			function ()
			{
				$('div.dash.page#screenshots img.screenshot.thumbnail').not(this).removeClass ('zoomed');
                                $(this).addClass ('zoomed', 320);
			}
		);
	}
);

function goToPage (name)
{
	name = name.substr (6);
	if ($('div.dash.page#' + name).length != 0) // Check if it exists
	{
		$('img.ribbonicon').removeClass ('active');
		$('img.ribbonicon#ribbon' + name).addClass ('active');
		
		$('div.dash.page').fadeOut (0);
		$('div.dash.page#' + name).delay (300).fadeIn (400);
	}
	else
	{
		throw ("Page \"" + name + "\" doesn't exist");
	}
	
	document.location.hash = 'dash#' + name;		
}

function toggleDash ()
{
	$('div.unity.dash').fadeToggle (400);
	$('div.unity.launcher').toggleClass ('dashOpened');
	$('div.unity.launcher div.launchericon').toggleClass ('dashOpened');
	$('div.unity.panel').toggleClass ('dashOpened');
	dashOpened = (! dashOpened);
	
	var pageActive = false;
	$('img.ribbonicon').each
	(
		function (i, object)
		{
			if ($(object).hasClass ('active'))
			{
				pageActive = true;
			}
		}
	)
	if (dashOpened)
	{
		document.location.hash = 'dash';
		if (! pageActive)
		{
			goToPage ('ribbonhome');
		}
	}
	else
	{
		document.location.hash = 'desktop';
	}
}
