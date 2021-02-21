package Minigames;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import Minigames.Games.HideAndSeek.HideAndSeekLobby;
import Minigames.commands.HSBJoin;
import Minigames.listeners.JoinEvent;

public class minigamesMain extends JavaPlugin
{
	String sql;
	
	Statement SQL = null; 
	ResultSet resultSet = null;
	
	static minigamesMain instance;
	static FileConfiguration config;
	
	private String HOST;
	private int PORT;
	public String Database;
	private String USER;
	private String PASSWORD;
	public String STATS;
	
	public HideAndSeekLobby HSLobby;
	
	public Plugin plugin;
	
	Connection connection = null;
    
	boolean bIsConnected;
	
	private String DB_CON;

	@Override
	public void onEnable()
	{
		//Config Setup
		minigamesMain.instance = this;
		minigamesMain.config = this.getConfig();
		saveDefaultConfig();
		
		//MySQL
		boolean bSuccess;
		mysqlSetup();
		bSuccess = connect();
		
		bIsConnected = false;
		
		if (bSuccess)
		{
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[Minigames] MYSQL CONNECTED");
			bIsConnected = true;
		}

		//Creates the MySQL table if not already exists
		createStatsTable();
		
		//--------------------------------------
		//------------Create lobbies------------
		//--------------------------------------
		
		HSLobby = new HideAndSeekLobby(this);
		
		
		//--------------------------------------
		//--------------Listeners--------------
		//--------------------------------------
		
		new JoinEvent(this);
		
		
		//--------------------------------------
		//---------------Commands---------------
		//--------------------------------------
		getCommand("hide").setExecutor(new HSBJoin());
		
	}
	
	@Override
	public void onDisable()
	{
		disconnect();
	}
	
	public void mysqlSetup()
	{
		this.HOST = config.getString("MySQL_host");
		this.PORT = config.getInt("MySQL_port");
		this.Database = config.getString("MySQL_database");
		this.USER = config.getString("MySQL_username");
		this.PASSWORD = config.getString("MySQL_password");
		this.STATS = config.getString("MySQL_stats");
		
		this.DB_CON = "jdbc:mysql://" + this.HOST + ":" 
				+ this.PORT + "/" + this.Database;
	}
	
	public boolean connect() 
	{
		try
		{
		//	System.out.println(this.getClass().getName() +" : Connecting la la la");
			DriverManager.getDriver(DB_CON);
			connection = DriverManager.getConnection(DB_CON, USER, PASSWORD);
			if (this.connection != null)
				return true;
			else
				return false;
		}
		catch (SQLException e)
		{
			if (e.toString().contains("Access denied"))
			{
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - Access denied");			
				System.out.println("Access denied");
			}
			else if (e.toString().contains("Communications link failure"))
			{
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - Communications link failure");			
				System.out.println("Communications link failure");
			}
			else
			{
				e.printStackTrace();
				System.out.println(e.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - Other SQLException - "+e.getMessage());			
			}
			return false;
		}
		catch (Exception e)
		{
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - Other Exception - "+e.getMessage());			
			return false;
		}
	}
	public void disconnect()
	{
		try
		{
			if (bIsConnected)
			{
				this.connection.close() ;
				this.bIsConnected = false ;
			}
		//	System.err.println( this.getClass().getName() + ":: disconnected." ) ;
		}
		catch ( SQLException se )
		{
			System.err.println( this.getClass().getName() + ":: SQL error " + se ) ;
			se.printStackTrace() ;
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - SQLException - closing connection");			
		}
		catch ( Exception e )
		{
			System.err.println( this.getClass().getName() + ":: error " + e ) ;
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "minigamesMain - Exception - closing connection");			
			e.printStackTrace() ;
		}
		finally
		{
		}
		return ;
	}
	
	public boolean createStatsTable()
	{
		boolean bSuccess = false;
		int iCount = -1;

		try
		{
			//Adds a weather pref table
			sql = "CREATE TABLE IF NOT EXISTS `"+this.Database+"`.`"+this.STATS+"` (\n" + 
					"  `UUID` VARCHAR(36) NOT NULL,\n" + 
					"  `TimeStamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" + 
					"  `Game` VARCHAR(36) NOT NULL,\n" + 
					"  `Points` Int NOT NULL,\n" + 
					"   PRIMARY KEY (`UUID`,`TimeStamp`));";
			SQL = connection.createStatement();

			//Executes the update and returns how many rows were changed
			iCount = SQL.executeUpdate(sql);

			//If only 1 record was changed, success is set to true
			if (iCount == 1)
			{
				Bukkit.getConsoleSender().sendMessage("[Minigames]" +ChatColor.AQUA + "Created minigame stats table");			
				bSuccess = true;
			}
		}
		catch(SQLException se)
		{
			se.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return (bSuccess);
	}
	public static minigamesMain getInstance()
	{
		return instance;
	}
	
	public Connection getConnection()
	{
		try
		{
			if(connection.isClosed() || connection == null)
			{
				connect();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return connection;
	}
}
