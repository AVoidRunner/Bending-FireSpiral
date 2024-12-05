package me.Mastervrunner.FireSpin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import me.Mastervrunner.FireSpin.FireSpin;

public class FireSpinListener implements Listener{

	@EventHandler
	public void onClick(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (event.isCancelled() || bPlayer == null) {
			return;
		}

		if (bPlayer.getBoundAbilityName().equalsIgnoreCase(null)) {
			return;
		}

		if (bPlayer.getBoundAbilityName().equalsIgnoreCase("FireSpiral")) {
			new FireSpin(player);
		}
	}
	
	
}
