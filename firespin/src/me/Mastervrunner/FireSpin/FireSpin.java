package me.Mastervrunner.FireSpin;

import java.util.HashMap;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

//import me.mastervrunner.multishot.MultiShot;
//import me.mastervrunner.multishot.MultiShotListener;
import me.Mastervrunner.FireSpin.*;

public class FireSpin extends FireAbility implements AddonAbility{

	//Listener of your ability. Listener doesn't have anything special for multi-shot ability.
			private Listener MSL;
			
			//Cooldown of your ability.
			private long cooldown;
			//Shot number of your ability.
			private int charge;
			//Range of your projectiles.
			private int range;
			//Damage of your projectiles.
			private double damage;
			//Speed of your projectiles.
			private double speed = 2;
			//Maximum waiting time for players to shoot all of the charges.
			private long duration;
			//Delay between shots to prevent spamming.
			private long timeBetweenShots;
			//A temporary variable for keeping track of last shot time. It is required to put delay between shots.
			private long lastShotTime;
			//Holding last projectile's unique id. Later, we will use this in hasmap as a key.
			private int lastProjectileId;
			
			//Each shot has different location so we use a hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> locations;
			//Each shot has different directions so we use a hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Vector> directions;
			//Each shot has different starting location so we hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> startLocations;
			//This hashmap is required for removing shots when they touch an entity, a block or when they are out of range. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> deadProjectiles;
			
			public FireSpin(Player player) {
				super(player);
				
				//Don't continue if your ability is on cooldown.
				if (bPlayer.isOnCooldown(this)) {
					return;
				}
				
				//Don't continue if player can't use this ability.
				if (!bPlayer.canBend(this)) {
					return;
				}
				
				//If player already has your ability active, create a new shot.
				//Otherwise, set the initial variables and start your ability.
				if (hasAbility(player, FireSpin.class) && 1 == 2) {
					//We are in this if block. So, your multi-shot ability was active for the player and he/she clicked it again to shoot another projectile.
					//Code below gets the active instance of your ability for the player.
					//From now on, we can use ms variable to get and set variables of that already active instance of your move.
					FireSpin ms = getAbility(player, FireSpin.class);
					
					//If block below checks if ms has any charges left and is it ready to shoot another projectile.
					if (ms.getCharge() == 0 || System.currentTimeMillis() < ms.getLastShotTime() + ms.getTimeBetweenShots()) {
						return;
					}
					
					//If block below checks if we are shooting the last projectile or not. (Charge is the number of projectiles you can shoot.)
					//If we are shooting the last projectile, it adds cooldown to the player.
					if (ms.getCharge() == 1) {
						bPlayer.addCooldown(ms);
					}
					
					//Determine the id of your new projectile. (It is the last id + 1)
					int projectileId = ms.getLastProjectileId() + 1;
					//Create the starting location of your projectile.
					Location loc = player.getLocation().add(0, 1, 0);
					//Add that location to ms's hashmap of locations.
					//Adding new location to ms means you are adding new projectile to your move.
					ms.getParticleLocations().put(projectileId, loc);
					//Set direction of this new projectile and add it to ms's directions hashmap.
					ms.getDirections().put(projectileId, player.getLocation().getDirection());
					//Set starting location of this new projectile and add it to ms's direction hashmap.
					ms.getStartLocations().put(projectileId, loc.clone());
					//Set last shot time of ms to current time because we just shot a new projectile.
					ms.setLastShotTime(System.currentTimeMillis());
					//Decrease the number of projectiles you can shoot by 1.
					ms.setCharge(ms.getCharge() - 1);
					//Update last projectile id to this projectile's id.
					ms.setLastProjectileId(projectileId);
					
					//Notice that even tho your ability is triggered by clicking, we are not starting a new move.
					//Instead, we are using the ability that is already active and update it's variables to make it
					//manage all the projectiles.
				} else {
					//We are here if the abilty is clicked for the first time.
					//So we will shoot the first projectile.
					//We initialize our variables.
					//Block underPlayerBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
					Material underPlayerBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
					
					if(underPlayerBlock != Material.AIR) {
						setField();
						//We start the ability.
						start();
					}
					
				}
				
			}
			
