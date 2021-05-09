package me.DMan16.AxRP;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.Aldreda.AxUtils.AxUtils;
import me.Aldreda.AxUtils.Utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class AxRP extends JavaPlugin implements Listener {
	private MySQL SQL;
	private String link = null;
	private String hash = null;
	private List<Player> waiting = new ArrayList<Player>();
	
	public void onEnable() {
		try {
			SQL = new MySQL();
		} catch (SQLException e) {
			Utils.chatColorsLogPlugin("&fAxRP &bMySQL connection: &cFAILURE!!!");
			this.getLogger().severe("MySQL error: ");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		getServer().getPluginManager().registerEvents(this,this);
		setLinkHash();
		new BukkitRunnable() {
			public void run() {
				if (!setLinkHash()) return;
				for (Player player : Bukkit.getOnlinePlayers()) setRP(player);
			}
		}.runTaskTimer(this,1 * 60 * 20,5 * 60 * 20);
		Utils.chatColorsLogPlugin("&fAxRP &aloaded!");
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		new BukkitRunnable() {
			public void run() {
				setRP(event.getPlayer());
			}
		}.runTask(this);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onResourcePack(PlayerResourcePackStatusEvent event) {
		Player player = event.getPlayer();
		if (event.getStatus() == Status.DECLINED) player.kick(Component.text("Resource pack apply denied!").color(NamedTextColor.RED).decoration(
				TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,false).append(Component.newline()).append(Component.text(
						"Our server uses a mandatory resource pack - please enable it!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false)));
		else if (event.getStatus() == Status.FAILED_DOWNLOAD) {
			if (!waiting.contains(player)) {
				setRP(player);
				waiting.add(player);
			} else player.kick(Component.text("Resource pack download failed!").color(NamedTextColor.GOLD).decoration(
					TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,false).append(Component.newline()).append(Component.text(
							"Please try to relog").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false)));
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		waiting.remove(event.getPlayer());
	}
	
	private void setRP(Player player) {
		if (link != null && hash != null) player.setResourcePack(link,hash);
	}
	
	private boolean setLinkHash() {
		try {
			String newLink = SQL.getLink();
			String newHash = SQL.getHash();
			if (((link == null && newLink != null) || !link.equals(newLink)) || ((hash == null && newHash != null) || !hash.equals(newHash))) {
				if (newLink == null) {
					link = null;
					hash = null;
				} else {
					link = newLink;
					hash = newHash;
				}
				return true;
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return false;
	}

	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		Utils.chatColorsLogPlugin("&fAxRP &adisabed");
	}
	
	private class MySQL {
		
		private MySQL() throws SQLException {
			Statement statement = AxUtils.getMySQL().getConnection().createStatement();
			DatabaseMetaData data = AxUtils.getMySQL().getConnection().getMetaData();
			statement.execute("CREATE TABLE IF NOT EXISTS RP (ID VARCHAR(2) NOT NULL UNIQUE);");
			if (!data.getColumns(null,null,"RP","ID").next())
				statement.execute("ALTER TABLE RP ADD ID VARCHAR(2) NOT NULL UNIQUE;");
			if (!data.getColumns(null,null,"RP","URL").next())
				statement.execute("ALTER TABLE RP ADD URL TEXT;");
			if (!data.getColumns(null,null,"RP","Hash").next())
				statement.execute("ALTER TABLE RP ADD Hash VARCHAR(40);");
			if (!statement.executeQuery("SELECT * FROM RP WHERE ID=\"RP\";").next()) statement.execute("INSERT INTO RP (ID) VALUES (\"RP\");");
			statement.close();
		}
		
		public String getLink() throws SQLException {
			String link = null;
			Statement statement = AxUtils.getMySQL().getConnection().createStatement();
			ResultSet result = statement.executeQuery("SELECT * FROM RP WHERE ID=\"RP\";");
			result.next();
			link = result.getString("URL");
			statement.close();
			return link;
		}
		
		public String getHash() throws SQLException {
			String link = null;
			Statement statement = AxUtils.getMySQL().getConnection().createStatement();
			ResultSet result = statement.executeQuery("SELECT * FROM RP WHERE ID=\"RP\";");
			result.next();
			link = result.getString("Hash");
			statement.close();
			return link;
		}
	}
}