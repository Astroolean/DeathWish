package com.astroolean.deathwish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.*;
import java.util.logging.Level;

/**
 * DeathWish.java
 * Single-file modern plugin for Paper/Spigot 1.21.8+
 *
 * - persistent runtime state in data.yml (dead, reviveOnJoin, reviveTimestamps)
 * - safe revive (find safe ground, slow-falling)
 * - strict head validation (PDC bound_to == UUID)
 * - recipe shell with safe registration
 * - mob override system and simple boss ability scheduling
 * - custom loot parsing
 * - Adventure components for players (console receives legacy)
 *
 * Additions:
 * - bStats integration (reflection; optional)
 * - basic async update checker (non-blocking, logs only)
 * - critical-state logging (deaths, revives, data save/load)
 */
@SuppressWarnings("unused")
public final class DeathWish extends JavaPlugin implements Listener, TabCompleter {
    private NamespacedKey KEY_BOUND_TO;
    private NamespacedKey KEY_RANDOM_HEAD_RECIPE = new NamespacedKey("deathwish", "revive_head_random");;
    private final Map<UUID, NamespacedKey> headRecipes = new HashMap<>();

    // runtime state (persisted)
    private final Set<UUID> dead = ConcurrentHashMap.newKeySet();
    private final Set<UUID> reviveOnJoin = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> reviveTimestamps = new ConcurrentHashMap<>();

    // data.yml
    private File dataFile;
    private FileConfiguration dataConfig;

    // config message templates
    private String prefixRaw;
    private String reviveBroadcastRaw;
    private String notDeadMsgRaw;
    private String needHeadMsgRaw;
    private String nobodyDeadMsgRaw;

    // track boss scheduled tasks (task id)
    private final Map<UUID, Integer> bossTaskIds = new ConcurrentHashMap<>();

    // bStats default id: replace with your plugin's bStats id if you have one
    private final int BSTATS_ID = 27272; // <-- replace with your bStats plugin id (optional)

    // ---------------- Scaling State ----------------
    private int totalPlayerDeaths = 0;
    private int totalBossKills = 0;

    // ---------------- enabling DeathWish ----------------
    @Override
    public void onEnable() {
        // Prints the banner for console
        printBanner();

        saveDefaultConfig();
        loadConfigValues();

        KEY_BOUND_TO = new NamespacedKey(this, "bound_to");
        KEY_RANDOM_HEAD_RECIPE = new NamespacedKey(this, "random_dead_head");

        setupDataFile();
        loadRuntimeData();

        // re-register recipes for still-dead players
        for (UUID u : dead) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            registerHeadRecipe(u, op.getName() == null ? "Player" : op.getName());
        }

        getServer().getPluginManager().registerEvents(this, this);

        // assign tab completers
        PluginCommand reviveCmd = getCommand("revive");
        if (reviveCmd != null) reviveCmd.setTabCompleter(this);

        PluginCommand forceReviveCmd = getCommand("forcerevive");
        if (forceReviveCmd != null) forceReviveCmd.setTabCompleter(this);

        PluginCommand ggCmd = getCommand("gg");
        if (ggCmd != null) ggCmd.setTabCompleter(this);

        // apply difficulty now and schedule reapply
        applyDifficultyAll();
        long period = getConfig().getLong("general.difficulty-reapply-ticks", 6000L);
        new BukkitRunnable() {
            @Override
            public void run() {
                applyDifficultyAll();
            }
        }.runTaskTimer(this, period, period);

        registerRecipeShell();

        getLogger().log(Level.INFO, "DeathWish enabled. Dead players tracked: {0}", dead.size());

        // Enable bStats optionally (reflection so plugin compiles without bStats jar)
        enableBStatsIfAvailable();