			public void setField() {
				//Cooldown of your ability.
				cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Cooldown");

				//Maximum projectile number of your ability.
				charge = 1;
				
				//Range of every projectile.
				range = ConfigManager.getConfig().getInt("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Range");

				//Damage of every projectile.
				damage = ConfigManager.getConfig().getInt("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Damage");
				
				//If you want to make the range and damage different for each shot,
				//You need to keep track of range and damage using a hashmap similar to what we
				//did for different directions and starting locations.
				
				//Time limit to shoot all of the projectiles.
				duration = 1000;
				//Delay between shooting projectiles.
				timeBetweenShots = 1;
				//Shooting time of last projectile is startTime because we just shot our first projectile.
				lastShotTime = getStartTime();
				
				//Initialize the hashmaps.
				deadProjectiles = new HashMap<Integer, Location>();
				locations = new HashMap<Integer, Location>();
				directions = new HashMap<Integer, Vector>();
				startLocations = new HashMap<Integer, Location>();
				
				//Creating an id for our first projectile.
				lastProjectileId = 1;
				//Create the starting location of first projectile.
				Location loc = player.getLocation().add(0, 1, 0);
				//Add it to hashmap of locations. This hashmap holds each projectile's current location.
				locations.put(lastProjectileId, loc);
				//Add first projectile's direction to hashmap of directions.
				//This hashmap holds each projectile's direction.
				directions.put(lastProjectileId, player.getLocation().getDirection());
				//Add first projectile's starting location to hashmap of starting locations.
				//This hashmap holds each projectile's starting location.
				//We are using starting locations when we check for the range.
				startLocations.put(lastProjectileId, loc.clone());
				
				//Decrease the charge by 1 because we just shot our first projectile.
				charge--;
				
				int points = 36;
				double radius2 = 3.0d;
				Location origin = player.getLocation();
				double moveval = 1.1;

				for (int i = 0; i < points; i++) {
				    double angle = 2 * Math.PI * i / points;

				    Location point = origin.clone().add(0.0d, radius * Math.sin(angle), 0.0d);
				    point.add(player.getLocation().getDirection().multiply(Math.cos(angle)));

				    player.getWorld().spawnParticle(Particle.REDSTONE, point.getX(), point.getY(), point.getZ(), 1, 0.001, 1, 0, 1, new Particle.DustOptions(Color.BLUE, 1));
				    
				}
				
				double speedyness = 2;
				double speedyConfig = ConfigManager.getConfig().getDouble("ExtraAbilities.Mastervrunner.Fire.FireSpiral.DashSpeed");

				
				Vector setVel = player.getVelocity();
				setVel.add(player.getLocation().getDirection().multiply(speedyConfig));

				player.setVelocity(setVel);
				
			}
			
			boolean goUpBack = true;
			
			int points = 10;
			double radius = 3.0d;
			Location origin = player.getLocation();
			
			int iterations = 0;
			

			@Override
			public void progress() {
				
				//If no charge left (So you cannot shoot more projectiles,
				//and location hashmap is empty (So all of the projectiles are dead)
				//we can remove the move. (We won't add cooldown because we added it
				//when we shot the last projectile.)
				
				//If duration is over, add cooldown and remove the ability.
				if (System.currentTimeMillis() > getStartTime() + duration) {
					bPlayer.addCooldown(this);
					remove();
				}
				
				//This for loop below is the thing that progress every projectile.
				//i is the projectile's id.
				
				//What this for does is, for each projectile:
				//spawn the particle,
				//check for living entities around to damage one of them.
				//move the projectile to it's next position.
				//check for the range,
				//check if the projectile hits a block.
				for (Integer i : locations.keySet()) {
					//Spawn your i'th projectile's particle at it's location
					
					player.getWorld().spawnParticle(Particle.REDSTONE, locations.get(i).getX(), locations.get(i).getY(), locations.get(i).getZ(), 1, 0.001, 1, 0, 1, new Particle.DustOptions(Color.AQUA, 1));
					
					//Check living entities near i'th projectile to damage one of them.
					for (Entity e : GeneralMethods.getEntitiesAroundPoint(locations.get(i), 1.5)) {
						if (e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId())) {
							DamageHandler.damageEntity(e, damage, this);
							//After you damage an entity, you need to remove that projectile unless
							//you want it to go through entities.
							//If we remove it right here, that will break our for loop because we are using
							//locations.size() and removing an element will change it's size.
							//So we use a temporary hashmap to keep track of projectile we want to remove 
							//after for loop is over.
							deadProjectiles.put(i, locations.get(i));
						}
					}
					bPlayer.addCooldown(this);
					
				////	Methods
					//Move i'th projectile to it's next position.
					
					speed = ConfigManager.getConfig().getInt("ExtraAbilities.Mastervrunner.Fire.FireSpiral.AttackSpeed");
					locations.get(i).add(directions.get(i).clone().multiply(speed));
					
					//If it is out of range or it hit a block, add it to the temporary hashmap to remove later.
					if (locations.get(i).distance(startLocations.get(i)) > range
							|| GeneralMethods.isSolid(locations.get(i).getBlock())) {
						deadProjectiles.put(i, locations.get(i));
					}
				}
				
