package us.drome.CobraKits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;

import net.minecraft.server.v1_5_R3.NBTBase;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.NBTTagList;

import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
/*
* Modified form of ItemSerialization by Comphenix - https://gist.github.com/4102407
*/
public class Serialize {
    public static String toBase64(ItemStack[] inventory) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        NBTTagList itemList = new NBTTagList();
        
        // Save every element in the list
        for (int i = 0; i < inventory.length; i++) {
            NBTTagCompound outputObject = new NBTTagCompound();
            CraftItemStack craft = getCraftVersion(inventory[i]);
            
            // Convert the item stack to a NBT compound
            if (craft != null && craft.getAmount() != 0)
                CraftItemStack.asNMSCopy(craft).save(outputObject);
            itemList.add(outputObject);
        }

        // Now save the list
        NBTBase.a(itemList, dataOutput);

        // Serialize that array
        return new BigInteger(1, outputStream.toByteArray()).toString(32);
    }
    
    public static ItemStack[] fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(data, 32).toByteArray());
        NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
        ItemStack[] inventory = new ItemStack[itemList.size()];

        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
            
            // IsEmpty
            if (!inputObject.isEmpty()) {
                inventory[i] = CraftItemStack.asCraftMirror(net.minecraft.server.v1_5_R3.ItemStack.createStack(inputObject));
                 //CraftItemStack.asCraftMirror(ItemStack.a(inputObject));
                 //new CraftItemStack(net.minecraft.server.v1_4_6.ItemStack.a(inputObject));
            }
        }
        
        // Serialize that array
        return inventory;
    }
    
    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack)
            return (CraftItemStack) stack;
        else if (stack != null)
            return CraftItemStack.asCraftCopy(stack);
        else
            return null;
    }
}