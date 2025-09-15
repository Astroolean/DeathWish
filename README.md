# ☠️ DeathWish — Created by Astroolean

**Version:** 1.0.2  
**Author:** astroolean  

> A next-level hardcore Minecraft plugin featuring **mob scaling, unique boss fights, powerful abilities, custom loot drops, dynamic difficulty multipliers, player revival mechanics, and fully configurable progression** — designed to push survival to its limits.

> I made this for my hardcore smp server called The Final Block which will end up becoming my forever world. This plugin is very far from complete; a lot still has to be fixed and changed. However this is something I consider to be a stable prototype. Until I find the freetime to work on it further. This plugin will continue to be updated for newer versions never earlier versions. Keep in mind this plugin will only be updated when I need to update my server aka The Final Block. As long as The Final Block is being updated this will be updated too. I have no idea how to setup bstats or metrics and completely winged this entire project. This is far from perfect, but is HIGHLY configurable. Completely open-source and free forever!

> I will try to push a flawless stable build by the end of the year. Looking to play a bit before I work 24/7 again lmfao... Please rate this and review this honestly not because of its current state but what the plugin could offer and provide. This is solely just a prototype I believe could turn into something a lot of people would love to use. I needed something like this and im sure many others do too. Im no where near an expert in programming but with the power of AI I managed to make this... 

---

## 📑 Table of Contents

1. ✨ Features  
2. 📸 Screenshots  
3. 🔐 Commands & Permissions  
4. ⚙️ Configuration  
5. 🗄️ Data Structure  
6. 🧠 Developer Notes  
7. ✅ Usage Examples  
8. 🛠️ Troubleshooting  
9. 📜 License & Contributing  

---

## ✨ Features

- ☠️ **Hardcore Death & Revival**
  - True hardcore experience: when you die, you’re marked as dead until revived.
  - Revive with **player heads**, or via special admin commands.
  - `/deadlist` keeps track of who’s alive vs. who’s fallen.

- 📈 **Dynamic Difficulty & Scaling**
  - Custom multipliers make mobs stronger over time.
  - Configurable difficulty curves ensure the game never feels “too easy.”
  - Supports timed abilities and scaling challenges.

- 👹 **Mob Scaling & Boss Encounters**
  - Normal mobs gain boosted health, damage, and effects.
  - Custom boss fights with unique mechanics.
  - Keeps end-game dangerous and engaging.

- 🎁 **Custom Loot System**
  - Rare drops and rewards balanced around the hardcore system.
  - Tie loot progression to server difficulty for a smooth gameplay arc.

- 🎨 **Styled Messaging**
  - Gradient text and colored chat utilities built-in.
  - Every notification looks polished and modern.

- 📜 **Extensive Configurability**
  - `config.yml` lets you adjust multipliers, scaling rates, revival rules, and more.
  - `data.yml` persists player states across server restarts.
  - `plugin.yml` defines every command & permission for easy setup.

- ⚡ **Lightweight & Efficient**
  - Built with performance in mind — minimal overhead even on large servers.
  - Maven-powered build system for easy development & updates.

- 🔧 **Admin-Friendly**
  - Full suite of commands for reviving, force-reviving, reloading configs, and managing hardcore progression.
  - Permission nodes let you fine-tune who has access to what.

- 🔄 **Replayable Hardcore Mode**
  - Every run feels different thanks to scaling, bosses, and loot.
  - Designed for **long-term progression** and **community challenges**.

---

## 📸 Screenshots 

<img width="869" height="226" alt="e8eb7f07f3268572cca5e227fb7fc2f3" src="https://github.com/user-attachments/assets/fce36e6a-dc3a-450d-b3fd-24f9024dc4d4" />
<img width="996" height="349" alt="8fa68c4e8e97184b143d466e02300e44" src="https://github.com/user-attachments/assets/dbdb3e0d-fcf7-430c-9d70-29f9d22a97e5" />
<img width="1920" height="1057" alt="2025-09-15_00 13 18" src="https://github.com/user-attachments/assets/6d5a2195-46b8-4047-b93f-305513b552ac" />
<img width="1920" height="1057" alt="2025-09-15_00 13 36" src="https://github.com/user-attachments/assets/d680416b-39e4-480e-8617-357a694b16cc" />
<img width="1920" height="1057" alt="2025-09-15_00 15 21" src="https://github.com/user-attachments/assets/74b10c4d-d47f-4fdb-8e30-9e3e98f70793" />

---

## 🔐 Commands & Permissions

Use a permissions manager (LuckPerms / UltraPermissions) to assign the nodes below.

### Commands