				//Our loop that progress all of the projectiles is over.
				//Now we can safely remove the dead projectiles from every
				//hashmap we used that projectile in.
				for(Integer i : deadProjectiles.keySet()) {
					locations.remove(i);
					directions.remove(i);
					startLocations.remove(i);
				}
				//Finally, we clear our temporary hashmap.
				deadProjectiles.clear();
				
			}
			
			//We use this method to get this instance of your ability's charge value.
			public int getCharge() {
				return this.charge;
			}
			
			//We use this method to update this instance of your ability's charge value.
			public void setCharge(int charge) {
				this.charge = charge;
			}
			
			public int getLastProjectileId() {
				return this.lastProjectileId;
			}
			
			public void setLastProjectileId(int id) {
				this.lastProjectileId = id;
			}
			
			//We use this method to get this instance of your ability's lastShotTime value.
			public long getLastShotTime() {
				return this.lastShotTime;
			}
			
			//We use this method to update this instance of your ability's lastShotTime value.
			public void setLastShotTime(long time) {
				this.lastShotTime = time;
			}
			
			//We use this method to get this instance of your ability's timeBetweenShots value.
			public long getTimeBetweenShots() {
				return this.timeBetweenShots;
			}
			
			//We use this method to get this instance of your ability's locations hashmap.
			public HashMap<Integer, Location> getParticleLocations() {
				return this.locations;
			}
			
			//We use this method to get this instance of your ability's directions hashmap.
			public HashMap<Integer, Vector> getDirections() {
				return this.directions;
			}
			
			//We use this method to get this instance of your ability's startLocations hashmap.
			public HashMap<Integer, Location> getStartLocations() {
				return this.startLocations;
			}
			
			@Override
			public long getCooldown() {
				return this.cooldown;
			}

			@Override
			public Location getLocation() {
				return null;
			}

			@Override
			public String getName() {
				return "FireSpiral";
			}

			@Override
			public boolean isHarmlessAbility() {
				return false;
			}

			@Override
			public boolean isSneakAbility() {
				return false;
			}

			@Override
			public String getAuthor() {
				return "Mastervrunner";
			}

			@Override
			public String getVersion() {
				return "1.48";
			}
			
			@Override
			public String getDescription() {
				return "<LEFT CLICK>: Spiral, dash, and attack... With fire. Can only work when you are on the ground";
			}
			

			@Override
			public void load() {
				//We are registering our listener.
				MSL = new FireSpinListener();
				ProjectKorra.plugin.getServer().getPluginManager().registerEvents(MSL, ProjectKorra.plugin);
				ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
				
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireSpiral.DashSpeed", 2);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Cooldown", 2000);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Range", 20);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireSpiral.Damage", 1);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireSpiral.AttackSpeed", 2);
				
				ConfigManager.defaultConfig.save();
				
			}

			@Override
			public void stop() {
				ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
				//We are unregistering our listener.
				HandlerList.unregisterAll(MSL);
				super.remove();
			}
	
	
}
