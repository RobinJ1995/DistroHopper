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
			<a href="https://github.com/RobinJ1995/DistroHopper">
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
				<h1>DistroHopper</h1>
				<p><em>The Ubuntu desktop on your Android device!</em></p>
				<p>Are you a Linux/open-source enthusiast? Whether you are or not, if it seems cool to be able to have the Linux desktop on your Android device, then this app is what you're looking for. Currently there is a choice between Ubuntu's Unity desktop, elementary OS' Pantheon desktop, and Gnome. Missing your desktop of choice? Get in touch and if there's enough interest I might just add it 😉</p>
				<p>Features include a couple of different themes, a search feature which allows you to search from a multitude of different search sources (both local and remote), and customisation options.</p>
				<p>If you have any suggestions or feedback, feel free to get in touch. The project is open-source with the source code publicly available at <a href="https://github.com/RobinJ1995/DistroHopper">https://github.com/RobinJ1995/DistroHopper</a>. If you're less technically-inclined but would still like to contribute, you can join the project's translation team over at <a href="https://www.transifex.com/distrohopper/">https://www.transifex.com/distrohopper/</a>.</p>
				
				<h2>Disclaimers</h2>
				<p>Ubuntu is a registered trademark of Canonical Ltd.<br />elementary is a registered trademark of elementary LLC.<br />Gnome is a registered trademark of the Gnome Foundation.<p>
				
				<h2>Download</h2>
				<p>You can download the latest version of DistroHopper either from the Google Play Store, or you can download the APK file here. Note that if you download and install DistroHopper via the provided APK file, you are responsible for manually keeping DistroHopper up-to-date on your device and will not automatically receive updates for it.</p>
				<p class="download">
					<a href="https://play.google.com/store/apps/details?id=be.robinj.distrohopper">Get it from the Google Play Store</a>
				</p>
				<p class="download">
					<a href="https://github.com/RobinJ1995/DistroHopper/raw/master/DistroHopper/app/app-release.apk">Download APK</a>
				</p>
				
				<p id="homeScreenshot">
					<a href="#dash#screenshots" onClick="goToPage ('ribbonscreenshots');"><img src="img/screenshots/ubuntu.png" alt="[Screenshot]" class="screenshot" /></a>
					<a href="#dash#screenshots" onClick="goToPage ('ribbonscreenshots');"><img src="img/screenshots/gnome.png" alt="[Screenshot]" class="screenshot" /></a>
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
				
				<h2>Note</h2>
				<p>This project was formerly known as Ubuntu Launcher, but got taken down from the Google Play Store due to "copyright infringement". I then changed the name to DistroHopper and implemented themes for other Linux desktops as well.</p>
			</section>
		</div>
		<div class="dash page" id="screenshots">
			<section class="imagegallery">
				<img src="img/screenshots/ubuntu.png" alt="[Ubuntu]" class="screenshot thumbnail" />
				<img src="img/screenshots/gnome.png" alt="[Gnome]" class="screenshot thumbnail" />
				<img src="img/screenshots/elementary.png" alt="[elementary]" class="screenshot thumbnail" />
				<img src="img/screenshots/search.png" alt="[Search]" class="screenshot thumbnail" />
			</section>
		</div>
	</div>
</div>
</body>
</html>
