package us.drome.CobraKits;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/*
 * CobraKits - Coded by TheAcademician - Released under GPLv3
 * @author - TheAcademician@gmail.com
 */

//Enum to store all valid plugin functions.
enum Func { usage, list, create, update, delete, give }

public class CobraKits extends JavaPlugin implements Listener {
	//HashMap to contain kit data. Key is username:kitname as String for permissions
	//Kits are stored in serialized form in a String array, index 0 is inventory contents, 1 is armor contents
	private Map<String, String[]> kitList = new HashMap<String, String[]>();
	//File object to store the kits in serialized form in plugins/CobraKits/kits.bin
	private File kitsBin;
	//Variables to store config file values.
	private boolean silentEnabled = false;
	private boolean concatEnabled = false;
	private boolean cooldownEnabled = false;
	private int cooldownDuration = 0;
	private String startkit = "";
	//Value to indicate whether the player is still on cooldown
	private boolean onCooldown = false;
	
	public void onEnable(){
		getLogger().info(getDescription().getName() + " Version " + getDescription().getVersion() + " Is Loading...");
		try{
			//Save the default configuration.
			saveDefaultConfig();
			getConfig().options().header("Cobra Kits Configuration - v.07 - by TheAcademician");
			//Load values from the config.yml
			silentEnabled = getConfig().getBoolean("silent.always");
			concatEnabled = getConfig().getBoolean("concat.always");
			cooldownEnabled = getConfig().getBoolean("cooldown.enabled");
			cooldownDuration = getConfig().getInt("cooldown.duration");
			startkit = getConfig().getString("startkit");
			
			kitsBin = new File(getDataFolder() + File.separator + "kits.bin");
			//If the plugins/CobraKits/ directory does not exist, create it.
			if(!kitsBin.getParentFile().exists()){
				kitsBin.getParentFile().mkdirs();	
			}
			//If the kits.bin file exists, load it via SLAPI into the kitList HashMap. If not, create the file.
			if(!kitsBin.createNewFile()){
				kitList = SLAPI.load(kitsBin.getPath());
				getLogger().info("CobraKits Has Loaded " + kitList.size() + " Kits!");
			}
			
			if(!startkit.isEmpty() && !kitList.containsKey(startkit)) {
				startkit = "";
				getLogger().info(getDescription().getName() + " could not find the Start Kit, clearing it.");
			}
			getServer().getPluginManager().registerEvents(this, this);
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	@EventHandler
	public void firstLoginDetector(PlayerLoginEvent login) {
		if(!login.getPlayer().hasPlayedBefore() && startkit != "") {
			giveKit(login.getPlayer(), new Args(new String[]{startkit, "-silent"}));
		}
	}
	
	@EventHandler
	public void kitSignCreateDetector(SignChangeEvent sign) {
		if(sign.getLine(0).equalsIgnoreCase("[COBRAKIT]") || sign.getLine(0).equalsIgnoreCase("[KIT]")) {
			if(sign.getPlayer().hasPermission("cobrakits.sign.create")) {
				if(kitList.containsKey(sign.getLine(2))) {
					sign.setLine(0, ChatColor.RED + "[COBRAKIT]");
					sign.setLine(1, "");
					sign.setLine(2, ChatColor.DARK_PURPLE + sign.getLine(2));
					sign.setLine(3, "");
				} else {
					for(int x=0; x<4; x++) {
						sign.setLine(x, "");
					}
					sign.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + sign.getLine(2) + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
				}
			} else {
				for(int x=0; x<4; x++) {
					sign.setLine(x, "");
				}
				sign.getPlayer().sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			}
		}
	}
	
	public void onDisable(){
		try{
			//Save the config file.
			saveConfig();
			//Save the kitList to the kits.bin file via SLAPI and send unload message
			SLAPI.save(kitList, kitsBin.getPath());
			getLogger().info(getDescription().getName() + " Version " + getDescription().getVersion() + " Has Been Unloaded.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void helpInfo(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "/lkit or /kits " + ChatColor.WHITE + ": List all kits that you can use.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Make a kit based on your inventory.");
		sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname]" + ChatColor.WHITE + ": Update or rename a kit.");
		sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!");
		sender.sendMessage(ChatColor.RED + "/kit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[username]" + ChatColor.WHITE + ": Use/Give the specified kit.");
	}
	
	private void usageInfo(CommandSender sender){
		sender.sendMessage("- " + ChatColor.RED + "CobraKits" + ChatColor.WHITE + " - v" + getDescription().getVersion() + " - Developed by " + ChatColor.LIGHT_PURPLE + "TheAcademician" + ChatColor.WHITE + ".");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "help" + ChatColor.WHITE + ": List all CobraKits commands.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "current" + ChatColor.WHITE + ": List current settings.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "cooldown" + ChatColor.WHITE + ": Toggle kit cooldowns.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "duration" + ChatColor.AQUA+ " [ticks]" + ChatColor.WHITE + ": Set cooldown in seconds.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "silent" + ChatColor.WHITE + ": Toggles silent kit give.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.LIGHT_PURPLE + "concat" + ChatColor.WHITE + ": Toggles concatenation on give.");
	}
	
	private void saveKits(){
		try{
			SLAPI.save(kitList, kitsBin.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("ckits")) { //Usage info or make config changes
			if(args.length == 0){
				usageInfo(sender);
			} else if(args[0].toLowerCase() == "help") {
				helpInfo(sender);
			} else if(sender.hasPermission("cobrakits.*")) {
				switch (args[0].toLowerCase()){
					case "cooldown":
						cooldownEnabled = !cooldownEnabled;
						getConfig().set("cooldown.enabled", cooldownEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						break;
					case "duration":
						if(args.length > 1){
							cooldownDuration = Integer.valueOf(args[1]) * 20;
							getConfig().set("cooldown.duration", cooldownDuration);
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldown Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						} else {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						}
						break;
					case "silent":
						silentEnabled = !silentEnabled;
						getConfig().set("silent.always", silentEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						break;
					case "concat":
						concatEnabled = !concatEnabled;
						getConfig().set("concat.always", concatEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						break;
					case "startkit":
						if(args.length > 1) {
							if(kitList.containsKey(args[0])) {
								startkit = args[0];
								getConfig().set("startkit", startkit);
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Start Kit: " + ChatColor.RED + startkit);
							}
							else {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + args[0] + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
							}
						}
					case "current":
						sender.sendMessage(ChatColor.RED + "--" + ChatColor.LIGHT_PURPLE + "Current Settings" + ChatColor.RED + "--");
						sender.sendMessage(ChatColor.AQUA + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Current Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						sender.sendMessage(ChatColor.AQUA + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.RED + "------------------");
						break;
				}
				saveConfig();
			} else {
				sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			}
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("ckit")) { //Create a kit
			if(args.length == 1){
				if(sender instanceof Player){
					if(sender.hasPermission("cobrakits.create")){
						createKit((Player)sender, args[0]);
					} else {
						sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
					}
				} else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "Only a Player can run that command!");
				}
			} else { sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Make a kit based on your inventory."); }
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("ukit")) { //Update a kit
			if(args.length !=0 && args.length <= 2){
				if(sender.hasPermission("cobrakits.update") || sender.hasPermission("cobrakits.updateall")){
					updateKit(sender, args);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname]" + ChatColor.WHITE + ": Update or rename a kit."); }
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("lkit") || cmd.getName().equalsIgnoreCase("kits")) { //List available kits
			listKits(sender);
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("dkit")) { //Delete a kit
			if(args.length == 1){
				if(sender.hasPermission("cobrakits.delete") || sender.hasPermission("cobrakits.deleteall")){
					deleteKit(sender, args[0]);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!"); }
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("kit")) { //Use or Give a kit
			Args options = new Args(args);
			if(!options.Arg1().isEmpty()){
				if (sender.hasPermission("cobrakits.use") || sender.hasPermission("cobrakits.useall")
					|| sender.hasPermission("cobrakits." + options.Arg1()) || sender.hasPermission("cobrakits.give")) {
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
	private void listKits(CommandSender sender){
		//Create a String list to contain the command response.
		List<String> listReply = new ArrayList<String>();
		//Add header.
		listReply.add(ChatColor.RED + "--" + ChatColor.LIGHT_PURPLE + "Available Kits" + ChatColor.RED + "--");
		for(Map.Entry<String, String[]> entry : kitList.entrySet()){
			//For each entry in the kitList HashMap, if player has permission to list entry, add it to listReply 
			if(sender.hasPermission("cobrakits.useall")){
				listReply.add(ChatColor.AQUA + entry.getKey());
			} else if((sender instanceof Player)){
				if(sender.hasPermission("cobrakits.use") && entry.getKey().split(":")[0].equals(sender.getName())){
					listReply.add(ChatColor.AQUA + entry.getKey().split(":")[1]);
				} else if (sender.hasPermission("cobrakits." + entry.getKey())){
					listReply.add(ChatColor.AQUA + entry.getKey());
				}
			}
		}
		listReply.add(ChatColor.RED + "---------------");
		//If the player didn't have permission to list any kits, send a permission error and return;
		if(listReply.size() == 2 && kitList.size() != 0){
			sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			return;
		}
		//Send the compiled reply to the player
		sender.sendMessage(listReply.toArray(new String[listReply.size()]));
	}
	
	/*
	 * Create Function - Take the player's inventory and store it in the kitList, using their username and kitname as the Key
	 */
	private void createKit(Player player, String kitname){
		PlayerInventory inventory = player.getInventory();
		//Force player data to be saved. The ensures inventory is up-to-date.
		//Without this, recently moved ItemStacks can create duplicates in saved kits.
		player.saveData();
		//If the kitList does not already contain the username:kitname combination, serialize the player's inventory and add it to kitList
		if(!kitList.containsKey(player.getName() + ":" + kitname)){
			kitList.put(player.getName() + ":" + kitname, new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
			player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been created successfully.");
		} else {
			player.sendMessage(ChatColor.LIGHT_PURPLE + "The kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", already exists.");
		}
		saveKits(); //Save the kitlist to file
	}
	
	/*
	 * Update Function - Replace an existing kit with the contents of the player's inventory
	 */
	private void updateKit(CommandSender sender, String[] args) {
		String kitname = args[0];
		String newname = (args.length != 1) ? args[1] : "";
		if(!newname.isEmpty() && sender.hasPermission("cobrakits.updateall")) {
			if(kitList.containsKey(kitname)) {
				kitList.put(newname, kitList.get(kitname));
				kitList.remove(kitname);
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been renamed to " + ChatColor.AQUA + newname + ChatColor.LIGHT_PURPLE + ".");
			}
			else {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
			}
			return;
		}
		if(sender instanceof Player){
			Player player = (Player)sender;
			PlayerInventory inventory = player.getInventory();
			//Force player data to be saved. The ensures inventory is up-to-date.
			//Without this, recently moved ItemStacks can create duplicates in saved kits.
			player.saveData();
			//If the kitList contains the username:kitname combination, serialize the player's inventory and add it to the kitList
			if(kitList.containsKey(player.getName() + ":" + kitname)){
				kitList.put(player.getName() + ":" + kitname, new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been updated successfully.");
			}
			else if(player.hasPermission("cobrakits.updateall") && kitList.containsKey(kitname)){
				kitList.put(kitname, new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been updated successfully.");
			}
			else{
				player.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
			}
		}
		else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Only a Player can update a kit! To rename a kit you must specify a new name.");
		}
	}
	
	/*
	 * Delete Function - Allows players/console to remove an existing kit from the kitList HashMap
	 */
	private void deleteKit(CommandSender sender, String kitname) {
		//If the kitList contains the username:kitname combination, remove it from the kitList.
		if((sender instanceof Player) && kitList.containsKey(sender.getName() + ":" + kitname)){
			kitList.remove(sender.getName() + ":" + kitname);
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been permanently deleted.");
		}
		//Similar to above, but if user has deleteall permissions, assume arg contains full username:kitname syntax
		else if(sender.hasPermission("cobrakits.deleteall") && kitList.containsKey(kitname)) {
			kitList.remove(kitname);
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been permanently deleted.");
		}
		else{
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
		}
	}
	
	/*
	 * Give Function - Allows console to give player's kits, also allows player's to give themselves or other players kits
	 */
	private void giveKit(CommandSender sender, Args options) {
		Player target = null;
		String kitname = options.Arg1();
		Boolean silent = options.Silent();
		Boolean concat = options.Concat();
		if(!options.Arg2().isEmpty()) {
			//If Args2 is not empty, it is the target player. Check to make sure they are online.
			target = getServer().getPlayer(options.Arg2());
			if (target == null) { 
				sender.sendMessage(options.Arg2() + " is not online!");
				return;
			//If the target is online and the sender has give permissions, give the target player the kit.
			} else if (sender.hasPermission("cobrakits.give") && kitList.containsKey(kitname)) {
				applyKit(target, kitname, sender, silent, concat);	
			}
		}
		
		//If no target has been set, check to see if this is a player and then parse and apply kit according to permissions.
		if(target == null) {
			if(sender instanceof Player) {
				//If the cooldown setting is enabled, and were are not onCooldown, then run a delayed task for the cooldown duration.
				if(cooldownEnabled && !sender.hasPermission("cobrakits.cooldown.bypass")) {
					if(!onCooldown) {
						onCooldown = true;
						getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
							@Override
							public void run() {
								onCooldown = false;
							}
						}, Long.valueOf(cooldownDuration));
					} else {
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "You can only use that command every " + (cooldownDuration/20) + " seconds.");
						return;
					}
				}
				
				//If the player has permission to use any kit they have created, search for the kit and apply it.
				if ((sender.hasPermission("cobrakits.use") || sender.hasPermission("cobrakits.useall")) && kitList.containsKey(((Player)sender).getName() + ":" + kitname)){
					applyKit(((Player)sender),((Player)sender).getName() + ":" + kitname, sender, silent, concat);
				//If the player has permissions to use any kit, search for the kit and apply it.
				} else if(sender.hasPermission("cobrakits.useall") && kitList.containsKey(kitname)) {
					applyKit(((Player)sender), kitname, sender, silent, concat);
				//If the player has permission to a specific kit, search for it and apply it.
				} else if (sender.hasPermission("cobrakits." + kitname) && kitList.containsKey(kitname)){
					applyKit(((Player)sender), kitname, sender, silent, concat);
				//If no matching kit can be found, notify the player.
				} else {
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
				}
			}
			else {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "Only a Player can run that command!");
			}
		} else {
				sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");				
		}
	}
	
	private void applyKit(Player target, String kitname, CommandSender sender, Boolean silent, Boolean concat) {
		//Create a String array and fill it with the matching serialized contents from kitList
		String[] kitInven = kitList.get(kitname);
		if(concat || concatEnabled){
			//With the concat flag, a player's existing inventory will be preserved, items in the kit will be added to it.
			ItemStack[] inventory = Serialize.fromBase64(kitInven[0]);
			ItemStack[] armor = Serialize.fromBase64(kitInven[1]);
			//Variables used to remove null values from the inventory array.
			List<ItemStack> contents = new ArrayList<ItemStack>();
			//Variables to indicate how many items were unable to be added due to existing inventory.
			int failedArmor = 0;
			HashMap failed = null;
			for (ItemStack entry : armor){
				//For each item in the armor set, check if the player already has armor in that slot, and then replace.
				if(entry != null){
					if(entry.getType().toString().contains("BOOTS") && target.getInventory().getBoots() != null)
						{ target.getInventory().setBoots(entry); } else { failedArmor++; }
					if(entry.getType().toString().contains("CHEST") && target.getInventory().getChestplate() != null)
						{ target.getInventory().setChestplate(entry); } else { failedArmor++; }
					if(entry.getType().toString().contains("HELMET") && target.getInventory().getHelmet() != null)
						{ target.getInventory().setHelmet(entry); } else { failedArmor++; }
					if(entry.getType().toString().contains("LEGGINGS") && target.getInventory().getLeggings() !=null)
						{ target.getInventory().setLeggings(entry); } else { failedArmor++; }
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
			//Set the player's inventory and armor contents by de-serializing the 0 and 1 indexes of the kitInven array
			target.getInventory().setContents(Serialize.fromBase64(kitInven[0]));
			target.getInventory().setArmorContents(Serialize.fromBase64(kitInven[1]));
		}
		//If the silent flag has been set, do not send the apply message to the target player.
		if(!(silent || silentEnabled)) {
			target.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been applied.");
		}
	}
}

/*
 *  Args Class
 *  Parses out the arguments provided to the ckit command into a more usable format in the form of public properties.
 */
class Args{
	//Private non-changable variables to hold the parsed values.
	private String arg1 = "";
	private String arg2 = "";
	private Boolean concat = false;
	private Boolean silent = false;
	//Public properties to access the arguments.
	public String Arg1() { return arg1; }
	public String Arg2() { return arg2; }
	public Boolean Concat() { return concat; }
	public Boolean Silent() { return silent; }
	
	public Args(String[] args){
		if(args.length > 0){
			for(String entry : args){
				//If the entry is a flag, determine which it is and set the Boolean values.
				if(isFlag(entry)) {
					if(entry.contentEquals("-c") || entry.contentEquals("-concat")) { concat = true; }
					else if (entry.contentEquals("-s") || entry.contentEquals("-silent")) { silent = true; }
					//If the entry is NOT a flag, and arg1 is empty, set it. Repeat for arg2.
				} else if (arg1.isEmpty()) { arg1 = entry; }
				else if (arg2.isEmpty()) { arg2 = entry; }
			}
		}
	}
	
	private boolean isFlag(String arg){
		List<String> flags = Arrays.asList("-c", "-concat", "-s", "-silent");
		//Compare each valid flag against the provided string.
		for(String entry : flags){
			if(arg.equalsIgnoreCase(entry)){ return true; }
		}
		return false;
	}
}