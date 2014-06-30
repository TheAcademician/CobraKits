package us.drome.CobraKits;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/*
 * CobraKits v1.1 - Coded by TheAcademician - Released under GPLv3
 * TheAcademician@gmail.com
 */

public class CobraKits extends JavaPlugin implements Listener {
	//KitList is an implementation of ArrayList to store Kit objects, kitList is the local object to store kits for the plugin.
	private KitList kitList;
	//KitList to store kit records that can be removed from the yml file.
	private KitList kitsToRemove = new KitList();
	//File object to interact with kits.yml
	private File kitsDByml;
	//Object to interact with the in-memory configuration loaded from kits.yml
	private FileConfiguration kitsDB;
	//Variables to cache config file values.
	private boolean silentEnabled = false;
	private boolean concatEnabled = false;
	private boolean cooldownEnabled = false;
	private int cooldownDuration = 0;
	private ArrayList<String> startkits = new ArrayList<String>();
	private ArrayList<String> loginkits = new ArrayList<String>();
	private ArrayList<String> respawnkits = new ArrayList<String>();
	private boolean updateEnabled = false;
	private boolean updateAvailable = false;
	private String updateVersion = "";
	private String updateGameVersion = "";
	//Cooldown storage objects.
	private File cooldownDByml;
	private FileConfiguration cooldownDB;
	private CooldownList cooldownList;
	private CooldownList cdToRemove = new CooldownList();
	
