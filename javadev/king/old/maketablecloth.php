<?php
$scale = 2;
$squares = 20;

echo "@kinemage 1\n";
echo "@group {x} dominant\n@subgroup {x} dominant\n";

for($x = 0; $x < $squares; $x++)
{
    for($y = 0; $y < $squares; $y++)
    {
        if(($x + $y) % 2 == 0)  echo "@trianglelist {x} color= red\n";
        else                    echo "@trianglelist {x} color= white\n";
        echo "{x} ".(($x)*$scale)." 0 ".(($y)*$scale)."\n";
        echo "{x} ".(($x+1)*$scale)." 0 ".(($y)*$scale)."\n";
        echo "{x} ".(($x)*$scale)." 0 ".(($y+1)*$scale)."\n";
        echo "{x} ".(($x+1)*$scale)." 0 ".(($y+1)*$scale)."\n";
    }
}
?>