        // Simple async update check (non-blocking, safe)
        checkForUpdatesAsync();
    }

    @Override
    public void onDisable() {
        saveRuntimeData();
        bossTaskIds.values().forEach(Bukkit.getScheduler()::cancelTask);
        bossTaskIds.clear();
    }

    // ---------------- config & data ----------------

    private void loadConfigValues() {
        prefixRaw = getConfig().getString("general.prefix", "<gradient:#00FF00:#FFFFFF>[DeathWish] </gradient>");
        reviveBroadcastRaw = getConfig().getString("messages.revive-broadcast",
                "<gradient:#00FF00:#FFFFFF>[DeathWish] Player</gradient> <gradient:#FF0000:#FFFFFF>{player}</gradient> <gradient:#00FF00:#FFFFFF>has been REVIVED!</gradient>");
        notDeadMsgRaw = getConfig().getString("messages.not-dead", "<gradient:#FF0000:#FFFFFF>That player is not dead.</gradient>");
        needHeadMsgRaw = getConfig().getString("messages.need-head", "<gradient:#FF0000:#FFFFFF>Hold {player}'s head</gradient> <gradient:#00FF00:#FFFFFF>in your main hand to revive them.</gradient>");
        nobodyDeadMsgRaw = getConfig().getString("messages.nobody-dead", "<gradient:#FF0000:#FFFFFF>Nobody is dead yet.</gradient>");
    }


    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                YamlConfiguration cfg = new YamlConfiguration();
                cfg.set("dead", new ArrayList<String>());
                cfg.set("reviveOnJoin", new ArrayList<String>());
                cfg.set("reviveTimestamps", new ArrayList<String>());
                cfg.save(dataFile);
                getLogger().info("Created new data.yml");
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Failed to create data.yml: {0}", ex.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadRuntimeData() {
        try {
            if (dataConfig == null) dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            List<String> dl = dataConfig.getStringList("dead");
            for (String s : dl) {
                try { dead.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
            List<String> roj = dataConfig.getStringList("reviveOnJoin");
            for (String s : roj) {
                try { reviveOnJoin.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
            ConfigurationSection ts = dataConfig.getConfigurationSection("reviveTimestamps");
            if (ts != null) {
                for (String key : ts.getKeys(false)) {
                    try {
                        UUID u = UUID.fromString(key);
                        long v = ts.getLong(key, 0L);
                        reviveTimestamps.put(u, v);
                    } catch (Exception ignored) {}
                }
            }
            getLogger().log(Level.INFO, "Loaded runtime data: {0} dead, {1} revive-on-join entries.", new Object[]{dead.size(), reviveOnJoin.size()});
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to load runtime data: {0}", ex.getMessage());
        }
    }

    private synchronized void saveRuntimeData() {
        try {
            if (dataConfig == null) dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            dataConfig.set("dead", dead.stream().map(UUID::toString).collect(Collectors.toList()));
            dataConfig.set("reviveOnJoin", reviveOnJoin.stream().map(UUID::toString).collect(Collectors.toList()));
            dataConfig.set("reviveTimestamps", null);
            for (Map.Entry<UUID, Long> e : reviveTimestamps.entrySet()) {
                dataConfig.set("reviveTimestamps." + e.getKey().toString(), e.getValue());
            }
            dataConfig.save(dataFile);
            getLogger().log(Level.INFO, "Saved runtime data to data.yml ({0} dead, {1} revive-on-join).", new Object[]{dead.size(), reviveOnJoin.size()});
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Failed saving data.yml: {0}", ex.getMessage());
        }
    }

    // ---------------- messaging helper ----------------

    private Component componentFromRaw(String raw) {
        if (raw == null) raw = "";
        String withSections = GradientUtil.applyGradients(raw);
        return LegacyComponentSerializer.legacySection().deserialize(withSections);
    }
    private void sendToSender(CommandSender sender, Component comp) {
        if (sender == null) return; // defensive guard

        if (sender instanceof Player player) {
            player.sendMessage(comp);
        } else {
            sender.sendMessage(
                LegacyComponentSerializer.legacySection().serialize(comp)
            );
        }
    }

    // ---------------- difficulty enforcement ----------------
    private void applyDifficultyAll() {
        FileConfiguration config = getConfig();
        if (config == null) {
            getLogger().warning("Config missing; skipping difficulty apply.");
            return;
        }

        ConfigurationSection diffSec = config.getConfigurationSection("difficulty");
        if (diffSec == null || !diffSec.getBoolean("enabled", false)) {
            // Difficulty disabled or not present
            return;
        }

        // Load base defaults safely
        double baseHealth = 1.0;
        double baseDamage = 1.0;
        ConfigurationSection defaults = diffSec.getConfigurationSection("defaults");
        if (defaults != null) {
            baseHealth = defaults.getDouble("health-multiplier", baseHealth);
            baseDamage = defaults.getDouble("damage-multiplier", baseDamage);
        }

        double healthMult = baseHealth;
        double damageMult = baseDamage;

        StringBuilder breakdown = new StringBuilder("Scaling breakdown → ");
        breakdown.append(String.format("base=%.2f", baseHealth));

        // Scaling section
        ConfigurationSection scaling = diffSec.getConfigurationSection("scaling");
        if (scaling != null) {
            // Time-based
            ConfigurationSection timeSec = scaling.getConfigurationSection("time-based");
            if (timeSec != null && timeSec.getBoolean("enabled", false)) {
                // Guard against empty worlds list
                long days = 0;
                List<World> worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    // Use first loaded world as the primary time source
                    World w0 = worlds.get(0);
                    days = Math.max(0L, w0.getFullTime() / 24000L);
                }
                double perDay = timeSec.getDouble("multiplier-per-day", 0.0);
                double add = days * perDay;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", days=%d(+%.2f)", days, add));
            }

            // Death-based
            ConfigurationSection deathSec = scaling.getConfigurationSection("death-based");
            if (deathSec != null && deathSec.getBoolean("enabled", false)) {
                double perDeath = deathSec.getDouble("multiplier-per-death", 0.0);
                double add = totalPlayerDeaths * perDeath;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", deaths=%d(+%.2f)", totalPlayerDeaths, add));
            }

            // Boss-kill
            ConfigurationSection bossSec = scaling.getConfigurationSection("boss-kill");
            if (bossSec != null && bossSec.getBoolean("enabled", false)) {
                double perBoss = bossSec.getDouble("multiplier-per-boss", 0.0);
                double add = totalBossKills * perBoss;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", bosses=%d(+%.2f)", totalBossKills, add));
            }
        }

        // Safety clamps — avoid absurd multipliers (optional, tweakable)
        // healthMult = Math.min(healthMult, 10.0);
        // damageMult = Math.min(damageMult, 10.0);

        // Apply multipliers to each living non-player entity
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            getLogger().warning("No worlds loaded; skipping difficulty application.");
        } else {
            for (World w : worlds) {
                if (w == null) continue;
                for (LivingEntity ent : w.getLivingEntities()) {
                    if (ent == null) continue;
                    if (ent instanceof Player) continue;

                    // MAX HEALTH
                    AttributeInstance healthAttr = ent.getAttribute(Attribute.MAX_HEALTH);
                    if (healthAttr != null) {
                        double base = healthAttr.getBaseValue();
                        double newMaxHealth = base * healthMult;
                        try {
                            healthAttr.setBaseValue(newMaxHealth);
                            // keep current health at or below newMax
                            double currentHp = ent.getHealth();
                            ent.setHealth(Math.min(currentHp, newMaxHealth));
                        } catch (Exception ex) {
                            // Defensive: some entities can throw on setHealth or setBaseValue in odd implementations
                            getLogger().log(Level.FINER, "Failed to set health for {0} ({1}): {2}", new Object[]{ent.getType(), ent.getUniqueId(), ex.getMessage()});
                        }
                    }

                    // ATTACK DAMAGE
                    AttributeInstance dmgAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
                    if (dmgAttr != null) {
                        double base = dmgAttr.getBaseValue();
                        double newDamage = base * damageMult;
                        try {
                            dmgAttr.setBaseValue(newDamage);
                        } catch (Exception ex) {
                            getLogger().log(Level.FINER, "Failed to set damage for {0} ({1}): {2}", new Object[]{ent.getType(), ent.getUniqueId(), ex.getMessage()});
                        }
                    }
                }
            }
        }

        // Apply global potion effects to living non-player entities
        List<String> effects = diffSec.getStringList("global-effects");
        if (!effects.isEmpty()) {
            for (World w : worlds) {
                if (w == null) continue;
                for (LivingEntity ent : w.getLivingEntities()) {
                    if (ent == null) continue;
                    if (ent instanceof Player) continue;

                    for (String eff : effects) {
                        if (eff == null || eff.isBlank()) continue;
                        try {
                            String[] parts = eff.split(":");
                            String type = parts[0].trim();
                            int amp = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                            int dur = parts.length > 2 ? Integer.parseInt(parts[2]) : 999999;

                            PotionEffectType pet = Registry.EFFECT.get(NamespacedKey.minecraft(type.toLowerCase(Locale.ROOT)));
                            if (pet != null) {
                                try {
                                    ent.addPotionEffect(new PotionEffect(pet, dur, amp, true, false, true));
                                } catch (Exception ex) {
                                    // Add effect can fail for some entities — log at debug level
                                    getLogger().log(Level.FINER, "Failed to add potion effect {0} to {1}: {2}", new Object[]{type, ent.getType(), ex.getMessage()});
                                }
                            } else {
                                getLogger().log(Level.FINER, "Unknown potion effect in config: {0}", type);
                            }
                        } catch (NumberFormatException nfe) {
                            getLogger().log(Level.FINER, "Invalid effect numbers in config entry: {0}", eff);
                        } catch (Exception ex) {
                            getLogger().log(Level.FINER, "Unexpected error while applying effect {0}: {1}", new Object[]{eff, ex.getMessage()});
                        }
                    }
                }
            }
        }

        getLogger().log(Level.INFO, "{0}{1}", new Object[]{breakdown.toString(), String.format(" → final health x%.2f, damage x%.2f", healthMult, damageMult)});
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent e) { e.getWorld().setDifficulty(Difficulty.HARD); }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) { e.getWorld().setDifficulty(Difficulty.HARD); }

    // ---------------- death & head ----------------

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isWorldEnabled(p.getWorld().getName())) return;

        // increment scaling counter
        totalPlayerDeaths++;
        getLogger().log(Level.INFO, "Total player deaths: {0}", totalPlayerDeaths);

        UUID id = p.getUniqueId();
        dead.add(id);
        saveRuntimeData();
        getLogger().log(Level.INFO, "Marked player as dead: {0} ({1})", new Object[]{p.getName(), id});

        // drop the head
        ItemStack head = makeBoundHead(id, p.getName());
        e.getDrops().add(head);

        // register recipe for crafting more heads (only while dead)
        registerHeadRecipe(id, p.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                try { p.spigot().respawn(); } catch (Throwable ignored) {}
                p.setGameMode(GameMode.SPECTATOR);
            }
        }.runTask(this);
    }

    private ItemStack makeBoundHead(UUID who, String name) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(who));
        String playerName = (name == null ? "Player" : name);

        // --- display name ---
        String headTemplate = getConfig().getString("messages.head-name", "");
        Component displayComp;
        if (headTemplate != null && !headTemplate.trim().isEmpty()) {
            // admin-defined override
            String parsed = headTemplate.replace("{player}", playerName);
            displayComp = componentFromRaw(parsed);
        } else {
            // fallback gradient default
            displayComp = MiniMessage.miniMessage().deserialize(
                "<gradient:#00FF00:#FFFFFF>Revival Token: </gradient><gradient:#FF0000:#FFFFFF>" 
                + playerName + "</gradient>'s Head"
            );
        }
        meta.displayName(displayComp);

        // --- lore ---
        List<String> loreRaw = getConfig().getStringList("messages.head-lore");
        List<Component> loreComponents = new ArrayList<>();
        if (!loreRaw.isEmpty()) {
            for (String line : loreRaw) {
                loreComponents.add(componentFromRaw(line.replace("{player}", playerName)));
            }
        } else {
            // fallback gradient lore
            loreComponents.add(MiniMessage.miniMessage().deserialize(
                "<gradient:#00FF00:#FFFFFF>Use this head to revive </gradient>"
                + "<gradient:#FF0000:#FFFFFF>" + playerName + "</gradient>"
            ));
        }
        meta.lore(loreComponents);

        // tag item with bound UUID
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_BOUND_TO, PersistentDataType.STRING, who.toString());

        item.setItemMeta(meta);
        return item;
    }

    // ---------------- Revive Head Recipe Shell ----------------

    private void registerRecipeShell() {
        // avoid duplicate registration
        try {
            Iterator<Recipe> it = Bukkit.recipeIterator();
            while (it.hasNext()) {
                Recipe r = it.next();
                if (r instanceof Keyed keyed) {
                    NamespacedKey k = keyed.getKey();
                    if (KEY_RANDOM_HEAD_RECIPE.equals(k)) {
                        getLogger().info("Head recipe already registered, skipping.");
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}

        // default shape if config not defined
        List<String> shape = getConfig().getStringList("revive.recipe.shape");
        if (shape.isEmpty()) shape = Arrays.asList("DDD", "DND", "DDD");

        // default keys if config not defined
        Map<String, String> keys = new HashMap<>();
        if (getConfig().isConfigurationSection("revive.recipe.keys")) {
            ConfigurationSection cs = getConfig().getConfigurationSection("revive.recipe.keys");
            for (String k : Objects.requireNonNull(cs).getKeys(false)) {
                keys.put(k, cs.getString(k, ""));
            }
        } else {
            keys.put("D", "DIAMOND");
            keys.put("N", "NETHER_STAR");
        }

        // dummy item (will be replaced later in PrepareItemCraft)
        ItemStack dummy = new ItemStack(Material.BARRIER);
        ShapedRecipe sr = new ShapedRecipe(KEY_RANDOM_HEAD_RECIPE, dummy);
        sr.shape(shape.toArray(String[]::new));

        for (Map.Entry<String, String> e : keys.entrySet()) {
            String k = e.getKey();
            Material m = Material.matchMaterial(e.getValue());
            if (m != null && k != null && !k.isEmpty()) {
                sr.setIngredient(k.charAt(0), new MaterialChoice(m));
            }
        }

        try {
            Bukkit.addRecipe(sr);
            getLogger().log(Level.INFO, "Registered revive head recipe ({0}).", KEY_RANDOM_HEAD_RECIPE);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to register recipe: {0}", ex.getMessage());
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe r = e.getRecipe();
        if (r == null) return;
        if (r instanceof Keyed keyed) {
            NamespacedKey rk = keyed.getKey();
            if (KEY_RANDOM_HEAD_RECIPE.equals(rk)) {
                if (dead.isEmpty()) {
                    e.getInventory().setResult(null);
                } else {
                    // pick a random dead player
                    List<UUID> ids = new ArrayList<>(dead);
                    UUID pick = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
                    OfflinePlayer off = Bukkit.getOfflinePlayer(pick);

                    // build the bound head
                    ItemStack result = makeBoundHead(pick, off.getName() == null ? "Player" : off.getName());
                    e.getInventory().setResult(result);
                }
            }
        }
    }

    private void registerHeadRecipe(UUID uuid, String playerName) {
        NamespacedKey key = new NamespacedKey(this, "revive_head_" + uuid.toString().substring(0, 8));
        if (headRecipes.containsKey(uuid)) return; // already registered

        // default shape if config not defined
        List<String> shape = getConfig().getStringList("revive.recipe.shape");
        if (shape.isEmpty()) shape = Arrays.asList("DDD", "DND", "DDD");

        Map<String, String> keys = new HashMap<>();
        if (getConfig().isConfigurationSection("revive.recipe.keys")) {
            ConfigurationSection cs = getConfig().getConfigurationSection("revive.recipe.keys");
            for (String k : Objects.requireNonNull(cs).getKeys(false)) {
                keys.put(k, cs.getString(k, ""));
            }
        } else {
            keys.put("D", "DIAMOND");
            keys.put("N", "NETHER_STAR");
        }

        ItemStack result = makeBoundHead(uuid, playerName);
        ShapedRecipe sr = new ShapedRecipe(key, result);
        sr.shape(shape.toArray(String[]::new));

        for (Map.Entry<String, String> e : keys.entrySet()) {
            String k = e.getKey();
            Material m = Material.matchMaterial(e.getValue());
            if (m != null && !k.isEmpty()) {
                sr.setIngredient(k.charAt(0), new MaterialChoice(m));
            }
        }

        try {
            Bukkit.addRecipe(sr);
            headRecipes.put(uuid, key);
            getLogger().log(Level.INFO, "Registered revive head recipe for {0}", playerName);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to register recipe for {0}: {1}", new Object[]{playerName, ex.getMessage()});
        }
    }

    private void unregisterHeadRecipe(UUID uuid) {
        NamespacedKey key = headRecipes.remove(uuid);
        if (key != null) {
            Bukkit.removeRecipe(key);
            getLogger().log(Level.INFO, "Unregistered revive head recipe ({0})", key);
        }
    }

    // ---------------- commands ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "revive" -> handleRevive(sender, label, args);
            case "deadlist" -> handleDeadlist(sender);
            case "gg" -> handleForceRevive(sender, label, args);
            case "dwreload" -> handleReload(sender);
            case "deathwish", "dw" -> handleMain(sender, args);
            default -> false;
        };
    }

    private boolean handleForceRevive(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("deathwish.gg") && !sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>You do not have permission.</gradient>"));
            return true;
        }
        if (args.length != 1) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>Usage: </gradient><gradient:#FF0000:#FFFFFF>/" + label + " <player></gradient>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID targetUUID = target.getUniqueId();
        if (!dead.contains(targetUUID)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " " + notDeadMsgRaw));
            return true;
        }

        // instantly revive (ignore cooldown, head checks, etc.)
        dead.remove(targetUUID);
        reviveTimestamps.put(targetUUID, System.currentTimeMillis());
        unregisterHeadRecipe(targetUUID);
        saveRuntimeData();
        getLogger().log(Level.INFO, "Force revived: {0} by {1}", new Object[]{args[0], sender.getName()});

        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null) {
            reviveOnJoin.remove(targetUUID);
            online.setGameMode(GameMode.SURVIVAL);

            AttributeInstance inst = online.getAttribute(Attribute.MAX_HEALTH);
            double max = inst != null ? inst.getValue() : 20.0;
            online.setHealth(Math.max(1.0, max));

            Location safe = findSafeLocation(online.getLocation());
            if (safe != null) {
                online.teleport(safe);
                online.setFallDistance(0f);
                online.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false, true));
            }

            online.sendMessage(componentFromRaw(prefixRaw + " " + reviveBroadcastRaw.replace("{player}", online.getName())));
        } else {
            reviveOnJoin.remove(targetUUID);
            reviveOnJoin.add(targetUUID);
            saveRuntimeData();
        }

        boolean broadcast = getConfig().getBoolean("general.broadcast-revive", true);
        if (broadcast) {
            Component bc = componentFromRaw(reviveBroadcastRaw.replace("{player}", target.getName()));
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(bc);
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().serialize(bc));
        }

        sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>Force revived:</gradient> <gradient:#FF0000:#FFFFFF>" + target.getName() + "</gradient>"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("deathwish.reload") && !sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>You do not have permission.</gradient>"));
            return true;
        }

        saveRuntimeData();
        reloadConfig();
        loadConfigValues();
        registerRecipeShell();

        sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>Config & data reloaded.</gradient>"));
        getLogger().log(Level.INFO, "Config & data reloaded by {0}", sender.getName());
        return true;
    }

    private boolean handleDeadlist(CommandSender sender) {
        // permission check
        if (!sender.hasPermission("deathwish.deadlist") && !sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>You do not have permission.</gradient>"));
            return true;
        }

        // header
        sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#00FFFF>Dead Players</gradient>"));

        // empty
        if (dead.isEmpty()) {
            sendToSender(sender, componentFromRaw(prefixRaw + " " + nobodyDeadMsgRaw));
            return true;
        }

        // list each dead player
        for (UUID u : dead) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            String disp = op.getName() == null ? u.toString() : op.getName();
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>- " + disp + "</gradient>"));
        }

        return true;
    }

    private boolean handleRevive(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("deathwish.revive") && !sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>You do not have permission.</gradient>"));
            return true;
        }
        if (args.length != 1) {
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>Usage: /" + label + " <player></gradient>"));
            return true;
        }
        if (dead.isEmpty()) {
            sendToSender(sender, componentFromRaw(prefixRaw + " " + nobodyDeadMsgRaw));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID targetUUID = target.getUniqueId();
        if (!dead.contains(targetUUID)) {
            sendToSender(sender, componentFromRaw(prefixRaw + " " + notDeadMsgRaw));
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = getConfig().getLong("revive.cooldown-seconds", 86400L) * 1000L;
        Long last = reviveTimestamps.get(targetUUID);
        if (last != null && now - last < cooldownMs) {
            long remain = (cooldownMs - (now - last)) / 1000L;
            sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>That player is still on revive cooldown</gradient> <gradient:#FF0000:#FFFFFF>(" + remain + "s).</gradient>"));
            return true;
        }

        // if sender is player, require bound head for that target
        if (sender instanceof Player p) {
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR || !inHand.hasItemMeta()) {
                sendToSender(sender, componentFromRaw(prefixRaw + " " + needHeadMsgRaw.replace("{player}", target.getName())));
                return true;
            }
            PersistentDataContainer pdc = inHand.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(KEY_BOUND_TO, PersistentDataType.STRING)) {
                sendToSender(sender, componentFromRaw(prefixRaw + " " + needHeadMsgRaw.replace("{player}", target.getName())));
                return true;
            }
            String bound = pdc.get(KEY_BOUND_TO, PersistentDataType.STRING);
            if (bound == null || !bound.equals(targetUUID.toString())) {
                sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#FF0000:#FFFFFF>That head is not bound to that player.</gradient>"));
                return true;
            }
            if (p.getGameMode() != GameMode.CREATIVE) {
                int amt = inHand.getAmount();
                if (amt > 1) inHand.setAmount(amt - 1);
                else p.getInventory().setItemInMainHand(null);
            }
        }

        // perform revive
        dead.remove(targetUUID);
        reviveTimestamps.put(targetUUID, now);
        unregisterHeadRecipe(targetUUID);
        saveRuntimeData();
        getLogger().log(Level.INFO, "{0} revived {1} (head).", new Object[]{sender.getName(), args[0]});

        Player online = Bukkit.getPlayer(targetUUID);
        if (online != null) {
            reviveOnJoin.remove(targetUUID);
            online.setGameMode(GameMode.SURVIVAL);

            AttributeInstance inst = online.getAttribute(Attribute.MAX_HEALTH);
            double max = inst != null ? inst.getValue() : 20.0;
            online.setHealth(Math.max(1.0, max));

            Location safe = findSafeLocation(online.getLocation());
            if (safe != null) {
                online.teleport(safe);
                online.setFallDistance(0f);
                online.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false, true));
            }

            online.sendMessage(componentFromRaw(prefixRaw + " " + reviveBroadcastRaw.replace("{player}", online.getName())));
        } else {
            reviveOnJoin.remove(targetUUID);
            reviveOnJoin.add(targetUUID);
            saveRuntimeData();
        }

        boolean broadcast = getConfig().getBoolean("general.broadcast-revive", true);
        if (broadcast) {
            Component bc = componentFromRaw(reviveBroadcastRaw.replace("{player}", target.getName()));
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(bc);
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().serialize(bc));
        }

        sendToSender(Bukkit.getConsoleSender(), componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>Player revived:</gradient> <gradient:#FF0000:#FFFFFF>" + target.getName() + "</gradient>"));
        return true;
    }

    private boolean handleMain(CommandSender sender, String[] args) {
        // reload subcommand
        if (args != null && args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            return handleReload(sender);
        }

        // basic plugin info (no args)
        sendToSender(sender, componentFromRaw(prefixRaw + " <gradient:#00FF00:#FFFFFF>DeathWish </gradient><gradient:#FF0000:#FFFFFF>version " + getPluginMeta().getVersion() + "</gradient>"));
        sendToSender(sender, componentFromRaw("<gradient:#00FF00:#00FFFF>Commands:</gradient> <gradient:#FF0000:#FFFFFF>/revive /deadlist /gg /dwreload</gradient>"));
        return true;
    }

    // ---------------- safe revive ----------------

    private Location findSafeLocation(Location loc) {
        if (loc == null) return null;
        World world = loc.getWorld();
        if (world == null) return null;

        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int highest = world.getHighestBlockYAt(x, z);
        if (highest <= 0) {
            Location spawn = world.getSpawnLocation().clone();
            spawn.setY(spawn.getY() + 1);
            return spawn;
        }
        Location candidate = new Location(world, x + 0.5, highest + 1.0, z + 0.5);
        if (isSafeGround(candidate)) return candidate;

        Location search = loc.clone();
        for (int i = 0; i < 256; i++) {
            if (search.getY() <= 1) break;
            search.setY(search.getY() - 1);
            if (isSafeGround(search)) {
                Location out = search.clone();
                out.setY(search.getY() + 1.0);
                return out;
            }
        }
        Location spawn = world.getSpawnLocation().clone();
        spawn.setY(spawn.getY() + 1);
        return spawn;
    }

    private boolean isSafeGround(Location loc) {
        if (loc == null) return false;
        World w = loc.getWorld();
        if (w == null) return false;
        org.bukkit.block.Block below = w.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        org.bukkit.block.Block at = w.getBlockAt(loc);
        org.bukkit.block.Block above = w.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());

        boolean belowSolid = below.getType().isSolid();
        boolean atPassable = !at.getType().isSolid();
        boolean abovePassable = !above.getType().isSolid();
        boolean notLiquid = !below.isLiquid() && !at.isLiquid() && !above.isLiquid();
        return belowSolid && atPassable && abovePassable && notLiquid;
    }

    // Track boss kills
    @EventHandler
    public void onBossDeath(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        FileConfiguration config = getConfig();
        ConfigurationSection bossesSec = config.getConfigurationSection("bosses");
        if (bossesSec != null && bossesSec.isConfigurationSection(type.name())) {
            totalBossKills++;
        }
    }

    // ---------------- helper result ----------------
    private static class MultiplierResult {
        final double healthMult;
        final double damageMult;
        final String breakdown;
        MultiplierResult(double h, double d, String b) { healthMult = h; damageMult = d; breakdown = b; }
    }

    // ---------------- compute multipliers (from config + runtime state) ----------------
    private MultiplierResult computeMultipliers() {
        FileConfiguration config = getConfig();
        if (config == null) {
            return new MultiplierResult(1.0, 1.0, "config-missing");
        }

        ConfigurationSection diffSec = config.getConfigurationSection("difficulty");
        if (diffSec == null || !diffSec.getBoolean("enabled", false)) {
            return new MultiplierResult(1.0, 1.0, "disabled");
        }

        double baseHealth = 1.0;
        double baseDamage = 1.0;
        ConfigurationSection defaults = diffSec.getConfigurationSection("defaults");
        if (defaults != null) {
            baseHealth = defaults.getDouble("health-multiplier", baseHealth);
            baseDamage = defaults.getDouble("damage-multiplier", baseDamage);
        }

        double healthMult = baseHealth;
        double damageMult = baseDamage;

        StringBuilder breakdown = new StringBuilder();
        breakdown.append(String.format("base=%.2f", baseHealth));

        ConfigurationSection scaling = diffSec.getConfigurationSection("scaling");
        if (scaling != null) {
            // time-based
            ConfigurationSection timeSec = scaling.getConfigurationSection("time-based");
            if (timeSec != null && timeSec.getBoolean("enabled", false)) {
                long days = 0;
                List<World> worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    World w0 = worlds.get(0);
                    if (w0 != null) days = Math.max(0L, w0.getFullTime() / 24000L);
                }
                double perDay = timeSec.getDouble("multiplier-per-day", 0.0);
                double add = days * perDay;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", days=%d(+%.2f)", days, add));
            }

            // death-based
            ConfigurationSection deathSec = scaling.getConfigurationSection("death-based");
            if (deathSec != null && deathSec.getBoolean("enabled", false)) {
                double perDeath = deathSec.getDouble("multiplier-per-death", 0.0);
                double add = totalPlayerDeaths * perDeath;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", deaths=%d(+%.2f)", totalPlayerDeaths, add));
            }

            // boss-kill
            ConfigurationSection bossSec = scaling.getConfigurationSection("boss-kill");
            if (bossSec != null && bossSec.getBoolean("enabled", false)) {
                double perBoss = bossSec.getDouble("multiplier-per-boss", 0.0);
                double add = totalBossKills * perBoss;
                healthMult += add;
                damageMult += add;
                breakdown.append(String.format(", bosses=%d(+%.2f)", totalBossKills, add));
            }
        }

        // apply clamps (if set)
        double maxHealth = diffSec.getDouble("max-multiplier-health", 0.0);
        double maxDamage = diffSec.getDouble("max-multiplier-damage", 0.0);
        if (maxHealth > 0 && healthMult > maxHealth) {
            breakdown.append(String.format(", clampedHealth(%.2f->%.2f)", healthMult, maxHealth));
            healthMult = maxHealth;
        }
        if (maxDamage > 0 && damageMult > maxDamage) {
            breakdown.append(String.format(", clampedDamage(%.2f->%.2f)", damageMult, maxDamage));
            damageMult = maxDamage;
        }

        String finalBreak = breakdown.toString();
        finalBreak += String.format(" -> final h=%.2f d=%.2f", healthMult, damageMult);
        return new MultiplierResult(healthMult, damageMult, finalBreak);
    }

    @EventHandler
        public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!isWorldEnabled(e.getEntity().getWorld().getName())) return;

        if (!(e.getEntity() instanceof LivingEntity ent)) {
            return;
        }

        String typeKey = ent.getType().name();

        FileConfiguration config = getConfig();
        if (config == null) return;

        ConfigurationSection mobsSec = config.getConfigurationSection("mobs.overrides");
        ConfigurationSection bossesSec = config.getConfigurationSection("bosses");
        ConfigurationSection diffSec = config.getConfigurationSection("difficulty");

        // Apply per-mob overrides first (names, multipliers, effects, bosses scheduling)
        if (mobsSec != null && mobsSec.isConfigurationSection(typeKey)) {
            ConfigurationSection conf = mobsSec.getConfigurationSection(typeKey);
            if (conf != null && conf.getBoolean("enabled", true)) {
                // apply per-mob health/damage overrides (these are multipliers relative to entity base)
                double hm = conf.getDouble("health-multiplier",
                        config.getDouble("mobs.default.health-multiplier", 1.0));
                AttributeInstance healthAttr = ent.getAttribute(Attribute.MAX_HEALTH);
                if (healthAttr != null) {
                    double base = healthAttr.getBaseValue();
                    healthAttr.setBaseValue(base * hm);
                    ent.setHealth(Math.min(ent.getHealth(), healthAttr.getValue()));
                }

                AttributeInstance dmgAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
                if (dmgAttr != null) {
                    double dm = conf.getDouble("damage-multiplier",
                            config.getDouble("mobs.default.damage-multiplier", 1.0));
                    dmgAttr.setBaseValue(dmgAttr.getBaseValue() * dm);
                }

                String nameTemplate = Objects.requireNonNullElse(
                        conf.getString("gradient-name", config.getString("mobs.default.gradient-name")),
                        "<gradient:#00FF00:#FFFFFF>{mob}</gradient>"
                );
                String nameRaw = nameTemplate.replace("{mob}", prettyName(typeKey));
                ent.customName(componentFromRaw(nameRaw));
                ent.setCustomNameVisible(false);

                List<String> effects = conf.getStringList("effects");
                for (String eff : effects) {
                    if (eff == null || eff.isBlank()) continue;
                    try {
                        String[] parts = eff.split(":");
                        String t = parts[0].trim();
                        int amp = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                        int dur = parts.length > 2 ? Integer.parseInt(parts[2]) : 200;

                        PotionEffectType pet = Registry.EFFECT.get(NamespacedKey.minecraft(t.toLowerCase(Locale.ROOT)));
                        if (pet != null) {
                            ent.addPotionEffect(new PotionEffect(pet, dur, amp, true, false, true));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Boss handling — only if this entity is listed under bosses:
        if (bossesSec != null && bossesSec.isConfigurationSection(typeKey)) {
            ConfigurationSection bconf = bossesSec.getConfigurationSection(typeKey);
            if (bconf != null && bconf.getBoolean("enabled", true)) {
                String gradName = bconf.getString("gradient-name",
                    "<gradient:#00FF00:#FFFFFF>" + ent.getType().name() + "</gradient>");
                ent.customName(componentFromRaw(gradName));
                ent.setCustomNameVisible(true);

                double hm = bconf.getDouble("health-multiplier", 1.0);
                AttributeInstance hAttr = ent.getAttribute(Attribute.MAX_HEALTH);
                if (hAttr != null) hAttr.setBaseValue(hAttr.getBaseValue() * hm);

                // schedule abilities (existing code uses TimedAbility; adapt as needed)
                List<String> abilityStrings = bconf.getStringList("abilities");
                if (!abilityStrings.isEmpty()) {
                    List<TimedAbility> timedAbilities = abilityStrings.stream()
                            .map(TimedAbility::new)
                            .collect(Collectors.toList());

                    int taskId = new BukkitRunnable() {
                        long tick = 0;
                        @Override
                        public void run() {
                            if (ent.isDead() || !ent.isValid()) {
                                cancel();
                                bossTaskIds.remove(ent.getUniqueId());
                                return;
                            }
                            for (TimedAbility ta : timedAbilities) {
                                if (ta.ready(tick)) {
                                    runBossAbility(ent, ta.name);
                                    ta.trigger(tick);
                                }
                            }
                            tick += 20;
                        }
                    }.runTaskTimer(this, 20L, 20L).getTaskId();

                    bossTaskIds.put(ent.getUniqueId(), taskId);
                    getLogger().log(Level.INFO, "Scheduled boss abilities for {0} ({1})", new Object[]{ent.getType().name(), ent.getUniqueId()});
                }
            }
        }
        
    // ---------------- apply difficulty multipliers at spawn time ----------------
    boolean applyAtSpawn = diffSec != null && diffSec.getBoolean("apply-at-spawn", true);
    boolean applyOnlyOverrides = diffSec != null && diffSec.getBoolean("apply-only-overrides", false);

    if (applyAtSpawn) {
        // If applyOnlyOverrides==true, skip entities not present in mobs.overrides
        if (applyOnlyOverrides && (mobsSec == null || !mobsSec.isConfigurationSection(typeKey))) {
            // skip - not in overrides
        } else {
            MultiplierResult mr = computeMultipliers();
            // Apply to MAX_HEALTH
            AttributeInstance healthAttr = ent.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                try {
                    double base = healthAttr.getBaseValue();
                    double newMax = base * mr.healthMult;
                    healthAttr.setBaseValue(newMax);
                    ent.setHealth(Math.min(ent.getHealth(), newMax));
                } catch (Exception ex) {
                    getLogger().log(Level.FINER, "Failed to set spawned entity health: {0}", ex.getMessage());
                }
            }

            // Apply to ATTACK_DAMAGE
            AttributeInstance dAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dAttr != null) {
                try {
                    double base = dAttr.getBaseValue();
                    dAttr.setBaseValue(base * mr.damageMult);
                } catch (Exception ex) {
                    getLogger().log(Level.FINER, "Failed to set spawned entity damage: {0}", ex.getMessage());
                }
            }

            // log the application (info on first application, else finer)
            getLogger().log(Level.FINER, "Applied difficulty to spawn {0} {1} -> {2}", new Object[]{ent.getType().name(), ent.getUniqueId(), mr.breakdown});
        }
    }

        // bosses
        if (bossesSec != null && bossesSec.isConfigurationSection(typeKey)) {
            ConfigurationSection bconf = bossesSec.getConfigurationSection(typeKey);
            if (bconf != null && bconf.getBoolean("enabled", true)) {
                String gradName = bconf.getString(
                        "gradient-name",
                        "<gradient:#00FF00:#FFFFFF>" + ent.getType().name() + "</gradient>"
                );
                ent.customName(componentFromRaw(gradName));
                ent.setCustomNameVisible(true);

                double hm = bconf.getDouble("health-multiplier", 1.0);
                AttributeInstance hAttr = ent.getAttribute(Attribute.MAX_HEALTH);
                if (hAttr != null) hAttr.setBaseValue(hAttr.getBaseValue() * hm);

                List<String> abilityStrings = bconf.getStringList("abilities");
                if (!abilityStrings.isEmpty()) {
                    List<TimedAbility> timedAbilities = abilityStrings.stream()
                            .map(TimedAbility::new)
                            .collect(Collectors.toList());

                    int taskId = new BukkitRunnable() {
                        long tick = 0;

                        @Override
                        public void run() {
                            if (ent.isDead() || !ent.isValid()) {
                                cancel();
                                bossTaskIds.remove(ent.getUniqueId());
                                return;
                            }

                            for (TimedAbility ta : timedAbilities) {
                                if (ta.ready(tick)) {
                                    runBossAbility(ent, ta.name);
                                    ta.trigger(tick);
                                }
                            }

                            tick += 20; // increment every second
                        }
                    }.runTaskTimer(this, 20L, 20L).getTaskId();

                    bossTaskIds.put(ent.getUniqueId(), taskId);
                    getLogger().log(Level.INFO,
                            "Scheduled boss abilities for {0} ({1})",
                            new Object[]{ent.getType().name(), ent.getUniqueId()}
                    );
                }
            }
        }
    }

    // ---------------- TimedAbility helper ----------------

    private static class TimedAbility {
        final String name;
        final int cooldownTicks;
        private long nextUse = 0;

        TimedAbility(String raw) {
            String[] parts = raw.split(":");
            this.name = parts[0].trim().toUpperCase(Locale.ROOT);

            int cd = 100; // default 5s
            if (parts.length > 1 && parts[1].toLowerCase(Locale.ROOT).endsWith("s")) {
                try {
                    cd = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1)) * 20;
                } catch (NumberFormatException ignored) {}
            }
            this.cooldownTicks = cd;
        }

        boolean ready(long currentTick) {
            return currentTick >= nextUse;
        }

        void trigger(long currentTick) {
            this.nextUse = currentTick + cooldownTicks;
        }
    }

    // ---------------- Boss Abilities ----------------

    private void runBossAbility(LivingEntity boss, String ability) {
        if (ability == null || ability.isEmpty()) return;
        String a = ability.trim().toUpperCase(Locale.ROOT);

        if (a.startsWith("SUMMON_MINIONS")) {
            World w = boss.getWorld();
            for (int i = 0; i < 2; i++) {
                Location spawn = boss.getLocation().clone().add((i == 0) ? 1 : -1, 0, (i == 0) ? 1 : -1);
                w.spawn(spawn, org.bukkit.entity.Zombie.class);
            }
            getLogger().log(Level.INFO, "Boss {0} used SUMMON_MINIONS", boss.getUniqueId());

        } else if (a.startsWith("GROUND_POUND")) {
            for (Player p : boss.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(boss.getLocation()) < 25.0) {
                    Vector v = p.getLocation().toVector()
                            .subtract(boss.getLocation().toVector())
                            .normalize().multiply(1.5);
                    v.setY(0.7);
                    p.setVelocity(v);
                }
            }
            getLogger().log(Level.INFO, "Boss {0} used GROUND_POUND", boss.getUniqueId());

        } else if (a.startsWith("FIREBALL")) {
            List<Player> players = boss.getWorld().getPlayers().stream()
                    .filter(pl -> pl.getLocation().distanceSquared(boss.getLocation()) < 400)
                    .collect(Collectors.toList());
            if (!players.isEmpty()) {
                Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                boss.getWorld().spawn(
                        boss.getLocation().add(0, 1, 0),
                        org.bukkit.entity.SmallFireball.class,
                        fb -> fb.setDirection(
                                target.getLocation().toVector()
                                        .subtract(boss.getLocation().toVector())
                                        .normalize()
                        )
                );
            }
            getLogger().log(Level.INFO, "Boss {0} used FIREBALL", boss.getUniqueId());
        }
    }

    // ---------------- Entity Death Loot ----------------

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Entity ent = e.getEntity();
        if (!isWorldEnabled(ent.getWorld().getName())) return;

        String typeKey = ent.getType().name();

        FileConfiguration config = getConfig();
        ConfigurationSection mobsSec = config.getConfigurationSection("mobs.overrides");
        ConfigurationSection bossesSec = config.getConfigurationSection("bosses");

        // Handle mob overrides
        if (mobsSec != null && mobsSec.isConfigurationSection(typeKey)) {
            List<String> loot = mobsSec.getStringList(typeKey + ".custom-loot");
            if (!loot.isEmpty()) {
                for (String l : loot) {
                    ItemStack item = parseLootString(l);
                    if (item != null) e.getDrops().add(item);
                }
                getLogger().log(Level.INFO, "Applied custom loot for {0} on death ({1})",
                        new Object[]{typeKey, e.getEntity().getUniqueId()});
            }
        }

        // Handle bosses
        if (bossesSec != null && bossesSec.isConfigurationSection(typeKey)) {
            List<String> bossLoot = bossesSec.getStringList(typeKey + ".loot");
            if (!bossLoot.isEmpty()) {
                for (String l : bossLoot) {
                    ItemStack it = parseLootString(l);
                    if (it != null) e.getDrops().add(it);
                }
                getLogger().log(Level.INFO, "Applied boss loot for {0} on death ({1})",
                        new Object[]{typeKey, e.getEntity().getUniqueId()});
            }

            // increment scaling counter
            totalBossKills++;
            getLogger().log(Level.INFO, "Total boss kills: {0}", totalBossKills);
        }

        // --- Custom Loot Handling ---
        ConfigurationSection mobSec = config.getConfigurationSection("mobs.overrides." + ent.getType().name());
        if (mobSec != null) {
            List<String> loot = mobSec.getStringList("custom-loot");
            for (String s : loot) {
                ItemStack drop = parseLootString(s);
                if (drop != null) {
                    // Add alongside vanilla drops
                    e.getDrops().add(drop);
                }
            }
        }
    
    }

    private ItemStack parseLootString(String s) {
        try {
            if (s == null || s.isEmpty()) return null;
            String[] parts = s.split(":");
            String mat = parts[0].trim();
            int amount = 1;
            if (parts.length > 1) {
                if (parts[1].contains("-")) {
                    String[] r = parts[1].split("-");
                    int min = Integer.parseInt(r[0]);
                    int max = Integer.parseInt(r[1]);
                    amount = ThreadLocalRandom.current().nextInt(min, max + 1);
                } else {
                    amount = Integer.parseInt(parts[1]);
                }
            }
            Material m = Material.matchMaterial(mat);
            if (m == null) return null;
            return new ItemStack(m, Math.max(1, amount));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ---------------- join ----------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isWorldEnabled(p.getWorld().getName())) return;

        if (reviveOnJoin.remove(p.getUniqueId())) {
            saveRuntimeData();
            p.setGameMode(GameMode.SURVIVAL);
            AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
            double max = inst != null ? inst.getValue() : 20.0;
            p.setHealth(Math.max(1.0, max));
            p.sendMessage(componentFromRaw(prefixRaw + " " + reviveBroadcastRaw.replace("{player}", p.getName())));
            getLogger().log(Level.INFO, "Revived on join: {0}", p.getName());
        }
    }

    // ---------------- tab complete ----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "revive" -> {
                if (args.length == 1) {
                    if (!sender.hasPermission("deathwish.revive")) return Collections.emptyList();
                    String start = args[0].toLowerCase(Locale.ROOT);
                    return dead.stream()
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(start))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                }
            }

            case "gg" -> {
                if (args.length == 1) {
                    if (!sender.hasPermission("deathwish.gg")) return Collections.emptyList();
                    String start = args[0].toLowerCase(Locale.ROOT);
                    return dead.stream()
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(start))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                }
            }

            case "deathwish", "dw" -> {
                if (args.length == 1) {
                    if (!sender.hasPermission("deathwish.reload")) return Collections.emptyList();
                    return Arrays.asList("reload");
                }
            }
            case "dwreload", "deadlist" -> {
                // no arguments, so nothing to complete
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    // ---------------- utils ----------------

    private boolean isWorldEnabled(String worldName) {
        List<String> enabled = getConfig().getStringList("general.enabled-worlds");
        return enabled.isEmpty() || enabled.contains(worldName);
    }

    private String prettyName(String mobKey) {
        String[] parts = mobKey.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) sb.append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    // ---------------- bStats + update checker helpers ----------------

    private void enableBStatsIfAvailable() {
        boolean enabled = getConfig().getBoolean("general.enable-bstats", true);
        if (!enabled) {
            getLogger().info("bStats integration disabled via config.");
            return;
        }
        // Use reflection to avoid compile-time dependency
        try {
            Class<?> metricsClass = Class.forName("org.bstats.bukkit.Metrics");
            try {
                // constructor: Metrics(Plugin, int)
                Object metrics = metricsClass
                        .getConstructor(org.bukkit.plugin.Plugin.class, int.class)
                        .newInstance(this, BSTATS_ID);
                getLogger().log(Level.INFO, "bStats metrics attempted to initialize (id: {0}).", BSTATS_ID);
            } catch (NoSuchMethodException nsme) {
                getLogger().info("bStats Metrics class found but constructor mismatch.");
            }
        } catch (ClassNotFoundException cnfe) {
            getLogger().info("bStats not present; skipping metrics registration.");
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | SecurityException | InvocationTargetException t) {
            getLogger().log(Level.WARNING, "bStats registration failed: {0}", t.getMessage());
        }
    }

    private static final String UPDATE_CHECK_URL = 
        "https://raw.githubusercontent.com/Astroolean/DeathWish/refs/heads/main/version.txt";
    // ^ Replace with your own endpoint that returns plain text like: 1.0.3

    private void checkForUpdatesAsync() {
        boolean enabled = getConfig().getBoolean("general.update-check", true);
        if (!enabled) {
            getLogger().info("Update checker disabled via config.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URI uri = URI.create(UPDATE_CHECK_URL);
                    URL url = uri.toURL();
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(3000);
                    con.setReadTimeout(3000);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", "DeathWish-Updater");

                    int code = con.getResponseCode();
                    if (code != 200) {
                        getLogger().log(Level.INFO, "Update check responded with HTTP {0}", code);
                        return;
                    }

                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                        String latest = br.readLine();
                        if (latest != null) {
                            String current = getPluginMeta().getVersion();
                            if (!current.equals(latest.trim())) {
                                getLogger().log(Level.WARNING,
                                        "A new DeathWish version is available: {0} (running {1}).",
                                        new Object[]{latest, current});
                            } else {
                                getLogger().log(Level.INFO, "DeathWish is up to date ({0}).", current);
                            }
                        }
                    }
                } catch (IOException ex) {
                    getLogger().log(Level.INFO, "Update check failed (non-fatal): {0}", ex.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }

    // ---------------- startup banner ----------------
    private void printBanner() {
        getLogger().info(" ");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗    ██╗    ██╗██╗███████╗██╗  ██╗");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║    ██║    ██║██║██╔════╝██║  ██║");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ██║  ██║█████╗  ███████║   ██║   ███████║    ██║ █╗ ██║██║███████╗███████║");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║    ██║███╗██║██║╚════██║██╔══██║");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ██████╔╝███████╗██║  ██║   ██║   ██║  ██║    ╚███╔███╔╝██║███████║██║  ██║");
        getLogger().info("ㅤㅤㅤㅤㅤㅤㅤㅤㅤㅤ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝     ╚══╝╚══╝ ╚═╝╚══════╝╚═╝  ╚═╝");
        getLogger().info(" ");
        getLogger().log(Level.INFO, "DeathWish version{0} by astroolean", getPluginMeta().getVersion());
    }
}