# High-Albedo

The readme on the master / Twisted-Skies branch covers the basics of what High Albedo is.

Dark Frontier
-------------
"High Albedo: Dark Frontier" will eventually be the sequel to Twisted Skies. The goal is to add new features, improve graphics, and make the user experience nicer.

This readme will contain a list of goals for Dark Frontier. They will be marked complete when they are completed.

Gameplay Goals
---------------
* Frontier space. Frontier systems aren't always connected and their connections move at random as jumpholes appear and disappear unlike the static jumpholes connecting settled space.
* Carriers. These new ships will allow fighters to dock with them, and transport them quickly using their jump drive. Carriers will come in different sizes, including capital ship varieties. Fighters will be assigned to a carrier, which will automatically control its own wings.
* Beam weapons. There are currently no beam weapons, and the engine does not support them.
* AI difficulty variations. Right now everyone is an ace pilot.
* Improved missile guidance. Missiles are kind of poor at hitting things in many cases.
* More complex NPC interactions / standings.
* Mobile mining (aka mining with guns) on asteroids that haven't had a mine built.
* Wider variety of asteroids and minerals, ideally revamping the economy to require all of them.
* Improved collision detection. I want as close to pixel-perfect as possible.
* Additional campaigns. This is subject to further design.
* Additional factions.

Art Goals
---------
* General art pass to improve the quality of existing sprites.
* Visible turrets / batteries.
* Visible cannons / launchers.
* Visible shields.
* Visible engine effects / trails.
* Improve sound effects.
* Make distant sounds quieter.
* More lore and backstory, rumors, etc.
* Improved soundtrack.
* Procedurally generated solar system backplates.

UI Goals
--------
* Make the UI hover-to-focus instead of click-to-focus because it makes the UI hard to use in combat. - COMPLETED
* Remappable controls.
* Allow ships to be grouped into wings which can be ordered as a unit.
* Allow the player to command ships they own that are visible on their screen using an RTS style interface with drag selection.
* Allow the player to zoom the camera.
* Add a global market view which shows buy and sell prices of goods in stations the player is friendly to worldwide. This won't allow buying and selling but will make finding trade routes easier.
* Allow windows to behave like proper windows the player can move around the screen, resize, etc.
* Allow the player to manually set the price of goods in their station.
* Overall UI cleanup.
* Make the UI able to scale with extremely high resolution displays such as retina.
* Mission progress indicator. Right now mission progress must be remembered or reconstructed through messages.
* Add a loading screen.
* Game over screen when you die, and don't exit save if you're dead.

Performance / Tuning Goals
--------------------------
* Optimization pass of existing code.
* Take more advantage of multi threading in the simulation.
* Fix the temporary freeze (sometimes >30 seconds) that occurs when a procedural planet sprite is being generated. -- IN PROGRESS
