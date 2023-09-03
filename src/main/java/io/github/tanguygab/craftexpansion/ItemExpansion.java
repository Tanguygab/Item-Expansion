package io.github.tanguygab.craftexpansion;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ItemExpansion extends PlaceholderExpansion implements Taskable, Listener {

    private final List<String> placeholders = Arrays.asList("%item_key_<material>%","%item_craft_<materials...>%","%item_craft_key_<materials...>%","%item_craft_amount_<materials...>%","%item_craft_required:<position>_<materials...>%","%item_can_craft_<materials...>%","%item_last_clicked%","%item_last_clicked_key%");
    private final Map<String,List<ItemStack>> recipes = new HashMap<>();
    private final Map<Player,ItemStack> lastClickedItems = new HashMap<>();

    @Override
    public @NotNull String getIdentifier() {
        return "item";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Tanguygab";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return placeholders;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        params = PlaceholderAPI.setBracketPlaceholders(player,params);
        if (params.startsWith("key_")) {
            String item = params.substring(4);
            Material material = Material.getMaterial(item);
            return material == null ? "minecraft:air" : material.getKey().toString();
        }
        if (params.startsWith("last_clicked")) return getItem(lastClickedItems.get(player.getPlayer()),params.equals("last_clicked_key") ? "key" : "result");
        if (params.startsWith("can_craft_")) {
            if (player == null || player.getPlayer() == null) return getPlaceholderAPI().getPlaceholderAPIConfig().booleanFalse();
            params = params.substring(10);
            List<ItemStack> items = getRecipeItems(params);
            if (items == null) return getPlaceholderAPI().getPlaceholderAPIConfig().booleanFalse();
            Map<ItemStack,Integer> map = new HashMap<>();
            items.forEach(item->map.put(item,map.getOrDefault(item,0)+1));
            Player p = player.getPlayer();
            for (ItemStack item : map.keySet()) {
                if (!p.getInventory().contains(item,map.get(item)))
                    return getPlaceholderAPI().getPlaceholderAPIConfig().booleanFalse();
            }
            return getPlaceholderAPI().getPlaceholderAPIConfig().booleanTrue();
        }
        if (!params.startsWith("craft_") || params.length() < 7) return null;
        String type = params.split("_")[1];
        params = params.substring(type.length());

        List<ItemStack> items = getRecipeItems(params);
        if (items == null) return type.startsWith("required:") ? "0" : getItem(null,type);

        if (type.startsWith("required:")) {
            try {
                int i = Integer.parseInt(type.substring(9));
                if (i < 0 || i > 8) return "0";
                ItemStack item = items.get(i);
                if (item.getType() == Material.AIR) return "0";
                int count = 0;
                for (ItemStack it : items)
                    if (it.getType() == item.getType())
                        count++;
                return String.valueOf(count);
            } catch (Exception e) {return "0";}
        }

        World world = player == null || player.getPlayer() == null ? Bukkit.getServer().getWorlds().get(0) : player.getPlayer().getWorld();
        Recipe recipe = Bukkit.getServer().getCraftingRecipe(items.toArray(new ItemStack[]{}),world);
        ItemStack item = recipe == null ? null : recipe.getResult();

        return getItem(item,type);
    }

    private List<ItemStack> getRecipeItems(String params) {
        List<ItemStack> items;
        if (!recipes.containsKey(params)) {
            String[] materials = params.split(",");
            if (materials.length == 0) return null;
            items = new ArrayList<>();
            for (String material : materials) {
                Material mat = Material.getMaterial(material);
                items.add(new ItemStack(mat == null ? Material.AIR : mat));
            }
            if (items.size() < 9) items.addAll(Collections.nCopies(9-items.size(),new ItemStack(Material.AIR)));
            while (items.size() > 9) items.remove(9);
            recipes.put(params,items);
        }
        return recipes.get(params);
    }

    private String getItem(ItemStack item, String type) {
        Object output = "";
        switch (type) {
            case "result":
                output = item == null ? "AIR" : item.getType();
                break;
            case "key":
                output = item == null ? "minecraft:air" : item.getType().getKey();
                break;
            case "amount": output = item == null ? "0" : item.getAmount();
        }
        return String.valueOf(output);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickEvent(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        getPlaceholderAPI().getServer().getScheduler().runTask(getPlaceholderAPI(),()-> lastClickedItems.put((Player) e.getWhoClicked(),item));
    }

    @Override
    public void start() {
        getPlaceholderAPI().getServer().getPluginManager().registerEvents(this,getPlaceholderAPI());
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }
}
