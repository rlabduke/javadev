# phenyl group with circle in ring
bond 30 1
push
bond 60 1
bond -60 1
bond 60 1
bond 60 1
bond 60 1
bond 60 1
bond 60 1
jump 120 1
circle .7
pop
bond -60 1

# simple phenyl group
new
bond 30 1
push
bond 60 1
doublebond -60 1 left
arc .5 160 -90 arrow
bond 60 1
doublebond 60 1 left
arc .5 160 -90 arrow
bond 60 1
doublebond 60 1 left
arc .5 160 -90 arrow
bond 60 1
pop
bond -60 1

# another take on the simple phenyl ring
new
bond 30 1
push
bond 60 1
bond -60 1
doublebond 60 1 left
arc .5 -170 220 arrow
bond 60 1
doublebond 60 1 left
arc .5 -170 220 arrow
bond 60 1
doublebond 60 1 left
arc .5 -170 220 arrow
pop
bond -60 1

