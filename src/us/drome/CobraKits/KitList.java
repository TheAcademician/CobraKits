package us.drome.CobraKits;

import java.util.ArrayList;

class KitList extends ArrayList<Kit> {
	private static final long serialVersionUID = 7480424712605617303L;

	//Custom contains, only requires a kit name to search.
	boolean contains(String Name) {
		//Iterate through all kits in this list.
		for(Kit entry: this) {
			//If owner.kitname matches, return true;
			if((entry.Owner() + "." + entry.Name()).equalsIgnoreCase(Name)) {
				return true;
			//If the owner of this kit is Global, check to see if the name matches the query.
			} else if(entry.Owner().equals("Global") && entry.Name().equalsIgnoreCase(Name)) {
				return true;
			//If the owner of this kit is Server, check to see if the name matches the query.
			} else if(entry.Owner().equals("Server") && entry.Name().equalsIgnoreCase(Name)) {
				return true;
			}
		}
		return false;
	}
	
	//Custom indexOf, only requires a kit name to search.
	int indexOf(String Name) {
		//Iterate through all kits in this list.
		for(Kit entry: this) {
			//If owner.kitname matches, return true;
			if((entry.Owner() + "." + entry.Name()).equalsIgnoreCase(Name)) {
				return this.indexOf(entry);
				//If the owner of this kit is Global, check to see if the name matches the query.
			} else if(entry.Owner().equals("Global") && entry.Name().equalsIgnoreCase(Name)) {
				return this.indexOf(entry);
				//If the owner of this kit is Server, check to see if the name matches the query.
			} else if(entry.Owner().equals("Server") && entry.Name().equalsIgnoreCase(Name)) {
				return this.indexOf(entry);
			}
		}
		return -1; //Always check .contains before running .indexOf or it will return -1 and cause a NullPointerException on any KitList.get() run if it doesn't exist.
	}
}