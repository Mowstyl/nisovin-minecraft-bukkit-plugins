package com.nisovin.magicspells.shop;

import java.io.File;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.command.ScrollSpell;

public class MagicSpellsShop extends JavaPlugin implements Listener {

	private boolean requireKnownSpell;
	private boolean requireTeachPerm;
	
	private String firstLine;
	private String strAlreadyKnown;
	private String strCantAfford;
	private String strPurchased;
	
	private String firstLineScroll;
	private String scrollSpellName;
	private int scrollItemType;
	private String strCantAffordScroll;
	private String strPurchasedScroll;
	private String strScrollFail;
	
	private CurrencyHandler currency;
	
	@Override
	public void onEnable() {
		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}
		
		Configuration config = getConfig();
		requireKnownSpell = config.getBoolean("require-known-spell", true);
		requireTeachPerm = config.getBoolean("require-teach-perm", true);
		
		firstLine = config.getString("first-line", "[SPELL SHOP]");
		strAlreadyKnown = config.getString("str-already-known", "You already know that spell.");
		strCantAfford = config.getString("str-cant-afford", "You cannot afford that spell.");
		strPurchased = config.getString("str-purchased", "You have purchased the %s spell.");
		
		firstLineScroll = config.getString("first-line-scroll", "[SCROLL SHOP]");
		scrollSpellName = config.getString("scroll-spell-name", "scroll");
		scrollItemType = config.getInt("scroll-item-type", Material.PAPER.getId());
		strCantAffordScroll = config.getString("str-cant-afford-scroll", "You cannot afford that scroll.");
		strPurchasedScroll = config.getString("str-purchased-scroll", "You have purchased a scroll for the %s spell.");
		strScrollFail = config.getString("str-scroll-fail", "You cannot purchase a scroll at this time.");
		
		currency = new CurrencyHandler(config);
		
		// register events
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		
		// check for right-click on sign
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}		
		Block block = event.getClickedBlock();
		if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
			return;
		}
		
		// get shop sign
		Sign sign = (Sign)block.getState();
		String[] lines = sign.getLines();		
		if (lines[0].equals(firstLine)) {
			processSpellShopSign(event.getPlayer(), lines);
		} else if (lines[0].equals(firstLineScroll)) {
			processScrollShopSign(event.getPlayer(), lines);
		}
		
	}
	
	private void processSpellShopSign(Player player, String[] lines) {
		// get spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) {
			return;
		}
		
		// check if already known
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (spellbook.hasSpell(spell)) {
			MagicSpells.sendMessage(player, strAlreadyKnown, "%s", spellName);
			return;
		}
		
		// get cost
		Cost cost = getCost(lines[2]);
		
		// check for currency
		if (!currency.has(player, cost.amount, cost.currency)) {
			MagicSpells.sendMessage(player, strCantAfford, "%s", spellName, "%c", cost+"");
			return;
		}
		
		// attempt to teach
		boolean taught = MagicSpells.teachSpell(player, spellName);
		if (!taught) {
			return;
		}
		
		// remove currency
		currency.remove(player, cost.amount, cost.currency);
		
		// success!
		MagicSpells.sendMessage(player, strPurchased, "%s", spellName, "%c", cost+"");
	}
	
	private void processScrollShopSign(Player player, String[] lines) {
		// get spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) {
			return;
		}
		
		// get uses
		if (!lines[2].matches("^[0-9]+( .+)?$")) {
			return;
		}
		int uses = Integer.parseInt(lines[2].split(" ")[0]);
		
		// get cost
		Cost cost = getCost(lines[3]);
		
		// check if can afford
		if (!currency.has(player, cost.amount, cost.currency)) {
			MagicSpells.sendMessage(player, strCantAffordScroll, "%s", spellName, "%c", cost+"", "%u", uses+"");
			return;
		}
		
		// create scroll
		ScrollSpell scrollSpell = (ScrollSpell)MagicSpells.getSpellByInternalName(scrollSpellName);
		short scrollId = scrollSpell.createScroll(spell, uses);
		if (scrollId == 0) {
			MagicSpells.sendMessage(player, strScrollFail);
			return;
		}
		ItemStack scroll = new ItemStack(scrollItemType, 1, scrollId);
		
		// remove currency
		currency.remove(player, cost.amount, cost.currency);
		
		// give to player
		int slot = player.getInventory().firstEmpty();
		if (player.getItemInHand() == null) {
			player.setItemInHand(scroll);
		} else if (slot >= 0) {
			player.getInventory().setItem(slot, scroll);
		} else {
			player.getWorld().dropItem(player.getLocation(), scroll);
		}
		
		// done!
		MagicSpells.sendMessage(player, strPurchasedScroll, "%s", spellName, "%c", cost+"", "%u", uses+"");
	}
	
	private Cost getCost(String line) {
		Cost cost = new Cost();
		if (!line.isEmpty()) {
			if (!line.contains(" ") && line.matches("^[0-9]+(\\.[0-9]+)?$")) {
				cost.amount = Double.parseDouble(line);
			} else if (line.contains(" ")) {
				String[] s = line.split(" ");
				if (s[0].matches("^[0-9]+(\\.[0-9]+)?$")) {
					cost.amount = Double.parseDouble(s[0]);
					cost.currency = s[1];
				}
			}
		}
		return cost;
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onSignCreate(SignChangeEvent event) {
		if (event.isCancelled()) return;
		
		boolean isSpellShop;
		
		String lines[] = event.getLines();
		if (lines[0].equals(firstLine)) {
			isSpellShop = true;
		} else if (lines[0].equals(firstLineScroll)) {
			isSpellShop = false;
		} else {
			return;
		}
		
		// check permission
		if (!event.getPlayer().hasPermission("magicspells.createsignshop")) {
			event.setCancelled(true);
			return;
		}
		
		// check for valid spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) {
			event.getPlayer().sendMessage("A spell by that name does not exist.");
		}
		
		// check permissions
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (requireKnownSpell && !spellbook.hasSpell(spell)) {
			event.setCancelled(true);
			return;
		}
		if (requireTeachPerm && !spellbook.canTeach(spell)) {
			event.setCancelled(true);
			return;
		}
		
		// get cost
		Cost cost = getCost(lines[isSpellShop?2:3]);
		
		event.getPlayer().sendMessage((isSpellShop?"Spell":"Scroll") + " shop created: " + 
				spellName + (isSpellShop?"":"(" + lines[2] + ")") + 
				" for " + cost.amount + " " + (currency.isValidCurrency(cost.currency) ? cost.currency : "currency") + ".");
		
	}

	@Override
	public void onDisable() {
		
	}
	
	private class Cost {
		double amount = 0;
		String currency = null;
	}

}