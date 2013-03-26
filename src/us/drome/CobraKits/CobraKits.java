package us.drome.CobraKits;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/*
 * CobraKits - Coded by TheAcademician - Released under GPLv3
 * @author - TheAcademician@gmail.com
 */

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
	//ArrayList to temporarily store player's names that are on cooldown.
	private ArrayList<String> cooldownList = new ArrayList<String>();
	
	public void onEnable(){
		getLogger().info(getDescription().getName() + " Version " + getDescription().getVersion() + " Is Loading...");
		try{
			//Save the default configuration.
			saveDefaultConfig();
			getConfig().options().header("Cobra Kits Configuration - v" + getDescription().getVersion() + " - by TheAcademician");
			//Load values from the config.yml
			silentEnabled = getConfig().getBoolean("silent.always");
			concatEnabled = getConfig().getBoolean("concat.always");
			cooldownEnabled = getConfig().getBoolean("cooldown.enabled");
			cooldownDuration = getConfig().getInt("cooldown.duration");
			startkit = getConfig().getString("startkit");
			
			kitsBin = new File(getDataFolder() + File.separator + "kits.bin");
			//If the plugins/CobraKits/ directory does not exist, create it.
			if(!kitsBin.getParentFile().exists()){
				getLogger().info("CobraKits didn't find a config file. Creating it for you!");
				kitsBin.getParentFile().mkdirs();	
			}
			
			//If the kits.bin file exists, load it via SLAPI into the kitList HashMap. If not, create the file.
			if(!kitsBin.createNewFile()){
				kitList = SLAPI.load(kitsBin.getPath());
				getLogger().info("CobraKits Has Loaded " + kitList.size() + " Kits!");
			}
			
			//If the startkit is set in the config, try to find it in the kitList.
			if(startkit != null && !kitList.containsKey(startkit.replace(":", ""))) {
				startkit = "";
				getConfig().set("startkit", "");
				getLogger().info(getDescription().getName() + " could not find a valid Start Kit, clearing it.");
			}
			
			//Set this class up to handle events.
			getServer().getPluginManager().registerEvents(this, this);
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	
	@EventHandler
	public void firstLoginDetector(PlayerLoginEvent login) {
		//Detect if a player is new to the server, and give them the startkit silently if it is set.
		if(!login.getPlayer().hasPlayedBefore() && kitList.containsKey(startkit.replace(":", ""))) {
			applyKit(login.getPlayer(), startkit, login.getPlayer(), true, false);
		}
	}
	
	@EventHandler
	public void kitSignCreateDetector(SignChangeEvent sign) {
		//When a sign is created, if it matches the proper syntax, check permissions and see if the kitname is valid.
		if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[COBRAKIT]") || ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[KIT]")) {
			if(sign.getPlayer().hasPermission("cobrakits.sign.create")) {
				if(kitList.containsKey(sign.getLine(2)) && !sign.getLine(2).contains(":")) {
					//If the kit is valid, we format the sign to show the user it is enabled.
					if(sign.getLine(0).equalsIgnoreCase("[COBRAKIT]"))
						sign.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + "[COBRAKIT]");
					if(sign.getLine(0).equalsIgnoreCase("[KIT]"))
						sign.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + "[KIT]");
					sign.setLine(1, "");
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
					+ ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available Global kits, marked by " + ChatColor.GREEN + "*" + ChatColor.AQUA + ":");
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
	
	@EventHandler
	public void kitSignUseDetector(PlayerInteractEvent e) {
		//When a player interacts with a block, narrow down whether it is a kit sign or not.
		Block b = e.getClickedBlock();
		if(b == null) //If the player actually interacted with something continue on.
			return;
		if(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
			//If the block interacted by the player is a sign of either type, continue.
			Sign sign = (Sign)b.getState();
			if(ChatColor.stripColor(sign.getLine(0)).equals("[COBRAKIT]") || ChatColor.stripColor(sign.getLine(0)).equals("[KIT]")) {
				//If the kit sign matches the required syntax, determine if the player has permission to use it.
				if(e.getPlayer().hasPermission("cobrakits.sign.use")) {
					//Check the kitList to see if it contains the kitname, after formatting is removed.
					if(kitList.containsKey(ChatColor.stripColor(sign.getLine(2)))) {
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
						e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + sign.getLine(2) + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
					}
				} else {
					e.getPlayer().sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
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
	
	//Method that displays the /ckits help command output which details usage info for the various commands.
	private void helpInfo(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "/lkit or /kits " + ChatColor.WHITE + ": List all kits that you can use.");
		sender.sendMessage(ChatColor.RED + "/ckit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Make a kit based on your inventory.");
		sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname]" + ChatColor.WHITE + ": Update or rename a kit.");
		sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!");
		sender.sendMessage(ChatColor.RED + "/kit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[username]" + ChatColor.WHITE + ": Use/Give the specified kit.");
	}
	
	//Method that displays the usage info when a player runs the /ckits command with no options.
	private void usageInfo(CommandSender sender){
		sender.sendMessage("- " + ChatColor.RED + "CobraKits" + ChatColor.WHITE + " - v" + getDescription().getVersion() + " - Developed by " + ChatColor.LIGHT_PURPLE + "TheAcademician" + ChatColor.WHITE + ".");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "help" + ChatColor.WHITE + ": List all CobraKits commands.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "current" + ChatColor.WHITE + ": List current settings.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "cooldown" + ChatColor.WHITE + ": Toggle kit cooldowns.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "duration" + ChatColor.AQUA+ " [ticks]" + ChatColor.WHITE + ": Set cooldown in seconds.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "silent" + ChatColor.WHITE + ": Toggles silent kit give.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "concat" + ChatColor.WHITE + ": Toggles concatenation on give.");
		sender.sendMessage(ChatColor.RED + "/ckits " + ChatColor.LIGHT_PURPLE + "startkit" + ChatColor.WHITE + ": Set the first logon kit.");
	}
	
	private void saveKits(){
		try{
			//Serialize the kitList with SLAPI and save it to kits.bin.
			SLAPI.save(kitList, kitsBin.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("ckits")) { //Usage info or make config changes
			if(args.length == 0){
				usageInfo(sender);
			} else if(args[0].equalsIgnoreCase("help")) {
				//If the help option is specified, display help info for the various commands.
				helpInfo(sender);
			} else if(sender.hasPermission("cobrakits.*")) {
				switch (args[0].toLowerCase()){
					case "cooldown":
						//If cooldown is specified, toggle the cooldownEnabled boolean in memory and set the config option, then inform the player.
						cooldownEnabled = !cooldownEnabled;
						getConfig().set("cooldown.enabled", cooldownEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						break;
					case "duration":
						//If duration is specified, set the variable to the amount of time specified * 20 to convert to ticks. Set config values and send message.
						if(args.length > 1){
							cooldownDuration = Integer.valueOf(args[1]) * 20;
							getConfig().set("cooldown.duration", cooldownDuration);
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Cooldown Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						} else {
							sender.sendMessage(ChatColor.LIGHT_PURPLE + "Current Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						}
						break;
					case "silent":
						//If silent is specified, toggle the silentEnabled boolean and then set the config file. Finally, inform the user.
						silentEnabled = !silentEnabled;
						getConfig().set("silent.always", silentEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						break;
					case "concat":
						//If concat is specified, toggle the concatEnabled boolean and then set the config file. Finally, inform the user.
						concatEnabled = !concatEnabled;
						getConfig().set("concat.always", concatEnabled);
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						break;
					case "startkit":
						//If startkit is specified, check if the kit specified is valid and then set the string value and change config. Inform the user.
						if(args.length > 1) {
							if(kitList.containsKey(args[1])) {
								startkit = args[1];
								getConfig().set("startkit", startkit);
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "Start Kit: " + ChatColor.RED + startkit);
								break;
							}
							else {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "The specified kit, " + ChatColor.AQUA + args[1] + ChatColor.LIGHT_PURPLE + ", does not exist. Use " + ChatColor.RED + "/lkit or /kits " + ChatColor.LIGHT_PURPLE + "to see available kits.");
							}
						//If no kitname was provided, set the startkit to a blank string, disabling it.
						} else {
							getConfig().set("startkit", "");
						}
					case "current":
						//Reply with the current settings for all available configurable values.
						sender.sendMessage(ChatColor.RED + "--" + ChatColor.LIGHT_PURPLE + "Current Settings" + ChatColor.RED + "--");
						sender.sendMessage(ChatColor.AQUA + "Cooldowns Enabled: " + ChatColor.RED + ((cooldownEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Current Duration: " + ChatColor.RED + String.valueOf(cooldownDuration / 20) + " seconds");
						sender.sendMessage(ChatColor.AQUA + "Concat Kits Enabled: " + ChatColor.RED + ((concatEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Silent Kits Enabled: " + ChatColor.RED + ((silentEnabled) ? "True" : "False"));
						sender.sendMessage(ChatColor.AQUA + "Start Kit: " + ChatColor.RED + getConfig().getString("startkit"));
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
			//If the sender is a player, continue on, else provide usage information.
			if(sender instanceof Player){
				//Parse the provided options into the custom Args class.
				Args options = new Args(args);
				//If the player has specified a kitname, continue on.
				if(!options.Arg1().isEmpty()) {
					//Check the player's permissions, and then create the kit base on the options provided.
					if(sender.hasPermission("cobrakits.create") || sender.hasPermission("cobrakits.createall")) {
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
		}
		else if(cmd.getName().equalsIgnoreCase("ukit")) { //Update a kit
			if(args.length !=0 && args.length <= 2){
				if(sender.hasPermission("cobrakits.update") || sender.hasPermission("cobrakits.updateall")){
					//If the player specified at least a kitname to update, and has the valid permissions, continue.
					updateKit(sender, args);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/ukit " + ChatColor.AQUA + "[kitname] " + ChatColor.BLUE + "[newname]" + ChatColor.WHITE + ": Update or rename a kit."); }
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("lkit") || cmd.getName().equalsIgnoreCase("kits")) { //List available kits
			listKits(sender, args);
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("dkit")) { //Delete a kit
			if(args.length == 1){
				if(sender.hasPermission("cobrakits.delete") || sender.hasPermission("cobrakits.deleteall")){
					//If the player has the necessary permissions and has specified at least a kitname, continue.
					deleteKit(sender, args[0]);
				} else {
					sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
				}
			} else { sender.sendMessage(ChatColor.RED + "/dkit " + ChatColor.AQUA + "[kitname]" + ChatColor.WHITE + ": Deletes this kit. " + ChatColor.RED + "This is permanent!!!"); }
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("kit")) { //Use or Give a kit
			//Parse the provided options into the custom Args class.
			Args options = new Args(args);
			if(!options.Arg1().isEmpty()){
				//If the first argument is not blank, check the permissions of the player, if they have any of the use/give permissions, continue.
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
	private void listKits(CommandSender sender, String[] args){
		//Create a String list to contain the command response.
		List<String> listReply = new ArrayList<String>();
		//Create a String list to contain all kits the user can access.
		List<String> availableKits = new ArrayList<String>();
		for(Map.Entry<String, String[]> entry : kitList.entrySet()){
			//For each entry in the kitList HashMap, if player has permission to list entry, add it to listReply 
			if(sender instanceof Player) {
				if(sender.hasPermission("cobrakits.use") && entry.getKey().split(":")[0].equals(sender.getName())){
					//At each level of permission we check to see if the key is missing a ":", making it a global kit and viewable by anyone with any .use type permission.
					if(!entry.getKey().contains(":"))
						availableKits.add(ChatColor.GREEN + "*" + ChatColor.AQUA + ":" + entry.getKey());
					else
						availableKits.add(ChatColor.AQUA + ":" + entry.getKey().split(":")[1]);
				} else if (sender.hasPermission("cobrakits." + entry.getKey())){
					if(!entry.getKey().contains(":"))
						availableKits.add(ChatColor.GREEN + "*" + ChatColor.AQUA + ":" + entry.getKey());
					else
						availableKits.add(ChatColor.AQUA + entry.getKey());
				} else if (sender.hasPermission("cobrakits.useall")) {
					if(!entry.getKey().contains(":"))
						availableKits.add(ChatColor.GREEN + "*" + ChatColor.AQUA + ":" + entry.getKey());
					else
						availableKits.add(ChatColor.AQUA + entry.getKey());
				}
			}
			else { //If the console calls this command, they can view all kits by full name.
				if(!entry.getKey().contains(":"))
					availableKits.add(ChatColor.GREEN + "*" + ChatColor.AQUA + ":" + entry.getKey());
				else
					availableKits.add(ChatColor.AQUA + entry.getKey());
			}
		}
		//If the player didn't have permission to list any kits, send a permission error and return;
		if(availableKits.size() == 0 && kitList.size() != 0){
			sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do that.");
			return;
		}
		//At this point we should have a full list of kits to display. Let's sort it.
		java.util.Collections.sort(availableKits, Collator.getInstance());
		//Since the chat window can only display 10 lines, reduce the length of the kit list to 8, depending on if a page is specified.
		int page = 0;
		try {
			page = (args.length > 0) ? Integer.valueOf(args[0]) : 1;
		} catch(NumberFormatException e) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "There are no kits on that page.");
			return;
		}
		if((availableKits.size() / 8) >= (page - 1)) {
			int firstKit = ((page -1) * 8);
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
	 * Create Function - Take the player's inventory and store it in the kitList, using their username and kitname as the Key
	 */
	private void createKit(Player player, Args args){
		PlayerInventory inventory = player.getInventory();
		//Force player data to be saved. The ensures inventory is up-to-date.
		//Without this, recently moved ItemStacks can create duplicates in saved kits.
		player.saveData();
		
		//If the -g/-global flag was specified, we try to add a Global kit, otherwise, we create a Personal one.
		if(args.Global() && player.hasPermission("cobrakits.createall")) {
			//If the kitlist does not already contain a global kit by this name, serialize the player's inventory and add it to the kitList
			if(!kitList.containsKey(args.Arg1().replace(":", ""))) {
				//Using .replace() here to remove any ":" from the key name, preventing issues with parsing the name in the future.
				kitList.put(args.Arg1().replace(":", ""), new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + " has been created successfully.");
				saveKits(); //Save the kitlist to file
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "The kit, " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + ", already exists.");
			}
		} else {
			//If the kitList does not already contain the username:kitname combination, serialize the player's inventory and add it to kitList
			if(!kitList.containsKey(player.getName() + ":" + args.Arg1().replace(":", ""))){
				kitList.put(player.getName() + ":" + args.Arg1().replace(":", ""), new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + " has been created successfully.");
				saveKits(); //Save the kitlist to file
			} else {
				player.sendMessage(ChatColor.LIGHT_PURPLE + "The kit, " + ChatColor.AQUA + args.Arg1().replace(":", "") + ChatColor.LIGHT_PURPLE + ", already exists.");
			}
		}
	}
	
	/*
	 * Update Function - Replace an existing kit with the contents of the player's inventory
	 */
	private void updateKit(CommandSender sender, String[] args) {
		//Set the kitname to the first argument, newname, if it exists, to the second.
		String kitname = args[0];
		String newname = (args.length != 1) ? args[1] : "";
		if(!newname.isEmpty() && sender.hasPermission("cobrakits.updateall")) {
			//If a new name is specified, and the user has updateall permissions, add the kit back to the kitList with a new name and remove the old one.
			//This command is available to the console.
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
				//If the kit specified was created by the player, find and update it.
				kitList.put(player.getName() + ":" + kitname, new String[]{Serialize.toBase64(inventory.getContents()), Serialize.toBase64(inventory.getArmorContents())});
				player.sendMessage(ChatColor.LIGHT_PURPLE + "Kit " + ChatColor.AQUA + kitname + ChatColor.LIGHT_PURPLE + " has been updated successfully.");
			}
			else if(player.hasPermission("cobrakits.updateall") && kitList.containsKey(kitname)){
				//If the kit can't be found by username, assume the kitname provided is the full name and attempt to find and update it.
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
		//This command is available to the console.
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
		//Fill in the arguments variables.
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
			} else if (sender.hasPermission("cobrakits.give") && kitList.containsKey(kitname)) {
				applyKit(target, kitname, sender, silent, concat);	
			}
		}
		
		//If no target has been set, check to see if this is a player and then parse and apply kit according to permissions.
		if(target == null) {
			if(sender instanceof Player) {
				//If the cooldown setting is enabled, and the user is not on cooldown, then run a delayed task for the cooldown duration.
				final Player player = (Player)sender;
				if(cooldownEnabled && !player.hasPermission("cobrakits.cooldown.bypass")) {
					//If the player has the bypass permission we dont apply the cooldown.
					if(!cooldownList.contains(player.getName())) {
						//Otherwise, check if their name is in the cooldownList, if they are tell them they are on cooldown and how long they need to wait.
						cooldownList.add(player.getName());
						//Add the player to the cooldownList and start up the task, to last as much time as is specified by cooldownDuration.
						getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
							@Override
							public void run() {
								cooldownList.remove(player.getName());
							}
						}, Long.valueOf(cooldownDuration));
					} else {
						player.sendMessage(ChatColor.LIGHT_PURPLE + "You can only use that command every " + (cooldownDuration/20) + " seconds.");
						return;
					}
				}

				//If the player has permission to use any kit they have created, search for the kit and apply it. We also check for Global kits here.
				if (sender.hasPermission("cobrakits.use") || sender.hasPermission("cobrakits.useall")){
					if(!global && kitList.containsKey(((Player)sender).getName() + ":" + kitname)) {
						applyKit(((Player)sender),((Player)sender).getName() + ":" + kitname, sender, silent, concat);
					//Check if the kitname is a Global kit by removing any ":" and checking the kitList, if it exists, anyone with .use permissions can use it.
					} else if (kitList.containsKey(kitname.replace(":", ""))) {
						applyKit(((Player)sender), kitname, sender, silent, concat);
					}
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
		//Added check here for player logging in previously. If we do not, some code will break the first login kit code with NPEs.
		if(target.hasPlayedBefore()) {
			//Remove all potion effects from the player.
			Collection<PotionEffect> effects = target.getActivePotionEffects();
			for (PotionEffect effect : effects) {
				target.removePotionEffect(effect.getType());
			}
			//Force player to update inventory. Without this sometimes kits will apply but appear invisible.
			target.updateInventory();
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
	private Boolean global = false;
	//Public properties to access the arguments.
	public String Arg1() { return arg1; }
	public String Arg2() { return arg2; }
	public Boolean Concat() { return concat; }
	public Boolean Silent() { return silent; }
	public Boolean Global() { return global; }
	
	public Args(String[] args){
		if(args.length > 0){
			for(String entry : args){
				//If the entry is a flag, determine which it is and set the Boolean values.
				if(isFlag(entry)) {
					if(entry.contentEquals("-c") || entry.contentEquals("-concat")) { concat = true; }
					else if (entry.contentEquals("-s") || entry.contentEquals("-silent")) { silent = true; }
					else if (entry.contentEquals("-g") || entry.contentEquals("-global")) { global = true; }
					//If the entry is NOT a flag, and arg1 is empty, set it. Repeat for arg2.
				} else if (arg1.isEmpty()) { arg1 = entry; }
				else if (arg2.isEmpty()) { arg2 = entry; }
			}
		}
	}
	
	private boolean isFlag(String arg){
		List<String> flags = Arrays.asList("-c", "-concat", "-s", "-silent", "-g", "-global");
		//Compare each valid flag against the provided string.
		for(String entry : flags){
			if(arg.equalsIgnoreCase(entry)){ return true; }
		}
		return false;
	}
}