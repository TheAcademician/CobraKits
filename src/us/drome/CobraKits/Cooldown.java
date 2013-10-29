package us.drome.CobraKits;

import java.util.ArrayList;

class Cooldown {
	public String player;
	public String kit;
	public long endTime;
	
	public Cooldown(String player, String kit, long endTime) {
		this.player = player;
		this.kit = kit;
		this.endTime = endTime;
	}
	
	//Method to calculate the remaining cooldown time and return it in a formatted string.
	public String timeRemaining(long currentTime) {
		long timeLeft = (this.endTime - currentTime) / 1000; //Convert remaining time into seconds.
		int days = (int) (timeLeft / (24 * 60 * 60)); //Divide by total seconds in a day to reach days left.
		timeLeft = timeLeft % (24 * 60 * 60); //Reduce timeLeft by Mod dividing by total seconds in a day.
		int hours = (int) (timeLeft / (60 * 60)); //Divide by total seconds in an hour to reach hours left.
		timeLeft = timeLeft % (60 * 60); //Reduce timeLeft by Mod dividing by total seconds in an hour.
		int minutes = (int) timeLeft / 60; 
		int seconds = (int) timeLeft % 60;
		
		String timeLeftString = (days > 0 ? days + "D" : "") + (days > 0 && hours > 0 ? " " : "") +
				(hours > 0 ? hours + "H" : "") + (hours > 0 && minutes > 0 ? " " : "") +
				(minutes > 0 ? minutes + "m" : "") + (minutes > 0 && seconds > 0 ? " " : "") +
				(seconds > 0 ? seconds + "s" : "");
		
		return timeLeftString;
	}
}

class CooldownList extends ArrayList<Cooldown> {
	private static final long serialVersionUID = -2835374625134106220L;
	
	//Method for determining if a provided player and kit name are in the list and returning that entry.
	Cooldown isOnCooldown(String player, String kit) {
		for(Cooldown entry : this) {
			if(entry.player.equals(player) && entry.kit.equalsIgnoreCase(kit)) {
				return entry;
			}
		}
		return null;
	}
}
