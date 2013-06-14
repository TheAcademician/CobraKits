package us.drome.CobraKits;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class Args{
	//Private non-changable variables to hold the parsed values.
	private String arg1 = "";
	private String arg2 = "";
	private Boolean concat = false;
	private Boolean silent = false;
	private Boolean global = false;
	private Boolean server = false;
	private Boolean potions = false;
	private int cooldown = -1;
	private ItemStack cost = null;
	//Public properties to access the arguments.
	public String Arg1() { return arg1; }
	public String Arg2() { return arg2; }
	public Boolean Concat() { return concat; }
	public Boolean Silent() { return silent; }
	public Boolean Global() { return global; }
	public Boolean Server() { return server; }
	public Boolean Potions() { return potions; }
	public int Cooldown() { return cooldown; }
	public ItemStack Cost() { return cost; }
	
	public Args(String[] args){
		List<String> arguments = new LinkedList<String>(Arrays.asList(args));
		while (!arguments.isEmpty()) {
			String entry = arguments.get(0);
			if(isFlag(entry)) {
				if(entry.contentEquals("-c") || entry.contentEquals("-concat")) { concat = true; arguments.remove(0); }
				else if (entry.contentEquals("-s") || entry.contentEquals("-silent")) { silent = true; arguments.remove(0); }
				else if (entry.contentEquals("-g") || entry.contentEquals("-global")) { global = true; arguments.remove(0); }
				else if (entry.contentEquals("-sk") || entry.contentEquals("-server")) { server = true; arguments.remove(0); }
				else if (entry.contentEquals("-p") || entry.contentEquals("-potions")) { potions = true; arguments.remove(0); }
				else if (entry.contentEquals("-cd") || entry.contentEquals("-cooldown")) {
					if(arguments.size() > 1 && !isFlag(arguments.get(1))) {
						try {
							cooldown = Integer.parseInt(arguments.get(1)) * 20;
						} catch (NumberFormatException e) {
							emptyArgs();
						}
						arguments.remove(0);
						arguments.remove(0);
					} else { emptyArgs(); }
				} else if (entry.contentEquals("-kc") || entry.contentEquals("-cost")) {
					if(arguments.size() > 1 && !isFlag(arguments.get(1))) {
						if(arguments.get(1).contains(":")) {
							if(arguments.get(1).contentEquals("clear")) {
								cost = new ItemStack(Material.AIR);
							} else {
								int costAmount = 0;
								int costBlock = 0;
								try {
									costBlock = Integer.parseInt(arguments.get(1).split(":")[0]);
									costAmount = Integer.parseInt(arguments.get(1).split(":")[1]);
								} catch (NumberFormatException e) {
									emptyArgs();
									continue;
								}
								cost = new ItemStack(Material.getMaterial(costBlock), costAmount, Material.getMaterial(costBlock).getMaxDurability());
							}
						} else { emptyArgs(); continue; }
						
						arguments.remove(0);
						arguments.remove(0);
					} else { emptyArgs(); continue; }
				}
			} else if (arg1.isEmpty()) { arg1 = entry; arguments.remove(0); }
			else if (arg2.isEmpty()) { arg2 = entry; arguments.remove(0); }
		}
	}
	
	private void emptyArgs() {
		//Method to blank this Args object, indicating user input is incorrect. Kind of a kludgey way to do it.
		arg1 = "";
		arg2 = "";
		concat = false;
		silent = false;
		global = false;
		server = false;
		potions = false;
		cooldown = -1;
		cost = null;
	}
	
	private boolean isFlag(String arg){
		List<String> flags = Arrays.asList("-c", "-concat", "-s", "-silent", "-g", "-global", "-sk", "-server", "-p", "-potions", "-cd", "-cooldown", "-kc", "-cost");
		//Compare each valid flag against the provided string.
		for(String entry : flags){
			if(arg.equalsIgnoreCase(entry)){ return true; }
		}
		return false;
	}
}