| Command     | Description                        | Usage       | Aliases   |
|-------------|------------------------------------|-------------|-----------|
| `/deathwish`| Base command for DeathWish          | `/deathwish`| `/dw`     |
| `/revive`   | Revive a dead player using their head | `/revive <player>` | - |
| `/deadlist` | Shows all currently dead players    | `/deadlist` | -         |
| `/gg`       | Force revive a player (admin only)  | `/gg <player>` | -      |
| `/dwreload` | Reload the DeathWish config         | `/dwreload` | -        |

### Permissions

| Permission         | Default | Description                              |
|--------------------|---------|------------------------------------------|
| `deathwish.use`    | true    | Access to DeathWish base command          |
| `deathwish.revive` | true    | Allows reviving other players with heads |
| `deathwish.deadlist`| true   | Allows viewing the dead players list      |
| `deathwish.gg`     | op      | Force revive a player (admin only)        |
| `deathwish.reload` | op      | Reload plugin configs                     |

---

## ⚙️ Configuration (`config.yml`)

Edit these keys to tune behavior.

```yaml
# ================================
# DeathWish Master Configuration
# Version: 1.0.2
# ================================

# ----------------
# Plugin Settings
# ----------------
settings:
  bstats:
    enabled: true     # Set to false if you want to disable anonymous usage metrics
  update-checker:
    enabled: true     # Checks for new plugin versions on startup
    notify-ops: true  # If true, operators will see update notifications on join

# ----------------
# General Settings
# ----------------
general:
  # Prefix shown before plugin messages
  prefix: "<gradient:#00FF00:#FFFFFF>[DeathWish] </gradient>"

  # Enable debug logs (extra console info)
  debug: false

  # Worlds where DeathWish is active
  enabled-worlds:
    - world
    - world_nether
    - world_the_end

  # How often difficulty multipliers are reapplied (ticks)
  difficulty-reapply-ticks: 6000 # 5 minutes

  # Enable/disable custom revive recipe system
  recipe-enabled: true

# ----------------
# Messages
# ----------------
messages:
  revive-broadcast: "<gradient:#00FF00:#FFFFFF>[DeathWish] Player </gradient><gradient:#FF0000:#FFFFFF>{player}</gradient> <gradient:#00FF00:#FFFFFF>has been</gradient> </gradient><gradient:#FF0000:#FFFFFF>REVIVED</gradient>"
  not-dead: "<gradient:#FF0000:#FFFFFF>{player}</gradient> <gradient:#00FF00:#FFFFFF>is not dead.</gradient>"
  need-head: "<gradient:#00FF00:#FFFFFF>You must hold</gradient> <gradient:#FF0000:#FFFFFF>{player}'s</gradient> <gradient:#00FF00:#FFFFFF>head in your main hand to revive them!</gradient>"
  nobody-dead: "<gradient:#00FF00:#FFFFFF>Nobody is dead right now.</gradient>"
  permadeath: "<gradient:#FF0000:#FFFFFF>{player}</gradient> <gradient:#00FF00:#FFFFFF>has died permanently! Revive them... if you dare.</gradient>"
  cooldown-active: "<gradient:#00FF00:#FFFFFF>You must wait</gradient> <gradient:#FF0000:#FFFFFF>{time}</gradient> <gradient:#00FF00:#FFFFFF>before reviving again.</gradient>"
  revived-self: "<gradient:#00FF00:#FFFFFF>You revived</gradient> <gradient:#FF0000:#FFFFFF>{player}</gradient> <gradient:#00FF00:#FFFFFF>successfully!</gradient>"
  deadlist-header: "<gradient:#00FF00:#FFFFFF>Dead Players:</gradient>"
  deadlist-entry: "<gradient:#00FF00:#FFFFFF>➪</gradient> <gradient:#FF0000:#FFFFFF>{player}</gradient>"
  deadlist-empty: "<gradient:#00FF00:#FFFFFF>No one is dead.</gradient>"

# ----------------
# Revive System
# ----------------
revive:
  # Global cooldown between revives (per player in seconds)
  cooldown-seconds: 86400 # 24 hours

  # Automatically revive dead players when they rejoin (if their head was crafted)
  revive-on-join: true

  # Storage (⚠️ do not edit manually unless you know what you’re doing)
  data:
    dead: []
    reviveTimestamps: {}

  # ----------------
  # Revive Recipe
  # ----------------
  recipe:
    # Recipe shape (3 lines, 3 chars each)
    shape:
      - "DDD"
      - "DND"
      - "DDD"

    # Keys that correspond to items
    keys:
      D: "DIAMOND"
      N: "NETHER_STAR"

    # Customize crafted head item
    item:
      # Display name (supports MiniMessage)
      name: "<gradient:#00FF00:#FFFFFF>Revive Head:</gradient> <gradient:#FF0000:#FFFFFF>{player}</gradient>"

      # Lore (list of lines, MiniMessage supported)
      lore:
        - "<gradient:#00FF00:#FFFFFF>This head belongs to</gradient> <gradient:#FF0000:#FFFFFF>{player}</gradient>"
        - "<gradient:#00FF00:#FFFFFF>Use it to bring them back to life.</gradient>"
        - "<gradient:#00FF00:#FFFFFF>One-time use</gradient>"

# ----------------
# Item & Loot Settings
# ----------------
items:
  # Default gradients for plugin-created items
  defaults:
    name-gradient: "<gradient:#00FF00:#FFFFFF>"
    lore-gradient: "<gradient:#FF0000:#FFFFFF>"

  # Toggle if crafted heads/lore can be overridden by config
  allow-custom-items: true

# ----------------
# Difficulty Scaling
# ----------------
difficulty:
  # Enable/disable difficulty multipliers globally
  enabled: true

  # Safety clamps to avoid runaway multipliers (set to 0 to disable)
  max-multiplier-health: 5.0
  max-multiplier-damage: 5.0

  # If true, only apply scaling to mobs defined in mobs.overrides (recommended)
  apply-only-overrides: false

  # If true, apply scaling at spawn time (recommended). If false, server will rely on applyDifficultyAll().
  apply-at-spawn: true

  # Default scaling applied to all mobs (feels like Hard+ mode)
  defaults:
    health-multiplier: 1.4
    damage-multiplier: 1.3

  # Global effects that apply to ALL mobs (immersive baseline buffs)
  global-effects:
    - "DAMAGE_RESISTANCE:0:99999"  # Slight toughness buff
    - "FAST_DIGGING:0:99999"       # Faster attack animations / reactions

  # Progressive scaling options
  scaling:
    time-based:
      enabled: true                # true = mobs scale with day count
      multiplier-per-day: 0.05     # +5% per Minecraft day

    death-based:
      enabled: true                # true = mobs scale with total player deaths
      multiplier-per-death: 0.1    # +10% per death

    boss-kill:
      enabled: true                # true = mobs scale once bosses are defeated
      multiplier-per-boss: 0.2     # +20% per boss kill

# ----------------
# Experimental Features
# ----------------
experimental:
  # Placeholder for future systems (scaling loot tables, event bosses, etc.)
  # Only read this after viewing source code and understanding this project.
  # This will only work if there are new features that are not released yet.
  enabled: false

# ----------------
# Passive Settings
# ----------------
passive-mobs:
  default:
    enabled: true
    health-multiplier: 1.0
    gradient-name: "<gradient:#00FF7F:#FFFFFF>{mob}</gradient>"
    effects: []
    custom-loot: []

  overrides:

    Bat:
      enabled: true
      health-multiplier: 1.0
      gradient-name: "<gradient:#708090:#A9A9A9>Bat</gradient>"
      effects:
        - "INVISIBILITY:0:99999"
        - "SPEED:0:99999"
      custom-loot:
        - "LEATHER:0-1"
        - "FEATHER:0-1"
        - "GLOWSTONE_DUST:0-1"

    Bee:
      enabled: true
      health-multiplier: 1.0
      gradient-name: "<gradient:#FFDE00:#FFFF7F>Bee</gradient>"
      effects:
        - "SPEED:1:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "HONEY_BOTTLE:0-1"
        - "SUGAR:0-2"
        - "HONEYCOMB:0-2"

    Cat:
      enabled: true
      health-multiplier: 1.1
      gradient-name: "<gradient:#FFFFFF:#CCCCCC>Cat</gradient>"
      effects:
        - "SPEED:1:99999"
        - "SLOW_FALLING:0:99999"
      custom-loot:
        - "RAW_COD:0-1"
        - "STRING:0-2"
        - "FEATHER:0-1"

    Chicken:
      enabled: true
      health-multiplier: 1.0
      gradient-name: "<gradient:#FFFFF0:#FFD700>Chicken</gradient>"
      effects:
        - "SPEED:0:99999"
        - "JUMP:0:99999"
      custom-loot:
        - "CHICKEN:1-2"
        - "FEATHER:1-3"
        - "EGG:0-2"

    Cow:
      enabled: true
      health-multiplier: 1.1
      gradient-name: "<gradient:#8B4513:#D2691E>Cow</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "HEALTH_BOOST:0:99999"
      custom-loot:
        - "BEEF:1-3"
        - "LEATHER:1-2"
        - "MILK_BUCKET:0-1"

    Donkey:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#DAA520:#FFD700>Donkey</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "LEATHER:1-2"
        - "CHEST:0-1"
        - "IRON_INGOT:0-2"

    Frog:
      enabled: true
      health-multiplier: 1.0
      gradient-name: "<gradient:#00FF00:#32CD32>Frog</gradient>"
      effects:
        - "JUMP:1:99999"
        - "SPEED:0:99999"
      custom-loot:
        - "SLIME_BALL:0-2"
        - "SPONGE:0-1"
        - "FROGLIGHT:0-1"

    Goat:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#F5DEB3:#DEB887>Goat</gradient>"
      effects:
        - "JUMP:1:99999"
        - "SPEED:0:99999"
      custom-loot:
        - "GOAT_HORN:0-1"
        - "LEATHER:0-1"
        - "MILK_BUCKET:0-1"

    Horse:
      enabled: true
      health-multiplier: 1.3
      gradient-name: "<gradient:#CD853F:#DEB887>Horse</gradient>"
      effects:
        - "SPEED:1:99999"
        - "JUMP:1:99999"
      custom-loot:
        - "LEATHER:1-3"
        - "SADDLE:0-1"
        - "IRON_HORSE_ARMOR:0-1"

    Mule:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#DAA520:#F4A460>Mule</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "LEATHER:1-2"
        - "CHEST:0-1"
        - "IRON_INGOT:0-1"

    Ocelot:
      enabled: true
      health-multiplier: 1.1
      gradient-name: "<gradient:#FFD700:#FFE4B5>Ocelot</gradient>"
      effects:
        - "SPEED:1:99999"
        - "SLOW_FALLING:0:99999"
      custom-loot:
        - "RAW_COD:0-2"
        - "LEATHER:0-1"
        - "STRING:0-1"

    Pig:
      enabled: true
      health-multiplier: 1.1
      gradient-name: "<gradient:#FFC0CB:#FF69B4>Pig</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "SATURATION:0:99999"
      custom-loot:
        - "PORKCHOP:1-3"
        - "CARROT:0-1"
        - "POTATO:0-1"

    Rabbit:
      enabled: true
      health-multiplier: 1.0
      gradient-name: "<gradient:#FFFFFF:#E0E0E0>Rabbit</gradient>"
      effects:
        - "SPEED:1:99999"
        - "JUMP:1:99999"
      custom-loot:
        - "RABBIT_HIDE:0-2"
        - "RABBIT_FOOT:0-1"
        - "RABBIT_STEW:0-1"

    Sheep:
      enabled: true
      health-multiplier: 1.1
      gradient-name: "<gradient:#FFFFFF:#DDDDDD>Sheep</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "SATURATION:0:99999"
      custom-loot:
        - "MUTTON:1-2"
        - "WOOL:1-2"
        - "STRING:0-1"

    Turtle:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#20B2AA:#7FFFD4>Turtle</gradient>"
      effects:
        - "WATER_BREATHING:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "SCUTE:0-1"
        - "SEAGRASS:0-2"
        - "NAUTILUS_SHELL:0-1"

    Villager:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#32CD32:#ADFF2F>Villager</gradient>"
      effects:
        - "REGENERATION:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "EMERALD:0-2"
        - "BREAD:0-2"
        - "BOOK:0-1"

    Wolf:
      enabled: true
      health-multiplier: 1.2
      gradient-name: "<gradient:#A52A2A:#DEB887>Wolf</gradient>"
      effects:
        - "SPEED:0:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "RAW_PORKCHOP:0-1"
        - "LEATHER:0-1"
        - "BONE:0-1"

# ----------------
# Hostile Settings 
# ----------------
mobs:
  default:
    enabled: true
    health-multiplier: 1.5
    damage-multiplier: 1.2
    gradient-name: "<gradient:#00FF00:#FFFFFF>{mob}</gradient>"
    effects: []
    custom-loot: []

  overrides:

    Blaze:
      enabled: true
      health-multiplier: 1.4
      damage-multiplier: 1.5
      gradient-name: "<gradient:#FF6600:#FFCC00>Blaze</gradient>"
      effects:
        - "FIRE_RESISTANCE:0:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "BLAZE_ROD:1-2"
        - "BLAZE_POWDER:1-3"
        - "GOLD_NUGGET:1-2"

    CaveSpider:
      enabled: true
      health-multiplier: 1.2
      damage-multiplier: 1.2
      gradient-name: "<gradient:#00AA00:#55FF55>Cave Spider</gradient>"
      effects:
        - "SPEED:1:99999"
        - "POISON:0:200"
      custom-loot:
        - "STRING:1-3"
        - "SPIDER_EYE:0-1"
        - "FERMENTED_SPIDER_EYE:0-1"

    Creeper:
      enabled: true
      health-multiplier: 1.2
      damage-multiplier: 1.0
      gradient-name: "<gradient:#228B22:#ADFF2F>Creeper</gradient>"
      effects:
        - "RESISTANCE:0:99999"
        - "SPEED:0:99999"
      custom-loot:
        - "GUNPOWDER:2-4"
        - "SULPHUR:1-2"
        - "TNT:0-1"

    Drowned:
      enabled: true
      health-multiplier: 1.6
      damage-multiplier: 1.4
      gradient-name: "<gradient:#00CED1:#4682B4>Drowned</gradient>"
      effects:
        - "WATER_BREATHING:0:99999"
        - "SLOW:0:99999"
      custom-loot:
        - "NAUTILUS_SHELL:0-1"
        - "TRIDENT:0-1"
        - "COPPER_INGOT:1-2"

    Enderman:
      enabled: true
      health-multiplier: 2.0
      damage-multiplier: 1.6
      gradient-name: "<gradient:#551A8B:#9370DB>Enderman</gradient>"
      effects:
        - "SPEED:2:99999"
        - "STRENGTH:1:99999"
      custom-loot:
        - "ENDER_PEARL:1-2"
        - "END_ROD:0-1"
        - "OBSIDIAN:1-2"

    Endermite:
      enabled: true
      health-multiplier: 1.0
      damage-multiplier: 1.1
      gradient-name: "<gradient:#808080:#C0C0C0>Endermite</gradient>"
      effects:
        - "SPEED:1:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "CHORUS_FRUIT:0-1"
        - "ENDER_PEARL:0-1"
        - "PURPUR_BLOCK:0-1"

    Ghast:
      enabled: true
      health-multiplier: 1.6
      damage-multiplier: 1.6
      gradient-name: "<gradient:#F8F8FF:#FF6347>Ghast</gradient>"
      effects:
        - "FIRE_RESISTANCE:0:99999"
        - "LEVITATION:0:99999"
      custom-loot:
        - "GHAST_TEAR:1-2"
        - "GUNPOWDER:2-4"
        - "QUARTZ:1-3"

    Guardian:
      enabled: true
      health-multiplier: 1.5
      damage-multiplier: 1.6
      gradient-name: "<gradient:#00BFFF:#1E90FF>Guardian</gradient>"
      effects:
        - "WATER_BREATHING:0:99999"
        - "SPEED:0:99999"
      custom-loot:
        - "PRISMARINE_SHARD:2-4"
        - "PRISMARINE_CRYSTALS:1-3"
        - "COD:1-2"

    Hoglin:
      enabled: true
      health-multiplier: 1.8
      damage-multiplier: 1.6
      gradient-name: "<gradient:#CD853F:#FF7F50>Hoglin</gradient>"
      effects:
        - "STRENGTH:1:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "PORKCHOP:2-4"
        - "LEATHER:1-2"
        - "BONE:0-2"

    Husk:
      enabled: true
      health-multiplier: 1.7
      damage-multiplier: 1.4
      gradient-name: "<gradient:#DAA520:#F5DEB3>Husk</gradient>"
      effects:
        - "HUNGER:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "ROTTEN_FLESH:2-4"
        - "SAND:1-2"
        - "CACTUS:0-1"

    MagmaCube:
      enabled: true
      health-multiplier: 1.6
      damage-multiplier: 1.3
      gradient-name: "<gradient:#FF4500:#FF8C00>Magma Cube</gradient>"
      effects:
        - "FIRE_RESISTANCE:0:99999"
        - "JUMP:1:99999"
      custom-loot:
        - "MAGMA_CREAM:1-3"
        - "BLAZE_POWDER:0-2"
        - "GOLD_NUGGET:0-2"

    Phantom:
      enabled: true
      health-multiplier: 1.3
      damage-multiplier: 1.4
      gradient-name: "<gradient:#191970:#4169E1>Phantom</gradient>"
      effects:
        - "SPEED:1:99999"
        - "NIGHT_VISION:0:99999"
      custom-loot:
        - "PHANTOM_MEMBRANE:1-2"
        - "FEATHER:1-3"
        - "GUNPOWDER:0-1"

    Piglin:
      enabled: true
      health-multiplier: 1.5
      damage-multiplier: 1.4
      gradient-name: "<gradient:#FFD700:#FFA500>Piglin</gradient>"
      effects:
        - "SPEED:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "GOLD_NUGGET:2-5"
        - "GOLD_INGOT:0-2"
        - "LEATHER:0-2"

    Pillager:
      enabled: true
      health-multiplier: 1.5
      damage-multiplier: 1.4
      gradient-name: "<gradient:#696969:#C0C0C0>Pillager</gradient>"
      effects:
        - "SPEED:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "CROSSBOW:0-1"
        - "ARROW:2-6"
        - "IRON_INGOT:0-1"

    Shulker:
      enabled: true
      health-multiplier: 1.6
      damage-multiplier: 1.4
      gradient-name: "<gradient:#BA55D3:#DA70D6>Shulker</gradient>"
      effects:
        - "LEVITATION:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "SHULKER_SHELL:1-2"
        - "PURPUR_BLOCK:0-2"
        - "ENDER_PEARL:0-1"

    Skeleton:
      enabled: true
      health-multiplier: 1.5
      damage-multiplier: 1.4
      gradient-name: "<gradient:#C0C0C0:#FFFFFF>Skeleton</gradient>"
      effects:
        - "SPEED:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "BONE:2-4"
        - "ARROW:2-4"
        - "BOW:0-1"

    Slime:
      enabled: true
      health-multiplier: 1.5
      damage-multiplier: 1.2
      gradient-name: "<gradient:#32CD32:#7CFC00>Slime</gradient>"
      effects:
        - "JUMP:1:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "SLIME_BALL:2-4"
        - "LILY_PAD:0-2"
        - "GREEN_DYE:0-1"

    Spider:
      enabled: true
      health-multiplier: 1.4
      damage-multiplier: 1.3
      gradient-name: "<gradient:#006400:#8FBC8F>Spider</gradient>"
      effects:
        - "SPEED:1:99999"
        - "JUMP:0:99999"
      custom-loot:
        - "STRING:2-4"
        - "SPIDER_EYE:0-1"
        - "FERMENTED_SPIDER_EYE:0-1"

    Stray:
      enabled: true
      health-multiplier: 1.4
      damage-multiplier: 1.35
      gradient-name: "<gradient:#ADD8E6:#E0FFFF>Stray</gradient>"
      effects:
        - "SLOW:1:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "TIPPED_ARROW:1-3"
        - "SPECTRAL_ARROW:0-2"
        - "BONE:1-2"

    Vex:
      enabled: true
      health-multiplier: 1.2
      damage-multiplier: 1.5
      gradient-name: "<gradient:#87CEEB:#B0E0E6>Vex</gradient>"
      effects:
        - "LEVITATION:0:99999"
        - "SPEED:1:99999"
      custom-loot:
        - "FEATHER:1-2"
        - "GLOWSTONE_DUST:0-2"
        - "REDSTONE:0-1"

    Witch:
      enabled: true
      health-multiplier: 1.4
      damage-multiplier: 1.2
      gradient-name: "<gradient:#4B0082:#9400D3>Witch</gradient>"
      effects:
        - "REGENERATION:0:200"
        - "SPEED:0:99999"
      custom-loot:
        - "POTION:1-2"
        - "GUNPOWDER:1-2"
        - "REDSTONE:1-3"

    WitherSkeleton:
      enabled: true
      health-multiplier: 2.2
      damage-multiplier: 1.8
      gradient-name: "<gradient:#2F4F4F:#696969>Wither Skeleton</gradient>"
      effects:
        - "WITHER:0:100"
        - "FIRE_RESISTANCE:0:99999"
      custom-loot:
        - "WITHER_SKELETON_SKULL:0-1"
        - "COAL:1-3"
        - "BONE:2-4"

    Zoglin:
      enabled: true
      health-multiplier: 2.0
      damage-multiplier: 1.8
      gradient-name: "<gradient:#CD5C5C:#FA8072>Zoglin</gradient>"
      effects:
        - "STRENGTH:1:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "PORKCHOP:2-4"
        - "BONE:1-2"
        - "LEATHER:0-1"

    Zombie:
      enabled: true
      health-multiplier: 1.8
      damage-multiplier: 1.5
      gradient-name: "<gradient:#556B2F:#8FBC8F>Zombie</gradient>"
      effects:
        - "SLOW:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "ROTTEN_FLESH:2-4"
        - "IRON_INGOT:1-2"
        - "CARROT:0-1"

    ZombieVillager:
      enabled: true
      health-multiplier: 1.8
      damage-multiplier: 1.5
      gradient-name: "<gradient:#6B8E23:#9ACD32>Zombie Villager</gradient>"
      effects:
        - "SLOW:0:99999"
        - "RESISTANCE:0:99999"
      custom-loot:
        - "ROTTEN_FLESH:2-4"
        - "EMERALD:0-2"
        - "BOOK:0-1"

    ZombifiedPiglin:
      enabled: true
      health-multiplier: 1.6
      damage-multiplier: 1.4
      gradient-name: "<gradient:#FFD700:#CD853F>Zombified Piglin</gradient>"
      effects:
        - "FIRE_RESISTANCE:0:99999"
        - "STRENGTH:0:99999"
      custom-loot:
        - "GOLD_NUGGET:2-5"
        - "GOLD_INGOT:0-2"
        - "ROTTEN_FLESH:1-3"

# ----------------
# Boss Settings
# ----------------
bosses:
  ENDER_DRAGON:
    enabled: true
    health-multiplier: 4.0
    damage-multiplier: 1.8
    gradient-name: "<gradient:#9400D3:#00FFFF>Abyssal Dragon</gradient>"
    abilities:
      - "FIREBALL:5s"        # Dragon breath replacement
      - "KNOCKBACK:8s"       # Tail swipe
      - "GROUND_POUND:15s"   # Wing smash AoE
    loot:
      - "DRAGON_EGG:1"
      - "ELYTRA:1"
      - "DIAMOND_BLOCK:2-4"
      - "NETHERITE_SCRAP:2-5"

  WARDEN:
    enabled: true
    health-multiplier: 5.0
    damage-multiplier: 2.2
    gradient-name: "<gradient:#191970:#00FF00>Downbad Warden</gradient>"
    abilities:
      - "SONIC_BOOM:8s"      # The iconic ranged blast
      - "CHARGE:6s"          # Rush forward through players
      - "GROUND_POUND:12s"   # Slam quake
    loot:
      - "SCULK_SHARD:2-6"
      - "NETHERITE_SCRAP:1-2"
      - "DIAMOND:3-6"
      - "ANCIENT_DEBRIS:3-6"

  WITHER:
    enabled: true
    health-multiplier: 4.0
    damage-multiplier: 2.0
    gradient-name: "<gradient:#8B0000:#FF0000>Cataclysm Wither</gradient>"
    abilities:
      - "WITHER_SKULL:5s"    # Shoots heads rapidly
      - "EXPLOSION:10s"      # Creates bursts of destruction
      - "SUMMON_MINIONS:20s" # Summons wither skeletons
    loot:
      - "NETHER_STAR:1-2"
      - "DIAMOND:5-10"
      - "ANCIENT_DEBRIS:3-6"
      - "GOLD_BLOCK:2-4"

  ELDER_GUARDIAN:
    enabled: true
    health-multiplier: 3.0
    damage-multiplier: 1.4
    gradient-name: "<gradient:#00CED1:#1E90FF>Elder Sentinel</gradient>"
    abilities:
      - "MINING_FATIGUE:10s" # Classic guardian debuff
      - "FIREBALL:12s"       # Shoots charged orb
      - "GROUND_POUND:18s"   # Water quake AoE
    loot:
      - "PRISMARINE:4-8"
      - "SPONGE:2-4"
      - "GOLD_INGOT:8-16"
      - "PRISMARINE_CRYSTALS:3-6"

  EVOKER:
    enabled: true
    health-multiplier: 2.0
    damage-multiplier: 1.5
    gradient-name: "<gradient:#FF4500:#FFD700>Grand Evoker</gradient>"
    abilities:
      - "SUMMON_VEX:15s"     # Signature vex summon
      - "TOTEM_REVIVE:60s"   # Self-resurrection
      - "FIREBALL:20s"       # Ranged attack, fiery magic theme
    loot:
      - "TOTEM_OF_UNDYING:1"
      - "EMERALD:3-6"
      - "GOLDEN_APPLE:2-6"
      - "BOOK:1"

  PIGLIN_BRUTE:
    enabled: true
    health-multiplier: 2.5
    damage-multiplier: 1.8
    gradient-name: "<gradient:#B22222:#FF8C00>Infernal Brute</gradient>"
    abilities:
      - "CHARGE:10s"         # Brutal rush
      - "KNOCKBACK:15s"      # Axe swing knockback
      - "GROUND_POUND:20s"   # Brutish shockwave
    loot:
      - "GOLD_BLOCK:1-2"
      - "NETHERITE_SCRAP:1-4"
      - "EMERALD:2-4"
      - "IRON_BLOCK:2-4"

  VINDICATOR:
    enabled: true
    health-multiplier: 1.8
    damage-multiplier: 1.6
    gradient-name: "<gradient:#708090:#A9A9A9>Battle Vindicator</gradient>"
    abilities:
      - "CHARGE:12s"         # Axe dash
      - "KNOCKBACK:15s"      # Axe push
      - "FIREBALL:18s"       # Hex-style projectile for uniqueness
    loot:
      - "IRON_AXE:1"
      - "EMERALD:2-5"
      - "IRON_INGOT:4-8"
      - "BOOK:1"

  RAVAGER:
    enabled: true
    health-multiplier: 3.0
    damage-multiplier: 1.8
    gradient-name: "<gradient:#696969:#8B0000>Rampaging Ravager</gradient>"
    abilities:
      - "GROUND_POUND:10s"   # Hoof stomp quake
      - "CHARGE:8s"          # Horn charge
      - "KNOCKBACK:14s"      # Roar pushback
    loot:
      - "SADDLE:1"
      - "EMERALD_BLOCK:1-4"
      - "LEATHER:4-6"
      - "IRON_INGOT:3-5"
```

