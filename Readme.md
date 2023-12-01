AI for the Softwarechallenge 2018 featuring the game "Hase und Igel" (Hare and Tortoise)

[Game rules (german)](Regeln.pdf)

[Game server & Contest infos](https://www.software-challenge.de)

### The AI

Jumper uses an Alpha–beta pruning approach, with three basic components:
- `findBestMove` - starts the search and manages queues, nodes, depth and timing
- `findMoves` - find Moves for a given GameState
- `evaluate` - evaluate a given GameState

[Sketch of my latest strategy (german)](Jumper3.md)

The code is a bit messy, a lot was written in the final days,
and some comments mix german and english.
Don't expect me to clean up, the contest is over ^^

#### Training

To finetune the parameters at the end,
I put all computers of my school to work over a whole weekend.
They ran the AI with different parameters against a reference version,
from time to time updating the reference version with updated parameters if a particularly effective modification was found.
Unfortunately the end results did not carry sufficient statistical significance to make a big difference.
That is what the scripts without extension at the root of this repo are mostly for.

### The Team

I am a German student in grade 12 and developed MANOS 12 Jackrabbit entirely by myself.
I give thanks to my informatics teacher, without him I wouldn't even be aware of the competitions existence!

### Results

I easily reached the top 24 and got to [place 6](https://contest.software-challenge.de/wettbewerb/2018_champions/rangliste)
in the champions league due to a bit of laziness in the following weeks.
But everyone from the top 8 ascended to the [finals](https://contest.software-challenge.de/wettbewerb/2018_champions/finale/lineup).

The last week before the competition I worked ceaselessly to fix mistakes and create a new logic,
so in the end I reached the second place in an extremely close final match.
