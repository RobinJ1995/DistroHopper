<?php
require_once ('curl.php');
$lastCommit = json_decode (file_get_contents ('cron/lastCommit.json'));
?>
<!DOCTYPE html>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<title>DistroHopper</title>
	<link type="text/css" rel="stylesheet" href="css/stylesheet.css" />
	<link type="text/css" rel="stylesheet" href="wallpaper.php" />
	<script type="text/javascript" src="js/jquery.js"></script>
	<script type="text/javascript" src="js/script.js"></script>
</head>
<body>
<div id="desktop">
	<div class="unity panel">
	</div>
	<div class="unity launcher">
		<div class="launchericon bfb">
			<img src="img/unity.launcher.bfb.png" alt="Dash Home" />
		</div>
		<div style="background-color: #D45646;" class="launchericon" id="googleplus">
			<a href="https://plus.google.com/u/0/communities/111207292310016778258">
				<img alt="Google+" src="https://ssl.gstatic.com/images/icons/gplus-64.png">
			</a>
		</div>
		<div style="background-color: #7CBAE6;" class="launchericon" id="github">
			<a href="https://github.com/RobinJ1995/be.robinj.ubuntu">
				<img alt="GitHub" src="img/unity.launcher.github.png">
			</a>
		</div>
		<div class="launchericon trash">
			<a href="http://is.gd/10QgK7" target="_blank">
				<img src="img/unity.launcher.trash.empty.png" alt="Rubbish Bin" />
			</a>
		</div>
	</div>
	<div class="unity dash">
		<div class="dash ribbon">
			<img src="img/unity.dash.ribbon.home.png" alt="Home" class="ribbonicon" id="ribbonhome" />
			<img src="img/unity.dash.ribbon.photo.png" alt="Screenshots" class="ribbonicon" id="ribbonscreenshots" />
		</div>
		
		<div class="dash page" id="home">
			<section id="intro">
				<h1>Ubuntu Launcher</h1>
				<p><em>The Ubuntu Unity desktop on your Android device.</em></p>
				<p>Do you like Linux/Ubuntu? Does it seem awesome to you to be able to have the Unity desktop as your Android home screen? No? Ok, bye bye. In that case, this isn't for you.<br />This work in progress will allow you to have the Unity desktop running as your Android home/launcher screen.<p>
				
				<h2>Disclaimer</h2>
				<p>Ubuntu is a registered trademark of Canonical Ltd, which I am not associated with. I don't take any responsibility for... anything.</p>
				
				<h2>Download</h2>
				<p>Here you can download the last version of Ubuntu Launcher that was released on the Google Play Store. From the next release on this app will probably no longer be called <em>Ubuntu Launcher</em>. For more information read the notice on the side/below.</p>
				<p class="download">
					<a href="etc/be.robinj.ubuntu.apk">Download (529 KiB)</a><br />
					<small>Latest version: 0.5.12</small>
				</p>
				
				<p id="homeScreenshot">
					<a href="#dash#screenshots" onClick="goToPage ('ribbonscreenshots');"><img src="img/screenshots/home.png" alt="[Screenshot]" class="screenshot" /></a>
					<a href="#dash#screenshots" onClick="goToPage ('ribbonscreenshots');"><img src="img/screenshots/about.png" alt="[Screenshot]" class="screenshot" /></a>
				</p>
			</section>
			<section id="side">
				<h2><a href="<?php echo htmlentities ($lastCommit->url); ?>" title="<?php echo $lastCommit->sha; ?>">Last commit</a></h2>
				<p>
					Message: <em><?php echo $lastCommit->commit->message; ?></em><br />
					Branch: <em><?php echo $lastCommit->branch->name; ?></em><br />
					Date: <em><?php echo $lastCommit->commit->committer->date; ?></em><br />
					Changes: <em><?php echo $lastCommit->stats->total; ?></em>
				</p>
				
				<h2>Notice</h2>
				<p>Ubuntu Launcher got taken down from the Google Play Store due to "copyright infringement".<br />
				Long story short; From the next release on the project will be called DistroHopper instead of Ubuntu Launcher. It may also contain a new default theme, but the Ubuntu theme will still be available.</p>
			</section>
		</div>
		<div class="dash page" id="screenshots">
			<section class="imagegallery">
				<img src="img/screenshots/home.png" alt="[Home screen]" class="screenshot thumbnail" />
				<img src="img/screenshots/dash.png" alt="[Dash]" class="screenshot thumbnail" />
				<img src="img/screenshots/dash_search.png" alt="[Dash search]" class="screenshot thumbnail" />
				<img src="img/screenshots/about.png" alt="[About]" class="screenshot thumbnail" />
			</section>
		</div>
	</div>
</div>
</body>
</html>