---

## 🗄 Data file: `data.yml` (structure)

```yaml
# -------------------
# Runtime Data 
# -------------------
data:
  # Track players who are dead (name -> last death time)
  dead: []

  # Track revive timestamps (name -> last revive time)
  reviveTimestamps: {}

  # Global counters
  totalPlayerDeaths: 0
  totalBossKills: 0
  totalMobKills: 0

  # Passive mobs
  passive:
    COW: { kills: 0, lastKilled: null }
    SHEEP: { kills: 0, lastKilled: null }
    PIG: { kills: 0, lastKilled: null }
    CHICKEN: { kills: 0, lastKilled: null }
    HORSE: { kills: 0, lastKilled: null }
    DONKEY: { kills: 0, lastKilled: null }
    MULE: { kills: 0, lastKilled: null }
    RABBIT: { kills: 0, lastKilled: null }
    TURTLE: { kills: 0, lastKilled: null }
    VILLAGER: { kills: 0, lastKilled: null }
    CAT: { kills: 0, lastKilled: null }
    WOLF: { kills: 0, lastKilled: null }
    PARROT: { kills: 0, lastKilled: null }
    BEE: { kills: 0, lastKilled: null }
    AXOLOTL: { kills: 0, lastKilled: null }
    GOAT: { kills: 0, lastKilled: null }
    CAMEL: { kills: 0, lastKilled: null }
    LLAMA: { kills: 0, lastKilled: null }
    PANDA: { kills: 0, lastKilled: null }
    FOX: { kills: 0, lastKilled: null }
    SNIFFER: { kills: 0, lastKilled: null }

  # Hostile mobs
  hostile:
    ZOMBIE: { kills: 0, lastKilled: null }
    SKELETON: { kills: 0, lastKilled: null }
    CREEPER: { kills: 0, lastKilled: null }
    SPIDER: { kills: 0, lastKilled: null }
    CAVE_SPIDER: { kills: 0, lastKilled: null }
    SLIME: { kills: 0, lastKilled: null }
    MAGMA_CUBE: { kills: 0, lastKilled: null }
    DROWNED: { kills: 0, lastKilled: null }
    HUSK: { kills: 0, lastKilled: null }
    STRAY: { kills: 0, lastKilled: null }
    WITCH: { kills: 0, lastKilled: null }
    GUARDIAN: { kills: 0, lastKilled: null }
    BLAZE: { kills: 0, lastKilled: null }
    GHAST: { kills: 0, lastKilled: null }
    PHANTOM: { kills: 0, lastKilled: null }
    ENDERMAN: { kills: 0, lastKilled: null }
    PILLAGER: { kills: 0, lastKilled: null }
    VEX: { kills: 0, lastKilled: null }
    SILVERFISH: { kills: 0, lastKilled: null }
    SHULKER: { kills: 0, lastKilled: null }
    PIGLIN: { kills: 0, lastKilled: null }
    HOGLIN: { kills: 0, lastKilled: null }
    ZOGLIN: { kills: 0, lastKilled: null }
    STRIDER: { kills: 0, lastKilled: null }
    ZOMBIFIED_PIGLIN: { kills: 0, lastKilled: null }
    RAVAGER: { kills: 0, lastKilled: null }
    VINDICATOR: { kills: 0, lastKilled: null }
    EVOKER: { kills: 0, lastKilled: null }
    PIGLIN_BRUTE: { kills: 0, lastKilled: null }

  # Bosses / Mini-bosses
  bosses:
    ENDER_DRAGON: { kills: 0, lastKilled: null }
    WITHER: { kills: 0, lastKilled: null }
    WARDEN: { kills: 0, lastKilled: null }
    ELDER_GUARDIAN: { kills: 0, lastKilled: null }
    RAVAGER: { kills: 0, lastKilled: null }
    VINDICATOR: { kills: 0, lastKilled: null }
    EVOKER: { kills: 0, lastKilled: null }
    PIGLIN_BRUTE: { kills: 0, lastKilled: null }
```

---

## 🧠 Developer Notes

- `src/main/java/com/astroolean/deathwish/DeathWish.java` — plugin entrypoint: registers events, commands, and handles core logic.
- `HardcoreUtil.java` — utilities for multipliers, death tracking, and revive handling.
- `GradientUtil.java` — helpers to render gradient text and chat visuals.

Review the Java sources for exact method-level behavior and hooks.

---

## ✅ Usage Examples

- `/revive <player>` — revive a dead player (may require player head depending on config).
- `/deadlist` — list dead players.
- `/gg <player>` — admin force-revive.
- `/dwreload` — reload config and data.

---

## 🛠 Troubleshooting

- Commands missing: verify `plugin.yml` is packaged at the root of the JAR.
- Permissions failing: assign nodes via your permissions plugin.
- Data not saving: ensure plugin folder has write permissions.

---

## 🧾 License & Contributing

- See `LICENSE.txt` for license details.
- To contribute: fork → change → repeat. Run `mvn clean package` for the JAR.
