<?php
// A simple script to generate a starting point for a new palette kinemage
$colors = array("red", "orange", "gold", "yellow", "lime", "green", "sea", "cyan",
    "sky", "blue", "purple", "magenta", "hotpink", "pink", "peach",
    "invisible", "yellow", "invisible", "sea", "invisible", "cyan",
    "invisible", "sky", "lilac", "invisible", "invisible",
    "pinktint", "peachtint", "invisible", "yellowtint", "invisible", "greentint",
    "invisible", "invisible", "invisible", "bluetint", "lilactint", "invisible", "invisible",
    "invisible", "invisible", "invisible", "invisible", "invisible", "invisible",
    "white", "gray", "brown", "invisible", "invisible ", "deadwhite", "deadblack", );
$columns = array("saturated", "semi-sat", "tints", "neutrals");
$colorcnt = 0;

echo("@kinemage 1\n");
echo("@viewid {Full screen KiNG}\n");
echo("@span 115\n");
echo("@zslab 200.0\n");
echo("@center 45.5 -50.0 0.0\n");
echo("@matrix 1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0\n");
echo("@master {saturated}\n@master {semi-sat}\n@master {tints}\n@master {neutrals}\n");
echo("@master {color bars}\n@master {labels}\n");
echo("@animation {desaturate} {saturated} {semi-sat} {tints} {neutrals}\n");
echo("@group {palette} dominant\n");
for($i = 0; $i < 100; $i += 25)
{
    for($j = 0; $j > -100; $j -=8)
    {
        if($colors[$colorcnt] != "invisible")
        {
            echo("@subgroup {".$colors[$colorcnt]."} dominant master= {".$columns[$i/25]."}\n");
            echo("@labellist {x} color= gray master= {labels}\n");
            echo("{".$colors[$colorcnt]."} ".($i-4)." ".($j+1)." 0\n");
            echo("@vectorlist {x} color= $colors[$colorcnt] width= 4 master= {color bars}\n");
            echo("{}P ".($i)." ".($j-0)."  40 {} ".($i+20)." ".($j-0)."  40\n");
            echo("{}P ".($i)." ".($j-1)."  20 {} ".($i+20)." ".($j-1)."  20\n");
            echo("{}P ".($i)." ".($j-2)."   0 {} ".($i+20)." ".($j-2)."   0\n");
            echo("{}P ".($i)." ".($j-3)." -20 {} ".($i+20)." ".($j-3)." -20\n");
            echo("{}P ".($i)." ".($j-4)." -40 {} ".($i+20)." ".($j-4)." -40\n");
        }
        $colorcnt++;
    }
}
?>
