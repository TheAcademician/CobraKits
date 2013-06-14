package us.drome.CobraKits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

class Kit {
	private String owner = "";
	private String name = "";
	private ItemStack[] inventory;
	private ItemStack[] armor;
	private List<PotionEffect> potions;
	private int cooldown;
	private ItemStack cost;
	public String Owner() { return owner; }
	public String Name() { return name; }
	public ItemStack[] Inventory() { return inventory; }
	public ItemStack[] Armor() { return armor; }
	public List<PotionEffect> Potions() { return potions; }
	public int Cooldown() { return cooldown; }
	public ItemStack Cost() { return cost; }
	
	public Kit(String Owner, String Name, ItemStack[] Inventory, ItemStack[] Armor, List<PotionEffect> Potions, int Cooldown, ItemStack Cost) {
		owner = Owner;
		name = Name;
		inventory = Inventory;
		armor = Armor;
		potions = Potions;
		cooldown = Cooldown;
		cost = Cost;
	}
	
	//Other constructor that can take a generic List as input. This is needed for LoadKits() as the kits.yml file can only store Lists and not ItemStack[] directly.
	@SuppressWarnings("unchecked")
	public Kit(String Owner, String Name, List<?> Inventory, List<?> Armor, List<?> Potions, int Cooldown, ItemStack Cost) {
		owner = Owner;
		name = Name;
		inventory = Inventory.toArray(new ItemStack[Inventory.size()]);
		armor = Armor.toArray(new ItemStack[Armor.size()]);
		potions = (List<PotionEffect>)Potions;
		cooldown = Cooldown;
		cost = Cost;
	}
	
	//Converts the inventory ItemStack[] to a list for the SaveKits() method, as you can only store Lists in configuration files.
	ArrayList<ItemStack> InventoryAsList() {
		return new ArrayList<ItemStack>(Arrays.asList(inventory));
	}
	
	//Converts the armor ItemStack[] to a list for the SaveKits() method, as you can only store Lists in configuration files.	
	ArrayList<ItemStack> ArmorAsList() {
		return new ArrayList<ItemStack>(Arrays.asList(armor));
	}
}