<?php
header ('Content-type: text/css');

$dir = scandir ('img/wallpapers');
$wallpapers = array ();
for ($i = 0; $i < count ($dir); $i++)
{
	if (($dir[$i] != '.') && ($dir[$i] != '..'))
	{
		array_push ($wallpapers, 'img/wallpapers/' . $dir[$i]);
	}
}
$wallpaper = $wallpapers[array_rand ($wallpapers)];
?>

div#desktop
{
	background: black url('<?php echo $wallpaper; ?>');
	background-repeat: no-repeat;
	background-position: center;
	background-size: cover;
}
