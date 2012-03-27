package com.nisovin.yapp;

import java.util.Set;

public class VaultService extends net.milkbowl.vault.permission.Permission {
	
	@Override
	public String getName() {
		return "YAPP";
	}

	@Override
	public boolean isEnabled() {
		return plugin.isEnabled() && MainPlugin.yapp != null;
	}

	@Override
	public boolean hasSuperPermsCompat() {
		return true;
	}

	@Override
	public boolean playerHas(String world, String player, String permission) {
		return MainPlugin.getPlayerUser(player).has(world, permission);
	}

	@Override
	public boolean playerAdd(String world, String player, String permission) {
		return MainPlugin.getPlayerUser(player).removePermission(world, permission);
	}

	@Override
	public boolean playerRemove(String world, String player, String permission) {
		return MainPlugin.getPlayerUser(player).removePermission(world, permission);
	}

	@Override
	public boolean groupHas(String world, String group, String permission) {
		Group g = MainPlugin.getGroup(group);
		if (g != null) {
			return g.has(world, permission);
		} else {
			return false;
		}
	}

	@Override
	public boolean groupAdd(String world, String group, String permission) {
		Group g = MainPlugin.getGroup(group);
		if (g != null) {
			return g.addPermission(world, permission);
		} else {
			return false;
		}
	}

	@Override
	public boolean groupRemove(String world, String group, String permission) {
		Group g = MainPlugin.getGroup(group);
		if (g != null) {
			return g.removePermission(world, permission);
		} else {
			return false;
		}
	}

	@Override
	public boolean playerInGroup(String world, String player, String group) {
		Group g = MainPlugin.getGroup(group);
		if (g == null) {
			return false;
		} else {
			return MainPlugin.getPlayerUser(player).inGroup(world, g, true);
		}
	}

	@Override
	public boolean playerAddGroup(String world, String player, String group) {
		Group g = MainPlugin.getGroup(group);
		if (g == null) {
			g = MainPlugin.newGroup(group);
		}
		return MainPlugin.getPlayerUser(player).addGroup(world, g);
	}

	@Override
	public boolean playerRemoveGroup(String world, String player, String group) {
		Group g = MainPlugin.getGroup(group);
		if (g == null) {
			return false;
		} else {
			return MainPlugin.getPlayerUser(player).removeGroup(world, g);
		}
	}

	@Override
	public String[] getPlayerGroups(String world, String player) {
		Set<Group> groups = MainPlugin.getPlayerUser(player).getGroups(world);
		Group[] groupsArray = groups.toArray(new Group[groups.size()]);
		String[] groupNames = new String[groups.size()];
		for (int i = 0; i < groupsArray.length; i++) {
			groupNames[i] = groupsArray[i].getName();
		}
		return groupNames;
	}

	@Override
	public String getPrimaryGroup(String world, String player) {
		Group group = MainPlugin.getPlayerUser(player).getPrimaryGroup(world);
		if (group == null) {
			return null;
		} else {
			return group.getName();
		}
	}	
	
	@Override
	public String[] getGroups() {
		return MainPlugin.getGroupNames().toArray(new String[]{});
	}

}