	public void onEnable(){
		getLogger().info("version " + getDescription().getVersion() + " is loading...");
		try{
			//Save the default configuration.
			saveDefaultConfig();
			getConfig().options().header("Cobra Kits Configuration - v" + getDescription().getVersion() + " - by TheAcademician");
			//Load values from the config.yml
			silentEnabled = getConfig().getBoolean("silent.always");
			concatEnabled = getConfig().getBoolean("concat.always");
			cooldownEnabled = getConfig().getBoolean("cooldown.enabled");
			cooldownDuration = getConfig().getInt("cooldown.duration");
			startkits = (ArrayList<String>) getConfig().getStringList("startkits");
			loginkits = (ArrayList<String>) getConfig().getStringList("loginkits");
			respawnkits = (ArrayList<String>) getConfig().getStringList("respawnkits");
			updateEnabled = getConfig().getBoolean("update");
			
			kitsDByml = new File(getDataFolder() + File.separator + "kits.yml");
			//If kits.yml does not exist, create it.
			if(!kitsDByml.exists()) kitsDByml.createNewFile();
			//Load the values of kits.yml into the kitsDB object.
			kitsDB = YamlConfiguration.loadConfiguration(kitsDByml);
			//load the kitList with the values from kitsDB.
			kitList = LoadKits();
			KitListChecker();
			
			cooldownDByml = new File(getDataFolder() + File.separator + "cooldowns.yml");
			if(!cooldownDByml.exists()) cooldownDByml.createNewFile();
			cooldownDB = YamlConfiguration.loadConfiguration(cooldownDByml);
			cooldownList = LoadCooldowns();

			//Confirm that all specified startkits from config.yml are in the kitList.
			if(startkits.size() > 0 ) {
				for(String entry: startkits) {
					if(entry != null && !kitList.contains(entry)){
						//If the kit is not in kitList, clear the config.yml entry.
						getConfig().set(entry, null);
						getLogger().info("Configured Start Kit " + entry + " is not valid, clearing it.");
					}	
				}
			}
			
			//Confirm that all specified loginkits from config.yml are in the kitList.
			if(loginkits.size() > 0) {
				for(String entry: loginkits) {
					if(entry !=null && !kitList.contains(entry)) {
						//If the kit is not in kitList, clear the config.yml entry.
						getConfig().set(entry, null);
						getLogger().info("Configured Login Kit " + entry + " is not valid, clearing it.");
					}
				}
			}
			
			//Confirm that all specified respawnkits from config.yml are in the kitList.
			if(respawnkits.size() > 0) {
				for(String entry: respawnkits) {
					if(entry !=null && !kitList.contains(entry)) {
						//If the kit is not in kitList, clear the config.yml entry.
						getConfig().set(entry, null);
						getLogger().info("Configured Respawn Kit " + entry + " is not valid, clearing it.");
					}
				}
			}
			
			//Set this class up to handle events from Bukkit.
			getServer().getPluginManager().registerEvents(this, this);
			
			//Check for new version here and inform the console that it is available.
			if(updateEnabled) {
				Updater updater = new Updater(this,47788,this.getFile(),Updater.UpdateType.NO_DOWNLOAD, false);
				if(updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE) {
					updateAvailable = true;
					updateVersion = updater.getLatestName();
					updateGameVersion = updater.getLatestGameVersion();
					getLogger().info(updater.getLatestName() + " is now available for " + updater.getLatestGameVersion() + ". Run " + ChatColor.BOLD + "/ckits update" + ChatColor.RESET + " to download it!");
				}
			}
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	public void onDisable(){
		try{
			//Save the config file.
			saveConfig();
			//Save the kitList to kits.yml.
			SaveKits();
			SaveCooldowns();
			getLogger().info("version " + getDescription().getVersion() + " has been unloaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public KitList LoadKits() {
		getLogger().info("has begun loading kits...");
		KitList loadingList = new KitList();
		//Iterate through every top-level key in kitsDB (the false indicates to not grab sub-keys) which is the kit's owner.
		for(String owner : kitsDB.getKeys(false)) {
			//Iterate through every kit name by getting a list of keys belonging to the owner's section of the config.
			for(String name : kitsDB.getConfigurationSection(owner).getKeys(false)) {
				
				//Detect kits that have blank sections. These must be flushed from the kit list.
				if(kitsDB.getList(owner + "." + name + ".Inventory").isEmpty()) {
					kitsDB.set(owner + "." + name, null);
					getLogger().info("has removed invalid kit " + owner + "." + name + "!");
					continue;
				}
				//Add a new Kit to loadingList with the owner, name, and the Lists containing inventory, armor, and potions, as well as cooldown and cost values.
				loadingList.add(new Kit(owner, name,
						kitsDB.getList(owner + "." + name + ".Inventory"),
						kitsDB.getList(owner + "." + name + ".Armor"),
						kitsDB.getList(owner + "." + name + ".Potions"),
						kitsDB.getInt(owner + "." + name + ".Cooldown"),
						kitsDB.getItemStack(owner + "." + name + ".Cost")));
			}
		}
		getLogger().info("has successfully loaded " + loadingList.size() + " kits!");
		return loadingList;
	}
	
	public void KitListChecker() {
		//check kit list for any amount:0 entities and restore them to 1
		int itemsFixed = 0;
		int kitsFixed = 0;
		for(Kit kit : kitList) {
			for(ItemStack item : kit.Inventory()) {
				if(item != null && item.getAmount() == 0) {
					item.setAmount(1);
					itemsFixed++;
				}
			}
			for(ItemStack item : kit.Armor()) {
				if(item != null && item.getAmount() == 0) {
					item.setAmount(1);
					itemsFixed++;
				}
			}
			if(itemsFixed > 0) {
				getLogger().info("has fixed " + itemsFixed + " \"ghost\" items in kit: " + kit.Owner() + "." + kit.Name());
				itemsFixed = 0;
				kitsFixed++;
			}
		}
		
		if(kitsFixed > 0)
			SaveKits();
	}
	
	public void SaveKits() {
		//Clean up all deleted entries from the yml file here.
		for(Kit entry: kitsToRemove) {
			if(kitsDB.contains(entry.Owner() + "." + entry.Name())) {
				kitsDB.set(entry.Owner() + "." + entry.Name(), null);
			}
		}
		kitsToRemove.clear();
		
		//Iterate through all Kits in the kitList.
		for(Kit entry : kitList) {
			//If the kitsDB does not have a section with a key owner.kitname.Armor(a basic check to see if the kit exists or not), create the .Armor and .Inventory sections.
			if(!kitsDB.contains(entry.Owner() + "." + entry.Name() + ".Armor")) {
				kitsDB.createSection(entry.Owner() + "." + entry.Name() + ".Armor");
				kitsDB.createSection(entry.Owner() + "." + entry.Name() + ".Inventory");
				kitsDB.createSection(entry.Owner() + "." + entry.Name() + ".Potions");
				kitsDB.createSection(entry.Owner() + "." + entry.Name() + ".Cooldown");
				kitsDB.createSection(entry.Owner() + "." + entry.Name() + ".Cost");
			}
			//Set the .Armor and .Inventory keys. As they are stored as ItemStack[] in the Kit, call a custom method to return them as a List, which can be saved to the config file.
			kitsDB.set(entry.Owner() + "." + entry.Name() + ".Armor", entry.ArmorAsList());
			kitsDB.set(entry.Owner() + "." + entry.Name() + ".Inventory", entry.InventoryAsList());
			//Set the potion effect list, cooldown, and cost values.
			kitsDB.set(entry.Owner() + "." + entry.Name() + ".Potions", entry.Potions());
			kitsDB.set(entry.Owner() + "." + entry.Name() + ".Cooldown", entry.Cooldown());
			kitsDB.set(entry.Owner() + "." + entry.Name() + ".Cost", entry.Cost());
		}
		//Save the in-memory kitsDB data to the kits.yml file via the kitsDByml object.
		try {
			kitsDB.save(kitsDByml);
			getLogger().info("kits.yml has been successfully saved with " + kitList.size() + " kits.");
		} catch (IOException e) {
			 getLogger().log(Level.SEVERE, "Could not save config to " + kitsDB, e);
		}
	}
	
	public CooldownList LoadCooldowns() {
		getLogger().info("has begun loading cached cooldowns...");
		CooldownList loadingList = new CooldownList();
		
		//Iterate through all keys in cooldowns.yml
		for(String player : cooldownDB.getKeys(false)) {
			for(String kit : cooldownDB.getConfigurationSection(player).getKeys(false)) {
				//Check for empty cooldowns and remove them.
				if(kit.split(".").length < 2) {
					cooldownDB.set(player + "." + kit, null);
					continue;
				}
				//Add a new Cooldown entry to the list with the player name, kit name, and end time (in milliseconds).
				cooldownList.add(new Cooldown(player, kit, cooldownDB.getLong(player + "." + kit + ".endTime")));
			}
		}
		getLogger().info("has successfully loaded " + loadingList.size() + " cooldowns!");
		return loadingList;
	}
	
	public void SaveCooldowns() {
		//Iterate through all Cooldowns to remove.
		for(Cooldown entry : cdToRemove) {
			//If they are found in the list, set them to null.
			if(cooldownDB.contains(entry.player + "." + entry.kit)) {
				cooldownDB.set(entry.player + "." + entry.kit.split(".")[0], null);
			}
		}
		cdToRemove.clear(); //Clear the list.
		
		//Iterate through all Cooldowns.
		for(Cooldown entry : cooldownList) {
			//If the sections are missing, create them.
			if(!cooldownDB.contains(entry.player + "." + entry.kit + ".endTime")) {
				cooldownDB.createSection(entry.player + "." + entry.kit + ".endTime");
			}
			//Set the entry including player name, kit name, and end time (in milliseconds).
			cooldownDB.set(entry.player + "." + entry.kit + ".endTime", entry.endTime);
		}
		
		//Attempt to save cooldowns.yml
		try {
			cooldownDB.save(cooldownDByml);
		} catch (IOException e) {
			 getLogger().log(Level.SEVERE, "Could not save config to " + cooldownDB, e);
		}
	}
	
	//Whenever someone logs onto the server, attempt to apply any startkits or loginkits to the player, if applicable.
	@EventHandler
	public void loginDetector(PlayerJoinEvent login) {
		//If the player has not played before on this server.
		if(!login.getPlayer().hasPlayedBefore()) {
			//Iterate through the startkits List and apply any kits. Startkits are applied silently (true) and concatenate with the current inventory (true).
			for(String kit : startkits) {
				if(kitList.contains(kit))
					applyKit(login.getPlayer(), kit, login.getPlayer(), true, true);
			}
		} else {
			//Iterate through the loginkits List and apply any kits. Loginkits are applied silently (true) and concatenate with the current inventory (true).
			for(String kit : loginkits) {
				if(kitList.contains(kit))
					if(login.getPlayer().hasPermission("cobrakits.login") || login.getPlayer().hasPermission("cobrakits.login." + kit)) {
						applyKit(login.getPlayer(), kit, login.getPlayer(), true, true);
					}
			}
		}
		
		//Notify anyone with cobrakits.* permissions if an update is available.
		if(login.getPlayer().hasPermission("cobrakits.*") && updateAvailable) {
			login.getPlayer().sendMessage(ChatColor.RED + updateVersion + ChatColor.LIGHT_PURPLE + " is now available for " + ChatColor.RED + updateGameVersion + ChatColor.LIGHT_PURPLE +
					". Run " + ChatColor.RED + ChatColor.BOLD + "/ckits update" + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " to download it!");
		}
	}
	
	//When someone respawns after a death, attempt to apply any respawn kits to the player if they have permission.
	@EventHandler
	public void respawnDetector(PlayerRespawnEvent respawn) {
		for(String kit : respawnkits) {
			if(kitList.contains(kit))
				if(respawn.getPlayer().hasPermission("cobrakits.respawn") || respawn.getPlayer().hasPermission("cobrakits.respawn." + kit)) {
					applyKit(respawn.getPlayer(), kit, respawn.getPlayer(), true, true);
				}
		}
	}
	
	//Whenever someone creates a sign, determine if it is a Kit Sign and correctly configure the signs values.
	@EventHandler
	public void kitSignCreateDetector(SignChangeEvent sign) {
		//When a sign is created, if it matches the proper syntax, check permissions and see if the kitname is valid.
		if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[COBRAKIT]") || ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[KIT]")) {
			if(sign.getPlayer().hasPermission("cobrakits.sign.create")) {
				if(kitList.contains(sign.getLine(2)) && !sign.getLine(2).contains(".")) {
					//If the kit is valid, we format the sign to show the user it is enabled.
					if(sign.getLine(0).equalsIgnoreCase("[COBRAKIT]"))
						sign.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + "[COBRAKIT]");
					if(sign.getLine(0).equalsIgnoreCase("[KIT]"))
						sign.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + "[KIT]");
					ItemStack cost = kitList.get(kitList.indexOf(sign.getLine(2))).Cost();
					if(cost != null) {
						sign.setLine(1, ChatColor.DARK_PURPLE + String.valueOf(cost.getAmount()) + " " + cost.getType().toString());
					} else { sign.setLine(1, ""); }
					sign.setLine(2, ChatColor.DARK_BLUE + sign.getLine(2));
					String signOptions ="";
					if(sign.getLine(3).contains("c"))
						signOptions += ChatColor.GOLD + "c";
					else
						signOptions += ChatColor.STRIKETHROUGH + "c";
					signOptions += ChatColor.RESET + "|";
					if(sign.getLine(3).contains("s"))
						signOptions += ChatColor.GOLD + "s";
					else
						signOptions += ChatColor.STRIKETHROUGH + "s";
					sign.setLine(3, signOptions);
				} else {
					//If we can't find the kit, clear the sign text and tell the player.
					for(int x=0; x<4; x++) {
						sign.setLine(x, "");
					}
					sign.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + sign.getLine(2) + ChatColor.LIGHT_PURPLE + ", does not exist or is a Personal kit. Use "
					+ ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "where Global kits are shown as Green.");
				}
			} else {
				//If the player doesn't have permission to make kit signs, blank the sign and send error message.
				for(int x=0; x<4; x++) {
					sign.setLine(x, "");
				}
				sign.getPlayer().sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			}
		}
	}
	
	//Whenever a Player interacts with a block, determine if it is a Kit Sign, and apply the kit.
	@EventHandler
	public void kitSignUseDetector(PlayerInteractEvent e) {
		//When a player interacts with a block, narrow down whether it is a kit sign or not.
		Block b = e.getClickedBlock();
		if(b == null) //If the player magically interacted with nothingness, return out of the method.
			return;
		if(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
			//If the block interacted by the player is a sign of either wall or post type, continue.
			Sign sign = (Sign)b.getState();
			if(ChatColor.stripColor(sign.getLine(0)).equals("[COBRAKIT]") || ChatColor.stripColor(sign.getLine(0)).equals("[KIT]")) {
				//If the kit sign matches the required syntax, check and see if the kit is still valid.
				if(kitList.contains(ChatColor.stripColor(sign.getLine(2)))) {
					//With a valid kit, check permissions to see if the player can use this sign.
					if(e.getPlayer().hasPermission("cobrakits.sign.use") || e.getPlayer().hasPermission("cobrakits.sign.use." + ChatColor.stripColor(sign.getLine(2)))) {
						Boolean[] options = new Boolean[2];
						//Here, check to see if either the concat or silent flags are set to ON.
						if(!sign.getLine(3).isEmpty()) {
							if(sign.getLine(3).contains(ChatColor.GOLD + "c"))
								options[1] = true;
							if(sign.getLine(3).contains(ChatColor.GOLD + "s"))
								options[0] = true;
						}
						//Send the applyKit method the required arguments, including kitname, and the silent and concat options.
						applyKit(e.getPlayer(), ChatColor.stripColor(sign.getLine(2)), e.getPlayer(), (options[0] != null) ? options[0] : false, (options[1] != null) ? options[1] : false);
					} else {
						e.getPlayer().sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
					}
				} else {
					e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + sign.getLine(2) + ChatColor.LIGHT_PURPLE + ", does not exist. Please update this sign to use a valid kit.");
				}
			}
		}
	}

	
	//Method that displays the /ckits help command output which details usage info for the various commands.
	private void helpInfo(CommandSender sender, Args args) {
		if(args.Arg2().isEmpty()) {
			sender.sendMessage("- " + ChatColor.RED + "CobraKits" + ChatColor.WHITE + " - v" + getDescription().getVersion() + " - Developed by " + ChatColor.LIGHT_PURPLE + "TheAcademician" + ChatColor.WHITE + ".");
			sender.sendMessage(ChatColor.RED + "/lkit or /kits " + ChatColor.WHITE + ": List all kits that you can use.");
			sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Make a kit based on your inventory.");
			sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Update a kit.");
			sender.sendMessage(ChatColor.RED + "/rkit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname]" + ChatColor.WHITE + ": Rename a kit.");
			sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!");
			sender.sendMessage(ChatColor.RED + "/kit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[username]" + ChatColor.WHITE + ": Use/Give the specified kit.");
			sender.sendMessage(ChatColor.RED + "Kit List Colors: " + ChatColor.DARK_PURPLE + "Server" + ChatColor.RED + ", " + ChatColor.GREEN + "Global" + ChatColor.RED + ", " + ChatColor.GOLD + "Personal" + ChatColor.RED + ", " + ChatColor.AQUA + "Other's Kits");
			sender.sendMessage(ChatColor.RED + "Run /ckits help " + ChatColor.AQUA + "[command]" + ChatColor.RED + " to view more information about it.");
		} else {
			switch (args.Arg2().toLowerCase()) {
			case "lkit": //passthrough to kits
			case "kits":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/lkit or /kits");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.WHITE + "none");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "Directly tied to kit use permissions.");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Colors: " + ChatColor.DARK_PURPLE + "Server" + ChatColor.RED + ", " + ChatColor.GREEN + "Global" + ChatColor.RED + ", " + ChatColor.GOLD + "Personal" + ChatColor.RED + ", " + ChatColor.AQUA + "Other's Kits");
				break;
			case "ckit":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/ckit " + ChatColor.AQUA + "[kitname]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.GREEN + "-g/-global: " + ChatColor.WHITE + "Create a Global Kit (requires .createall)");
				sender.sendMessage(ChatColor.GREEN + "        -sk/-server: " + ChatColor.WHITE + "Create a Server Kit (requires .createall)");
				sender.sendMessage(ChatColor.GREEN + "        -p/-potions: " + ChatColor.WHITE + "Capture active potion effects to the kit.");
				sender.sendMessage(ChatColor.GREEN + "        -cd/-cooldown [seconds]: " + ChatColor.WHITE + "Add a cooldown to the kit.");
				sender.sendMessage(ChatColor.GREEN + "        -kc/-cost [Material]:[amount]: " + ChatColor.WHITE + "Specify a cost that must be paid to use the kit.");
				sender.sendMessage(ChatColor.GREEN + "Materials: " + ChatColor.RESET + "jd.bukkit.org/rb/apidocs/org/bukkit/Material.html");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "cobrakits.create, cobrakits.createall");
				break;
			case "ukit":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/ukit " + ChatColor.AQUA + "[kitname]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.GREEN + "-p/-potions: " + ChatColor.WHITE + "Updates or clears potion effects.");
				sender.sendMessage(ChatColor.GREEN + "        -cd/-cooldown [seconds]: " + ChatColor.WHITE + "Change kit cooldown. Supply 0 to clear.");
				sender.sendMessage(ChatColor.GREEN + "        -kc/-cost [Material]:[amount]: " + ChatColor.WHITE + "Change the kit cost, or \"clear\".");
				sender.sendMessage(ChatColor.GREEN + "Materials: " + ChatColor.RESET + "jd.bukkit.org/rb/apidocs/org/bukkit/Material.html");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "cobrakits.update, cobrakits.updateall");
				break;
			case "rkit":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/rkit " + ChatColor.AQUA + "[old kitname] " + ChatColor.BLUE + "[new kitname]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.GREEN + "-g/-global: " + ChatColor.WHITE + "Change to a Global Kit, ignores [new kitname](requires .renameall)");
				sender.sendMessage(ChatColor.GREEN + "        -sk/-server: " + ChatColor.WHITE + "Change to a Server Kit, ignores [new kitname] (requires .renameall)");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "cobrakits.rename, cobrakits.renameall");
				break;
			case "dkit":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/dkit " + ChatColor.AQUA + "[kitname]");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.WHITE + "none");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "cobrakits.delete, cobrakits.deleteall");
				break;
			case "kit":
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Usage: " + ChatColor.WHITE + "/kit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[target player]" + ChatColor.WHITE + " (requires .give)");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Flags: " + ChatColor.GREEN + "-s/-silent: " + ChatColor.WHITE + "Applies the kit without informing the receiver.");
				sender.sendMessage(ChatColor.GREEN + "        -c/-concat: " + ChatColor.WHITE + "Add kit to the current inventory instead of replacing it.");
				sender.sendMessage(ChatColor.GREEN + "        -g/-global: " + ChatColor.WHITE + "If a Global and Personal kit share a name, use the Global kit.");
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Permissions: " + ChatColor.WHITE + "cobrakits.use, cobrakits.useeall, cobrakits.use.[kitname], cobrakits.give");
				break;
			}
		}
	}
	
	//Method that displays the usage info when a player runs the /ckits command with no options.
	private void usageInfo(CommandSender sender){
		sender.sendMessage("- " + ChatColor.RED + "CobraKits" + ChatColor.WHITE + " - v" + getDescription().getVersion() + " - Developed by " + ChatColor.LIGHT_PURPLE + "TheAcademician" + ChatColor.WHITE + ".");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "help" + ChatColor.WHITE + ": List all CobraKits commands.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "current" + ChatColor.WHITE + ": List current settings.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "cooldown" + ChatColor.WHITE + ": Toggle kit cooldowns.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "duration" + ChatColor.AQUA+ " [seconds]" + ChatColor.WHITE + ": Set cooldown in seconds.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "silent" + ChatColor.WHITE + ": Toggles silent kit give.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "concat" + ChatColor.WHITE + ": Toggles concatenation on give.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "startkit" + ChatColor.AQUA + " +/-[kitname]" + ChatColor.WHITE + ": Add/Remove first-time logon kits.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "loginkit" + ChatColor.AQUA + " +/-[kitname]" + ChatColor.WHITE + ": Add/Remove logon kits.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "respawnkit" + ChatColor.AQUA + " +/-[kitname]" + ChatColor.WHITE + ": Add/Remove respawn kits.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "updater" + ChatColor.WHITE + ":  Toggle the updater.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "update" + ChatColor.WHITE + ": Update to the latest CobraKits.");
	}

	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("ckits")) { //Usage info or make config changes
			if(args.length == 0){
				usageInfo(sender);
			} else if(args[0].equalsIgnoreCase("help")) {
				//If the help option is specified, display help info for the various commands.
				Args options = new Args(args);
				helpInfo(sender, options);
			} else if(sender.hasPermission("cobrakits.*")) {
				switch (args[0].toLowerCase()) {
					case "cooldown":
						//If cooldown is specified, toggle the cooldownEnabled boolean in memory and set the config option, then inform the player.
						cooldownEnabled = !cooldownEnabled;
						getConfig().set("cooldown.enabled", cooldownEnabled);
						saveConfig();
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						break;
					case "duration":
						//If duration is specified, set the variable to the amount of time specified * 20 to convert seconds to ticks. Set config values and send message.
						if(args.length > 1){
							cooldownDuration = Integer.valueOf(args[1]) * 20;
							getConfig().set("cooldown.duration", cooldownDuration);
							saveConfig();
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldown Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						} else {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						}
						break;
					case "silent":
						//If silent is specified, toggle the silentEnabled boolean and then set the config file. Finally, inform the user.
						silentEnabled = !silentEnabled;
						getConfig().set("silent.always", silentEnabled);
						saveConfig();
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						break;
					case "concat":
						//If concat is specified, toggle the concatEnabled boolean and then set the config file. Finally, inform the user.
						concatEnabled = !concatEnabled;
						getConfig().set("concat.always", concatEnabled);
						saveConfig();
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						break;
					case "startkit":
						if(args.length > 1) {
							//If the user has specified the clear option, set the startkits key to null.
							if(args[1].equalsIgnoreCase("clear")) {
								startkits.clear();
								getConfig().set("startkits", null);
								saveConfig();
								sender.sendMessage(ChatColor.LIGHT_PURPLE + " The Login Kits list has been cleared.");
							} else {
								//If the second argument begins with a "+" they want to add a kit to the startkits List.
								if(args[1].startsWith("+")) {
									//Remove the "+" from the kitname.
									String kitname = args[1].substring(1);
									//Check the kitList to see if the specified kitname is valid.
									if(kitList.contains(kitname)) {
										//If the kit exists, add the name to the startkits List and save it to the config.
										startkits.add(kitname);
										getConfig().set("startkits", startkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been added to the First-Time Login Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
								//If the second argument begins with a "-" they want to remove a kit from the startkits List.
								} else if(args[1].startsWith("-")) {
									//Remove the "-" from the kitname.
									String kitname = args[1].substring(1);
									//Check the startkits list to see if the specified kitname is present.
									if(startkits.contains(kitname)) {
										//If they kit exists, remove it from the startkits list.
										startkits.remove(kitname);
										getConfig().set("startkits", startkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been removed from the First-Time Login Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", is not a First-Time Login Kit. Use " + ChatColor.RED + "/ckits startkits" + ChatColor.LIGHT_PURPLE + "to see the current kits.");
								}
							}
						} else {
							//If the user has not specified any arguments, display a list of all current startkits entries.
							if(startkits.size() != 0) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current First-Time Login Kits:");
								sender.sendMessage(startkits.toArray(new String[startkits.size()]));
							} else
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have no active First-Time Login Kits. Set one with " + ChatColor.RED + "/ckits startkit +<kitname>");
						}
						break;
					case "loginkit":
						if(args.length > 1) {
							//If the user has specified the clear option, set the loginkits key to null.
							if(args[1].equalsIgnoreCase("clear")) {
								loginkits.clear();
								getConfig().set("loginkits", null);
								saveConfig();
								sender.sendMessage(ChatColor.LIGHT_PURPLE + " The Login Kits list has been cleared.");
							} else {
								//If the second argument begins with a "+" they want to add a kit to the loginkits List.
								if(args[1].contains("+")) {
									//Remove the "+" from the kitname.
									String kitname = args[1].substring(1);
									//Check the kitList to see if the specified kitname is valid.
									if(kitList.contains(kitname)) {
										//If the kit exists, add the name to the loginkits List and save it to the config.
										loginkits.add(kitname);
										getConfig().set("loginkits", loginkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been added to the Login Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
								//If the second argument begins with a "-" they want to remove a kit to the loginkits List.
								} else if(args[1].contains("-")) {
									//Remove the "-" from the kitname.
									String kitname = args[1].substring(1);
									//Check the loginkits List to see if the specified kitname is present.
									if(loginkits.contains(kitname)) {
										//If they kit exists, remove it from the loginkits list.
										loginkits.remove(kitname);
										getConfig().set("loginkits", loginkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been removed from the Login Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", is not a Login Kit. Use " + ChatColor.RED + "/ckits loginkits " + ChatColor.LIGHT_PURPLE + "to see the current kits.");
								}
							}
						} else {
							//If the user has not specified any arguments, display a list of all current loginkits entries.
							if(loginkits.size() != 0) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current Login Kits:");
								sender.sendMessage(loginkits.toArray(new String[loginkits.size()]));
							} else
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have no active Login Kits. Set one with " + ChatColor.RED + "/ckits loginkit +<kitname>");
						}
						break;
					case "respawnkit":
						if(args.length > 1) {
							//If the user has specified the clear option, set the loginkits key to null.
							if(args[1].equalsIgnoreCase("clear")) {
								respawnkits.clear();
								getConfig().set("respawnkits", null);
								saveConfig();
								sender.sendMessage(ChatColor.LIGHT_PURPLE + " The Respawn Kits list has been cleared.");
							} else {
								//If the second argument begins with a "+" they want to add a kit to the loginkits List.
								if(args[1].contains("+")) {
									//Remove the "+" from the kitname.
									String kitname = args[1].substring(1);
									//Check the kitList to see if the specified kitname is valid.
									if(kitList.contains(kitname)) {
										//If the kit exists, add the name to the loginkits List and save it to the config.
										respawnkits.add(kitname);
										getConfig().set("respawnkits", respawnkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been added to the Respawn Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
								//If the second argument begins with a "-" they want to remove a kit to the loginkits List.
								} else if(args[1].contains("-")) {
									//Remove the "-" from the kitname.
									String kitname = args[1].substring(1);
									//Check the loginkits List to see if the specified kitname is present.
									if(respawnkits.contains(kitname)) {
										//If they kit exists, remove it from the loginkits list.
										respawnkits.remove(kitname);
										getConfig().set("respawnkits", respawnkits);
										saveConfig();
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been removed from the Respawn Kits list.");
									} else 
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", is not a Respawn Kit. Use " + ChatColor.RED + "/ckits respawnkits " + ChatColor.LIGHT_PURPLE + "to see the current kits.");
								}
							}
						} else {
							//If the user has not specified any arguments, display a list of all current loginkits entries.
							if(respawnkits.size() != 0) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current Respawn Kits:");
								sender.sendMessage(respawnkits.toArray(new String[respawnkits.size()]));
							} else
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have no active Respawn Kits. Set one with " + ChatColor.RED + "/ckits respawnkit +<kitname>");
						}
						break;
					case "updater":
						//If updater is specified, toggle the updateEnabled boolean and then set the config file. Finally, inform the user.
						updateEnabled = !updateEnabled;
						getConfig().set("update", updateEnabled);
						saveConfig();
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Updater Enabled: " + ChatColor.RED + ((updateEnabled) ? "True" : "False"));
						break;
					case "current":
						//Reply with the current settings for all available configurable values.
						sender.sendMessage(ChatColor.RED + "--" + ChatColor.LIGHT_PURPLE + "Current Settings" + ChatColor.RED + "--");
						sender.sendMessage(ChatColor.AQUA + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Current Duration: " + ChatColor.RED + cooldownCalc(cooldownDuration));
						sender.sendMessage(ChatColor.AQUA + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "First-Time Login Kits:");
						sender.sendMessage(startkits.toArray(new String[startkits.size()]));
						sender.sendMessage(ChatColor.AQUA + "Login Kits:");
						sender.sendMessage(loginkits.toArray(new String[loginkits.size()]));
						sender.sendMessage(ChatColor.AQUA + "Respawn Kits:");
						sender.sendMessage(respawnkits.toArray(new String[respawnkits.size()]));
						sender.sendMessage(ChatColor.AQUA + "Updater Enabled: " + ChatColor.RED + ((updateEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.RED + "------------------");
						break;
					case "update":
						Updater updater = new Updater(this,47788,this.getFile(),Updater.UpdateType.DEFAULT, false);
						if(updater.getResult() == Updater.UpdateResult.SUCCESS) {
							sender.sendMessage(ChatColor.RED + "CobraKits " + ChatColor.LIGHT_PURPLE + "has been updated to " + ChatColor.RED + updater.getLatestName() + ChatColor.LIGHT_PURPLE + ".");
							updateAvailable = false;
							updateVersion = "";
							updateGameVersion = "";
						} else {
							sender.sendMessage(ChatColor.RED + "CobraKits " + ChatColor.LIGHT_PURPLE + "has failed to update with reason: " + ChatColor.RED + updater.getResult().toString() + ChatColor.LIGHT_PURPLE + ".");
						}
						break;
				}
				saveConfig();
			} else {
				sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("lkit") || cmd.getName().equalsIgnoreCase("kits")) { //List available kits
			listKits(sender, args);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("ckit")) { //Create a kit
			//If the sender is a player, continue on, else provide the console a stern warning.
			if(sender instanceof Player){
				//Parse the args[] into a new Args class object which will find any flags specified by the user.
				Args options = new Args(args);
				//If the player has specified a kitname, continue on.
				if(!options.Arg1().isEmpty()) {
					//As a period is used to denote owner.kitname, kits containing periods are not permitted.
					if(options.Arg1().contains(".")) {
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit names may not contain any periods (\".\"). Please choose a new name.");
						return true;
					}
					//Check the player's permissions, and then create the kit base on the options provided.
					if(sender.hasPermission("cobrakits.create")) {
						createKit((Player)sender, options);
					} else {
						sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
					}
				} else {
					sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Make a kit based on your inventory.");
				}
			} else {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Only a Player can run that command!");
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("ukit")) { //Update a kit
			if(args.length >= 1){
				if(sender.hasPermission("cobrakits.update")) {
					Args options = new Args(args);
					//If the player specified a kitname to update, and has the valid permissions, continue.
					updateKit(sender, options);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Update a kit."); }
			return true;
		} else if(cmd.getName().equalsIgnoreCase("rkit")) { //Rename a kit
			if(args.length >= 2) {
				//If the player specified a kitname to rename, a new name, and has the valid permissions, continue.
				if(sender.hasPermission("cobrakits.rename")) {
					Args options = new Args(args);
					renameKit(sender, options);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/rkit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname] " + ChatColor.WHITE + ": Rename a kit."); }
			return true;
		} else if(cmd.getName().equalsIgnoreCase("dkit")) { //Delete a kit
			if(args.length == 1){
				if(sender.hasPermission("cobrakits.delete")) {
					//If the player has the necessary permissions and has specified a kitname, continue.
					deleteKit(sender, args[0]);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!"); }
			return true;
		} else if(cmd.getName().equalsIgnoreCase("kit")) { //Use or Give a kit
			//Parse the args[] into a new Args class object which will find any flags specified by the user.
			Args options = new Args(args);
			if(!options.Arg1().isEmpty()){
				//If the first argument is not blank, check the permissions of the player, if they have any of the use/give permissions, continue.
				if(sender.hasPermission("cobrakits.use") || sender.hasPermission("cobrakits." + options.Arg1()) || sender.hasPermission("cobrakits.give")) {
				giveKit(sender, options);
			} else {
				sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			}
		} else { sender.sendMessage(ChatColor.RED + "/kit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[username]" + ChatColor.WHITE + ": Use/Give the specified kit."); }
			return true;
		}
		return false;
	}
	
	/*
	 * List Function - Display a list of kits that the player/console has access to use
	 */
	private void listKits(CommandSender sender, String[] args){
		//Create a String list to contain the command response.
		List<String> listReply = new ArrayList<String>();
		//Create String lists to contain all kits the user can access, one for each type of kit so they can be colored and organized.
		List<String> globalKits = new ArrayList<String>();
		List<String> serverKits = new ArrayList<String>();
		List<String> myKits = new ArrayList<String>();
		List<String> availableKits = new ArrayList<String>();
		//Iterate through every Kit in kitList.
		for(Kit entry: kitList) {
			//If they sender is a player, we must check permissions and separate out that player's Personal kits.
			if(sender instanceof Player) {
				//If the player is op or has .useall, filter all Personal, Server, Global, and Available kits.
				if(sender.hasPermission("cobrakits.useall")) {
					if(entry.Owner().equals("Global")) {
						globalKits.add(ChatColor.GREEN + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
					} else if(entry.Owner().equals(sender.getName())){
						myKits.add(ChatColor.GOLD +  entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
					} else if(entry.Owner().equals("Server")) {
						if(sender.hasPermission("cobrakits.*")) {
							serverKits.add(ChatColor.DARK_PURPLE + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
						}
					} else {
						availableKits.add(ChatColor.AQUA + entry.Owner() + "." + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
					}
				//With .use, a player has access to all Global and their own Personal kits, sort them out here.
				} else if(sender.hasPermission("cobrakits.use") && !sender.hasPermission("cobrakits.useall")) {
					if(entry.Owner().equals("Global")) {
						globalKits.add(ChatColor.GREEN + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
					} else if(entry.Owner().equals(sender.getName())){
						myKits.add(ChatColor.GOLD +  entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
					}
				//If the player has a specific kit's permission (cobrakits.kitname) sort it out here.
				} else if(sender.hasPermission("cobrakits." + entry.Owner() + "." + entry.Name()) && !sender.hasPermission("cobrakits.useall")) {
						availableKits.add(ChatColor.AQUA + entry.Owner() + "." + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
								(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
				}
			//If the console ran the command, separate out Server and Global kits.
			} else {
				if(entry.Owner().equals("Server")) {
					serverKits.add(ChatColor.DARK_PURPLE + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
							(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
				} else if(entry.Owner().equals("Global")) {
					globalKits.add(ChatColor.GREEN + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
							(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
				} else {
					availableKits.add(ChatColor.AQUA + entry.Owner() + "." + entry.Name() + (entry.Cost() != null ? " - Cost: " + String.valueOf(entry.Cost().getAmount()) + " " + entry.Cost().getType().toString() : "") +
							(entry.Cooldown() > 0 ? " - Cooldown: " + cooldownCalc(entry.Cooldown()) : ""));
				}
			}
		}
		//Sort all the kit lists a-z
		java.util.Collections.sort(globalKits, Collator.getInstance());
		java.util.Collections.sort(serverKits, Collator.getInstance());
		java.util.Collections.sort(myKits, Collator.getInstance());
		java.util.Collections.sort(availableKits, Collator.getInstance());
		//Add Personal kits to the full kit list, then Server and Global kits. This is to keep the list shown to the player in order of Personal -> Server -> Global -> Other's
		availableKits.addAll(0, globalKits);
		availableKits.addAll(0, serverKits);
		availableKits.addAll(0, myKits);
		
		//If the player has no permission to use any kits, send a permissions error.
		if(availableKits.size() == 0) {
			sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			return;
		}
		
		//Since the chat window can only display 10 lines, reduce the length of the kit list to 8, depending on if a page is specified.
		int page = 0;
		//Attempt to parse out an page number from the arguments. try/catch prevents error if non-numerial characters are entered.
		try {
			page = (args.length > 0) ? Integer.valueOf(args[0]) : 1;
			page = (page == 0) ? 1 : page;
		} catch(NumberFormatException e) {
			sender.sendMessage(ChatColor.DARK_RED + "Pages can only be entered as numbers, try again.");
			return;
		}
		//If list of kits is, when divided by 8 (10 lines, 2 for header/footer), greater than or equal to the page requested minus 1 (page 1 is really page 0, and so on), proceed.
		if((availableKits.size() / 8) >= (page - 1)) {
			//The first kit (an index) to display in the list, is the page number minus 1 multiplied by 8 (page 1 displays kits 0-7, page 2 8-15, etc).
			int firstKit = ((page -1) * 8);
			//The reply is a partial list from availableKits, starting at firstKit, and the end kit index is calculated by the following:
			//If the size of availableKits divided by 8 is less than the page number specified, then there are not enough kits to fill that page with 8 kits.
			//So we return the size of availableKits modularly divided by 8, giving us the remainder. If the size divided by 8 is equal or greater to the page entered
			//we simply return 8 as there are at least enough kits to fill one full page of 8 kits.
			//The result is then added to kitList to provide the ending index of the partial list we need.
			listReply = availableKits.subList(firstKit, firstKit + ((availableKits.size() / 8 < page) ? (availableKits.size() % 8) : 8));
		} else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "There are no kits on that page.");
			return;
		}
		//Add header.
		listReply.add(0, ChatColor.RED + "--" + ChatColor.LIGHT_PURPLE + "Available Kits" + ChatColor.RED + "--");
		//Add footer.
		int maxPage = (int) Math.ceil(availableKits.size() / 8d);
		listReply.add(ChatColor.RED + "-Page " + String.format("%2d", page) + " of " + String.format("%-2d", maxPage) + " -");
		
		//Send the compiled reply to the player
		sender.sendMessage(listReply.toArray(new String[listReply.size()]));
	}
	
	/*
	 * Create Function - Take the player's inventory and store it in the kitList as a new Kit.
	 */
	@SuppressWarnings("deprecation")
	private void createKit(Player player, Args args){
		//Force player data to be saved. The ensures inventory is up-to-date.
		//Without this, recently moved ItemStacks can create duplicates in saved kits or items that vanish when used.
		player.saveData();
		player.updateInventory();
		
		//Retrieve an instance of the player's inventory.
		PlayerInventory inventory = player.getInventory();
		
		//If the -sk/-server flag was specified and the player has permission, create a Server kit.
		if(args.Server() && player.hasPermission("cobrakits.*")) {
			//If the kitList doesn't already have a Server kit with that name, proceed.
			if(!kitList.contains("Server." + args.Arg1())) {
				//add a new Kit to the kitList, specifying the owner (Server), the kitname, and the player's current inventory, as well as Potions and Costif the flag is set.
				kitList.add(new Kit("Server", args.Arg1(),
						inventory.getContents(), inventory.getArmorContents(),
						(args.Potions() ? maxDurationEffects(player.getActivePotionEffects()) : null),
						(args.Cooldown() >= 0 ? args.Cooldown() : 0), args.Cost()));
				SaveKits();
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Server Kit " + ChatColor.AQUA + args.Arg1() + ChatColor.LIGHT_PURPLE + " has been created!");
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "A kit named " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + ", already exists.");
			}
		//If the -g/-global flag was specified and the player has permission, create a Global kit.
		} else if(args.Global() && player.hasPermission("cobrakits.createall")) {
			//If the kitList doesn't already have a Global kit with that name, proceed.
			if(!kitList.contains("Global." + args.Arg1())) {
				//add a new Kit to the kitList, specifying the owner (Global), the kitname, and the player's current inventory, as well as Potions and Cost if the flag is set.
				kitList.add(new Kit("Global", args.Arg1(),
						inventory.getContents(), inventory.getArmorContents(),
						(args.Potions() ? maxDurationEffects(player.getActivePotionEffects()) : null),
						(args.Cooldown() >= 0 ? args.Cooldown() : 0), args.Cost()));
				SaveKits();
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Global Kit " + ChatColor.AQUA + args.Arg1() + ChatColor.LIGHT_PURPLE + " has been created!");
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "A kit named " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + ", already exists.");
			}
		//If the Global flag is not specified, create a Personal kit (a basic .create permission check is done when the command is issued)
		} else {
			//Check the kitList to make sure it doesn't contain a playername.kitname combination already
			if(!kitList.contains(player.getName() + "." + args.Arg1())) {
				//add a new Kit to the kitList, specifying the owner (player's name), the kitname, and the player's current inventory, as well as Potions and Cost if the flag is set.
				kitList.add(new Kit(player.getName(), args.Arg1(),
						inventory.getContents(), inventory.getArmorContents(),
						(args.Potions() ? maxDurationEffects(player.getActivePotionEffects()) : null),
						(args.Cooldown() >= 0 ? args.Cooldown() : 0), args.Cost()));
				SaveKits();
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Personal Kit " + ChatColor.AQUA + args.Arg1() + ChatColor.LIGHT_PURPLE + " has been created!");
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "A kit named " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + ", already exists.");
			}
		}
	}
	
	/*
	 * Update Function - Replace an existing kit with the contents of the player's inventory
	 */
	@SuppressWarnings("deprecation")
	private void updateKit(CommandSender sender, Args args) {
		if(sender instanceof Player){
			String kitname = args.Arg1();
			//Retrieve an instance of the player who send the command.
			Player player = (Player)sender;
			//Force player data to be saved. The ensures inventory is up-to-date.
			//Without this, recently moved ItemStacks can create duplicates in saved kits or items that vanish when used.
			player.saveData();
			player.updateInventory();
			//Retrieve an instance of the player's inventory.
			PlayerInventory inventory = player.getInventory();
			//Check the kitList to see if it contains playername.kitname.
			if(kitList.contains(player.getName() + "." + kitname)) {
				//If it does, cache the current Kit that is in the kitList (at index playername.kitname).
				Kit temp = kitList.get(kitList.indexOf(player.getName() + "." + kitname));
				kitsToRemove.add(temp); //Set the old kit record to be cleaned from the yml file.
				//Set the Kit at the index of playername.kitname to a new Kit object retaining the cached owner and name, and replacing it with the players current inventory.
				kitList.set(kitList.indexOf(player.getName() + "." + kitname), new Kit(temp.Owner(), temp.Name(),
						inventory.getContents(), inventory.getArmorContents(),
						(args.Potions() ? maxDurationEffects(player.getActivePotionEffects()) : temp.Potions()),
						(args.Cooldown() >= 0 ? args.Cooldown() : temp.Cooldown()),
						(args.Cost() != null ? (args.Cost().getTypeId() != 0 ? args.Cost() : null) : temp.Cost())));
				temp = null;
				SaveKits();
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Personal Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been updated successfully.");
			//If we couldn't find a Personal kit and the player has .updateall permissions, attempt to do a search by the full kitname specified.
			} else if (player.hasPermission("cobrakits.updateall") && kitList.contains(kitname)) {
				//If it does, cache the current Kit that is in the kitList (at index kitname).
				Kit temp = kitList.get(kitList.indexOf(kitname));
				kitsToRemove.add(temp); //Set the old kit record to be cleaned from the yml file.
				//Set the Kit at the index of kitname to a new Kit object retaining the cached owner and name, and replacing it with the players current inventory.
				kitList.set(kitList.indexOf(kitname), new Kit(temp.Owner(), temp.Name(),
						inventory.getContents(), inventory.getArmorContents(),
						(args.Potions() ? maxDurationEffects(player.getActivePotionEffects()) : temp.Potions()),
						(args.Cooldown() >= 0 ? args.Cooldown() : temp.Cooldown()),
						(args.Cost() != null ? (args.Cost().getTypeId() != 0 ? args.Cost() : null) : temp.Cost())));
				temp = null;
				SaveKits();
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Global Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been updated successfully.");
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
			}
		}
		else {
			sender.sendMessage(ChatColor.DARK_RED + "Only a Player can update a kit!");
		}
	}
	
	/*
	 * Rename Function - Allows player/console to rename an existing kit from the kitList.
	 */
	private void renameKit(CommandSender sender, Args args) {
		String kitname = args.Arg1();
		String newname = args.Arg2();
		
		//Check the kitList to see if it contains playername.kitname. (sender can be either a player or console. If it is console, it will not be found and skip to the Else)
		if(kitList.contains(sender.getName() + "." + kitname)) {
			if(newname.contains(".")) {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Personal kit names are not allowed to contain periods.");
				return;
			}
			//If it does, cache the current Kit that is in the kitList (at index playername.kitname).
			Kit temp = kitList.get(kitList.indexOf(sender.getName() + "." + kitname));
			kitsToRemove.add(temp); //Set the old kit record to be cleaned from the yml file.
			//Set the Kit at the index of playername.kitname to a new Kit object retaining the cached inventory and owner, and replacing the kitname with playername.newname
			kitList.set(kitList.indexOf(sender.getName() + "." + kitname), new Kit(temp.Owner(), newname, temp.Inventory(), temp.Armor(), temp.Potions(), temp.Cooldown(), temp.Cost()));
			temp = null;
			SaveKits();
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Personal Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been renamed to " + ChatColor.AQUA + newname + ChatColor.LIGHT_PURPLE + ".");
		//If we couldn't find a Personal kit and the player has .renameall permissions, attempt to do a search by the full kitname specified.
		} else if(sender.hasPermission("cobrakits.renameall") && kitList.contains(kitname)) {
			//Cache the current Kit that is in the kitList (at index kitname).
			Kit temp = kitList.get(kitList.indexOf(kitname));
			kitsToRemove.add(temp); //Set the old kit record to be cleaned from the yml file.
			if(args.Global()) { //Change the owner of the kit to Global
				kitList.set(kitList.indexOf(kitname), new Kit("Global", temp.Name(), temp.Inventory(), temp.Armor(), temp.Potions(), temp.Cooldown(), temp.Cost()));
				newname = "Global." + temp.Name();
			} else if (args.Server()) { //Change the owner of the kit to Server
				kitList.set(kitList.indexOf(kitname), new Kit("Server", temp.Name(), temp.Inventory(), temp.Armor(), temp.Potions(), temp.Cooldown(), temp.Cost()));
				newname = "Server." + temp.Name();
			} else if(newname.contains(".")) { //If the new name has a period the kit owner is also being changed.
				//Set the Kit at the index of kitname to a new Kit object retaining the cached inventory, and replacing the owner and kitname with newname
				kitList.set(kitList.indexOf(kitname), new Kit(newname.split("\\.")[0], newname.split("\\.")[1], temp.Inventory(), temp.Armor(), temp.Potions(), temp.Cooldown(), temp.Cost()));
			} else {
				//Set the Kit at the index of kitname to a new Kit object retaining the cached inventory and owner, and replacing the kitname with newname
				kitList.set(kitList.indexOf(kitname), new Kit(temp.Owner(), newname, temp.Inventory(), temp.Armor(), temp.Potions(), temp.Cooldown(), temp.Cost()));
			}
			temp = null;
			SaveKits();
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Global Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been renamed to " + ChatColor.AQUA + newname + ChatColor.LIGHT_PURPLE + ".");
		} else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
		}
	}
	
	/*
	 * Delete Function - Allows players/console to remove an existing kit from the kitList.
	 */
	private void deleteKit(CommandSender sender, String kitname) {
		//If the user is a player, check the kitlist for the playername.kitname combination.
		if((sender instanceof Player) && kitList.contains(sender.getName() + "." + kitname)) {
			//If found, remove the Kit at the index of playername.kitname from kitList.
			kitsToRemove.add(kitList.get(kitList.indexOf(sender.getName() + "." + kitname)));
			kitList.remove(kitList.indexOf(sender.getName() + "." + kitname));
			SaveKits();
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been permanently deleted.");
		//If playername.kitname isnt found, and the sender has .deleteall permission, check for the full kitname.
		} else if(sender.hasPermission("cobrakits.deleteall") && kitList.contains(kitname)) {
			//If found, remove the Kit at the index of kitname from kitList.
			kitsToRemove.add(kitList.get(kitList.indexOf(kitname)));
			kitList.remove(kitList.indexOf(kitname));
			SaveKits();
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been permanently deleted.");
		} else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
		}
	}
	
	/*
	 * Give Function - Allows console to give player's kits, also allows player's to give themselves or other players kits
	 */
	private void giveKit(CommandSender sender, Args options) {
		//Initialize the arguments variables.
		Player target = null;
		String kitname = options.Arg1();
		Boolean silent = options.Silent();
		Boolean concat = options.Concat();
		Boolean global = options.Global();
		if(!options.Arg2().isEmpty()) {
			//If Args2 is not empty, it is the target player. Check to make sure they are online.
			target = getServer().getPlayer(options.Arg2());
			if (target == null) { 
				sender.sendMessage(options.Arg2() + " is not online!");
				return;
			//If the target is online and the sender has give permissions, give the target player the kit.
			} else if(sender.hasPermission("cobrakits.give")) {
				if(kitname.contains("Server.") && !sender.hasPermission("cobrakits.*")) {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist, or you do not have permission to use it. Use "  + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
					return;
				} else if(kitList.contains(kitname)) {
					applyKit(target, kitname, sender, silent, concat);	
					return;
				} else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
					return;
				}
			} else {
				sender.sendMessage("You do not have permission to give Kits.");
				return;
			}
		}
		
		//If no target has been set, check to see if this is a player and then parse and apply kit according to permissions.
		if(target == null) {
			if(sender instanceof Player) {
				if(kitname.contains("Server.") && !sender.hasPermission("cobrakits.*")) {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist, or you do not have permission to use it. Use "  + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
			    } else if(sender.hasPermission("cobrakits.useall") && kitList.contains(kitname)) {
					applyKit((Player)sender, kitname, sender, silent, concat);
				} else if(sender.hasPermission("cobrakits." + kitname) && kitList.contains(kitname)) {
					applyKit(((Player)sender), kitname, sender, silent, concat);
				} else if(sender.hasPermission("cobrakits.use")) {
					if((global && kitList.contains(kitname)) || kitList.contains("Global." + kitname)) {
						applyKit((Player)sender, kitname, sender, silent, concat);
					} else if (kitList.contains(sender.getName() + "." + kitname)) {
						applyKit((Player)sender, sender.getName() + "." + kitname, sender, silent, concat);
					} else {
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist, or you do not have permission to use it. Use "  + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
					}
				} else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
				}
			}
			else {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Only a Player can run that command!");
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private void applyKit(Player target, String kitname, CommandSender sender, Boolean silent, Boolean concat) {
		target.saveData();
		target.updateInventory();
		
		//Retrieve the Kit from the kitList at the index of the specified kitname.
		Kit kit = kitList.get(kitList.indexOf(kitname));
		
		if(sender instanceof Player) {
			if((cooldownEnabled || kit.Cooldown() > 0) && !(sender.hasPermission("cobrakits.cooldown.bypass" ) || sender.hasPermission("cobrakits.cooldown.bypass." + kit.Name()))) {
				//Set the cooldown to the global cooldown value if it is enabled, if not, load the specific kit's cooldown.
				int duration = cooldownEnabled ? (cooldownDuration > 0 ? cooldownDuration : 0) : (kit.Cooldown() > 0 ? kit.Cooldown() : 0);
				
				//Get the current system time in milliseconds.
				long currentTime = System.currentTimeMillis();
				//Check to see if the player and kit name combination are already listed.
				Cooldown cooldown = cooldownList.isOnCooldown(((Player)sender).getName(), kit.Owner() + "." + kit.Name());
				//If the player is not on cooldown for this kit.
				if(cooldown != null) {
					//Check to see of the current time is greater than end time, indicating the cooldown is over.
					if(currentTime > cooldown.endTime) {
						//Remove the cooldown and save the cooldowns file.
						cdToRemove.add(cooldown);
						cooldownList.remove(cooldown);
						SaveCooldowns();
					} else {
						//If the cooldown is still active, get the time remaining and display it to the player.
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "You cannot use that kit for another: " + cooldown.timeRemaining(currentTime));
						return;
					}
				} else {
					//If the player is not on cooldown for this kit, add a cooldown with end time equal to current time + duration in milliseconds.
					long endTime = System.currentTimeMillis() + (duration * 1000);
					//Add this new cooldown to the list and save it.
					cooldownList.add(new Cooldown(((Player)sender).getName(), kit.Owner() + "." + kit.Name(), endTime));
					SaveCooldowns();
				}	
			}
			
			if(kit.Cost() != null && !(target.hasPermission("cobrakits.cost.bypass") || target.hasPermission("cobrakits.cost.bypass." + kit.Name()))) {
				//If the player cannot bypass the cost, check to see if they have at least enough items to afford the kit and then remove them.
				if(((Player)sender).getInventory().containsAtLeast(kit.Cost(), kit.Cost().getAmount())) {
					//Remove the items from their inventory and update it.
					int slot;
					ItemStack stack;
					int stackAmount;
					int itemsLeft = kit.Cost().getAmount();
					while(itemsLeft > 0) {
						slot = ((Player)sender).getInventory().first(kit.Cost().getType());
						stack = ((Player)sender).getInventory().getItem(slot);
						stackAmount = stack.getAmount();
						if(stackAmount <= itemsLeft) {
							((Player)sender).getInventory().clear(slot);
							((Player)sender).updateInventory();
							itemsLeft -= stackAmount;
						} else {
							stackAmount -= itemsLeft;
							((Player)sender).getInventory().setItem(slot, new ItemStack(kit.Cost().getType(), stackAmount));
							((Player)sender).updateInventory();
							itemsLeft = 0;
						}
					}
				} else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "You cannot afford to use this kit. It costs: " + String.valueOf(kit.Cost().getAmount()) + " " + kit.Cost().getType().toString());
					//If the player cannot afford the kit, check to see if the cooldown was already added.
					Cooldown cooldown = cooldownList.isOnCooldown(((Player)sender).getName(), kit.Owner() + "." + kit.Name());
					//If a cooldown exists, remove it.
					if(cooldown != null) {
						cdToRemove.add(cooldown);
						cooldownList.remove(cooldown);
						SaveCooldowns();
					}
					return;
				}
			}
		}
		
		if(concat || concatEnabled){
			//With the concat flag, a player's existing inventory will be preserved, items in the kit will be added to it.
			ItemStack[] inventory = kit.Inventory();
			ItemStack[] armor = kit.Armor();
			//Variables used to remove null values from the inventory array.
			List<ItemStack> contents = new ArrayList<ItemStack>();
			//Variables to indicate how many items were unable to be added due to existing inventory.
			int failedArmor = 0;
			HashMap<Integer, ItemStack> failed = new HashMap<Integer, ItemStack>();
			for (ItemStack entry : armor){
				//For each item in the armor set, check if the player already has armor in that slot, and then replace.
				if(entry != null){
					if(entry.getType().toString().contains("BOOTS")) {
						if (target.getInventory().getBoots() == null) {
							target.getInventory().setBoots(entry);
						} else {
							failed = target.getInventory().addItem(entry);
							failedArmor += failed.size();
							failed.clear();
						}
					} else if(entry.getType().toString().contains("CHEST")) {
						if(target.getInventory().getChestplate() == null) {
							target.getInventory().setChestplate(entry);
						} else {
							failed = target.getInventory().addItem(entry);
							failedArmor += failed.size();
							failed.clear();
						}
					} else if(entry.getType().toString().contains("HELMET")) {
						if (target.getInventory().getHelmet() == null) { 
							target.getInventory().setHelmet(entry);
						} else {
							failed = target.getInventory().addItem(entry);
							failedArmor += failed.size();
							failed.clear();
						}
					} if(entry.getType().toString().contains("LEGGINGS")) {
						if (target.getInventory().getLeggings() ==null) { 
							target.getInventory().setLeggings(entry);
						} else {
							failed = target.getInventory().addItem(entry);
							failedArmor += failed.size();
							failed.clear();
						}
					}
				}
			}
			//Remove null items from the saved kit inventory.
			for (ItemStack entry : inventory){
				if(entry != null) { contents.add(entry); }
			}
			//Apply the kit to the target. addItem returns a HashMap of failed items so we can display failures.
			failed = target.getInventory().addItem(contents.toArray(new ItemStack[contents.size()]));
			if(!failed.isEmpty()){
				sender.sendMessage(ChatColor.LIGHT_PURPLE + String.valueOf(failed.size() + failedArmor) + " items were unable to be added due to a full inventory!");
			}
		} else {
			target.getInventory().setContents(kit.Inventory());
			target.getInventory().setArmorContents(kit.Armor());
			//Set the player's inventory and armor contents.
		}
		//If the silent flag has been set, do not send the apply message to the target player.
		if(!(silent || silentEnabled)) {
			target.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been applied.");
		}
		//Added check here for player logging in previously. If we do not, some code will break the loginDetector code with NPEs.
		if(target.hasPlayedBefore()) {
			//Remove all potion effects from the player.
			Collection<PotionEffect> effects = target.getActivePotionEffects();
			
			if(!(concat || concatEnabled)) {
				for (PotionEffect effect : effects) {
					target.removePotionEffect(effect.getType());
				}
			}
			
			//Apply any kit potion effects
			if(kit.Potions() != null) {
				target.addPotionEffects(kit.Potions());
			}
			
			//Force player to update inventory. Without this sometimes kits will apply but appear invisible.
			target.saveData();
			target.updateInventory();
		}
	}
	
	private List<PotionEffect> maxDurationEffects(Collection<PotionEffect> PotionEffects){
		List<PotionEffect> potionEffects = new ArrayList<PotionEffect>();
		for(PotionEffect effect : PotionEffects) {
			double maxDuration = 8 * 60 * 20 * effect.getType().getDurationModifier(); //The max potion duration is based off 8 minutes multiplied by the DurationModifier of the effect.
			double baseDuration = maxDuration / (8.0 / 3.0); //The base duration for all potions, is the max possible duration multiplied by 8/3.
			if(effect.getDuration() > (8 * 60 * 20)) { //If the duration is beyond 8 minutes, it is a custom duration and will be preserved.
				potionEffects.add(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier()));
			} else if(effect.getAmplifier() == 1) { 
				//If the Amplifier is 1, this is a "II" potion which has the duration of the base, divided by 2.
				potionEffects.add(new PotionEffect(effect.getType(), (int)(baseDuration / 2), 1));
			} else {
				//There is sadly no API way to tell if the potion effect was initiated by a potion of Max (8, usually)) or Base (3, usually) length,
				//So, to determine what the potion duration should be, check to see if the current duration is above the base, if it is, it must be a Max duration potion.
				potionEffects.add(new PotionEffect(effect.getType(), (int)(effect.getDuration() > baseDuration ? maxDuration : baseDuration), effect.getAmplifier()));
			}
		}
		return potionEffects;
	}
	
	//Helper method to calculate the cooldown duration for a kit in minutes/seconds. This is only used for /kits and /lkits.
	private String cooldownCalc(int cooldown) {
		int minutes = cooldown/60;
		int seconds = (minutes > 0) ? (cooldown % 60) : (cooldown);
		String time = (minutes > 0 ? minutes + "m" : "") + (minutes > 0 && seconds > 0 ? " " : "") + (seconds > 0 ? seconds + "s" : "");
		return time;
	}
}
