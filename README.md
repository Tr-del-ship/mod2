# Storm Baton

A Fabric mod for Minecraft Java Edition 26.2. The Storm Baton is a separate item with the same in-game appearance as the vanilla stick.

## Behavior

- A normal hit launches a living mob away and upward, then calls lightning down on it after a short delay.
- Attacking while sprinting also pulls nearby mobs toward the target.
- Attacking while crouching pulls the player and target toward each other, then strikes the target. Crouching takes priority over sprinting.

## Build

Requires Java 25 and Gradle. Run `gradle build`; the mod JAR is written to `build/libs/`.

Install the JAR in the `mods` folder alongside Fabric Loader and Fabric API for Minecraft 26.2. Give yourself the item with:

```text
/give @s storm_baton:storm_baton
```
