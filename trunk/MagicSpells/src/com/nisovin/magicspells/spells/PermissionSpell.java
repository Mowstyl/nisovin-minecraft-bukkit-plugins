package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;

public class PermissionSpell extends InstantSpell {

	private int duration;
	private List<String> permissionNodes;
	
	public PermissionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		duration = getConfigInt("duration", 0);		
		permissionNodes = getConfigStringList("permission-nodes", null);
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && duration > 0 && permissionNodes != null) {
			for (String node : permissionNodes) {
				player.addAttachment(MagicSpells.plugin, node, true, duration);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
