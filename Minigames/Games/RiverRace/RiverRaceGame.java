package Minigames.Games.RiverRace;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import Minigames.Announce;
import Minigames.minigamesMain;
import Minigames.Games.Game;
import Minigames.Games.Gametype;

public class RiverRaceGame
{
	//Stores the details of the game as in the Games table of the database
	public final Game game;

	//Used for access to the lobby and main plugin
	private final minigamesMain plugin;
	protected RiverRaceLobby RRL;
	
	//Stores the lists of players and racers
	private ArrayList<Player> players;
	private ArrayList<RiverRaceRacer> racers = new ArrayList<RiverRaceRacer>();
	
	//Stores the map details
	private RiverRaceMap map;
	private RiverRaceCheckpoint[] DBCheckPoints;
	
	//Holds whether the game has been terminated
	public boolean bTerminate;
	public boolean bGamePlayStarted;
	
	//Scoreboard
	private ScoreboardManager SBM;
	private Scoreboard SB;
	private Objective objCheckpoints;
	
	//Listeners
	ArrayList<Checkpoint> ListenerCheckpoints;
	FinishLine FinishLine;
	
	public void reset()
	{
		this.players = new ArrayList<Player>();
		racers = new ArrayList<RiverRaceRacer>();
		map = new RiverRaceMap();
		ListenerCheckpoints = new ArrayList<Checkpoint>();
		FinishLine = null;
		bTerminate = false;
		bGamePlayStarted = false;
		
		objCheckpoints.setDisplaySlot(DisplaySlot.SIDEBAR);
		objCheckpoints.setRenderType(RenderType.INTEGER);
	}
	
	public ArrayList<Player> getPlayers()
	{
		return players;
	}
	
	public RiverRaceGame(minigamesMain plugin, RiverRaceLobby RRL, ArrayList<Player> players)
	{
		//Scoreboard
		SBM = Bukkit.getScoreboardManager();
		SB = SBM.getNewScoreboard();
		objCheckpoints = SB.registerNewObjective("Checkpoints", "dummy", ChatColor.BLUE +"Leaderboard");
		
		reset();
		
		//Sets up the Game class
		this.game = new Game(Gametype.River_Race);

		this.plugin = plugin;
		this.RRL = RRL;
		
		for (Player player : players)
		{
			this.players.add(player);
		}				
	}
	
	public void highGameProcesses()
	{
		int i;
		
		//Selects map
		int[] MapIDs = RiverRaceMap.MapIDs();
		
		if (MapIDs.length < 1)
		{
			terminate(TerminateType.No_Map_Found);
			return;
		}
		
		map = new RiverRaceMap(MapIDs[(int) ( Math.random() * MapIDs.length)]);
		map.setMapFromMapID();
		
		game.setMapID(map.getMapID());
		
		//Get checkpoints
		DBCheckPoints = RiverRaceCheckpoint.getAllForMapID(map.getMapID(), map.getWorld());
		
		if (DBCheckPoints.length == 0)
		{
			terminate(TerminateType.No_Checkpoints_Found);
			return;
		}
		
		//Set up start grids
		RiverRaceStartGrid[] startGridsArray = RiverRaceStartGrid.allStartGrids(map.getMapID());
		
		if (startGridsArray.length == 0)
		{
			terminate(TerminateType.No_Startgrids_Found);
			return;
		}
		
		ArrayList<RiverRaceStartGrid> startGrids = new ArrayList<RiverRaceStartGrid>();
		for (i = 0 ; i < startGridsArray.length ; i++)
		{
			startGrids.add(startGridsArray[i]);
		}
		
		//Declare variables
		int iRandom;
		RiverRaceStartGrid newGrid;
		Score newScore;
		
		//Set up racers
		for (i = 0 ; i < players.size() ; i++)
		{
			//Get a random grid from the list
			iRandom = (int) (Math.random() * startGrids.size());
			newGrid = startGrids.get(iRandom);
			Bukkit.getConsoleSender().sendMessage(map.getWorld().getName());
			
			//Set up the location of this grid
			Location location = new Location(map.getWorld(), newGrid.getX(), newGrid.getY(), newGrid.getZ());

			//Create new racer. Pass the location into the newBoat method.
			RiverRaceRacer newRacer = new RiverRaceRacer(players.get(i), newBoat(location), plugin);
			racers.add(newRacer);
			
			//Add them to scoreboard
			newScore = objCheckpoints.getScore(players.get(i).getName());
			newScore.setScore(0);
			Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] Score for "+players.get(i).getName() +" added");
			
			players.get(i).setScoreboard(SB);
		}

		//5 Second count down
		Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable()
		{
			int time = 5;

			@Override
			public void run()
			{
				//Terminates the process if the game has been terminated
				if (bTerminate)
				{
					return;
				}
				if (this.time == 0)
				{
					return;
				}
				Announce.announce(players, (ChatColor.GOLD +""+ ChatColor.BOLD +time));
				//	Announce.playNote(players, Sound.GLASS);		 
				this.time--;
			}
		}, 0L, 20L);
		
		//Starts gameplay after 5 seconds
		Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				if (bTerminate)
				{
					return;
				}
				//Actual gameplay
				bGamePlayStarted = true;
				game();
			}
		}, 5 * 20L);
		
		if (bTerminate)
		{
			return;
		}
	}
	
	private void game()
	{
		int i;
		
		//Set speed of boats to 2D
		for (i = 0 ; i < racers.size() ; i++)
		{
			racers.get(i).increaseBoatSpeed(2D);
		}
		game.setTimeStart();
		game.storeGameInDatabase();
		game.selectLastInsertID();
		
		Checkpoint newCheck;
		
		for (i = 0 ; i < DBCheckPoints.length - 1 ; i++)
		{
			newCheck = new Checkpoint(plugin, this, racers, DBCheckPoints[i].getNumber(), DBCheckPoints[i].getLocations());
			ListenerCheckpoints.add(newCheck);
		}
		
		FinishLine = new FinishLine(plugin, this, racers, DBCheckPoints[DBCheckPoints.length - 1].getNumber(), DBCheckPoints[DBCheckPoints.length - 1].getLocations());
		
	}

	private Boat newBoat(Location location)
	{
		Boat boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);
		boat.setWoodType(TreeSpecies.GENERIC);
		return boat;
	}
	
	//----------------------------------------
	//-------Deals with players leaving-------
	//----------------------------------------
	public void playerLeave(Player player)
	{
		int i;
		
		//Goes through the racers to find the correct racer
		for (i = 0 ; i < racers.size() ; i++)
		{
			//Once (if) the racer is found
			if (racers.get(i).getUUID().equals(player.getUniqueId()))
			{
				racers.remove(i);
				
				//Go through checkpoints and remove player
				for (i = 0 ; i < ListenerCheckpoints.size() ; i++)
				{
					ListenerCheckpoints.get(i).playerLeft(player);
				}
				
				//Break would be more efficient but if a problem has occured where they were added twice, this will be eleviated
				//break;
			}
		}
		
		//Update the players array - Used for announcements
		
		//Go through players and remove player from players
		for (i = 0 ; i < players.size() ; i++)
		{
			//Once the player is found
			if (players.get(i).getUniqueId().equals(player.getUniqueId()))
			{
				players.remove(i);
			}
		}
		
		if (racers.size() == 0)
		{
			terminate(TerminateType.Last_Player_Left);
		}
	}
	
	public void terminate(TerminateType Reason)
	{
		try
		{
			bTerminate = true;
			Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] Game terminated. Reason: "+Reason.toString());
			
			if (FinishLine != null)
			{
				FinishLine.unRegister();
				Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] Unregistered finish line");
			}
			
			Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] Checkpoints: " +ListenerCheckpoints.size());
			for (int i = 0; i < ListenerCheckpoints.size() - 1; i++)
			{
				ListenerCheckpoints.get(i).unRegister();
				Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] Unregistered index " +i);
			}
			
			//Announce end
			switch (Reason)
			{
			case All_Players_Finished:
				Announce.announce(getPlayers(), (ChatColor.GREEN +"All players have finished"));
				Announce.announce(getPlayers(), (ChatColor.GREEN +"1st: "+FinishLine.Finished.get(0).getPlayer().getName()));
				if (FinishLine.Finished.size() > 1)
					Announce.announce(getPlayers(), (ChatColor.GREEN +"2nd: "+FinishLine.Finished.get(1).getPlayer().getName()));
				if (FinishLine.Finished.size() > 2)
					Announce.announce(getPlayers(), (ChatColor.GREEN +"3rd: "+FinishLine.Finished.get(2).getPlayer().getName()));
				break;
			case Last_Player_Left:
				Announce.announce(getPlayers(), (ChatColor.GREEN +"The last player has left"));
				Announce.announce(getPlayers(), (ChatColor.GREEN +"1st: "+FinishLine.Finished.get(0).getPlayer().getName()));
				if (FinishLine.Finished.size() > 1)
					Announce.announce(getPlayers(), (ChatColor.GREEN +"2nd: "+FinishLine.Finished.get(1).getPlayer().getName()));
				if (FinishLine.Finished.size() > 2)
					Announce.announce(getPlayers(), (ChatColor.GREEN +"3rd: "+FinishLine.Finished.get(2).getPlayer().getName()));
				break;
			case No_Map_Found:
				Announce.announce(getPlayers(), (ChatColor.GOLD +"No river was found to race on"));
				break;
			case Manual:
				Announce.announce(getPlayers(), (ChatColor.GOLD +"The game was terminated"));
				break;
			case No_Checkpoints_Found:
				Announce.announce(getPlayers(), (ChatColor.GOLD +"No checkpoints for this map were found"));
				break;
			case No_Startgrids_Found:
				Announce.announce(getPlayers(), (ChatColor.GOLD +"No start for this map were found"));
				break;
			default:
				break;
			}

			//Records whether game play started in the log
			if (!bGamePlayStarted)
				Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] The game play did not start");
			else
				Bukkit.getConsoleSender().sendMessage("[Minigames] [River Race] The game play did start");

			//Wait 6 seconds before sending players back to the lobby
			Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable()
			{
				@Override
				public void run()
				{
					//Notifies lobby of game ending
					if (bGamePlayStarted)
						RRL.gameFinished(racers);
					else
						RRL.gameFinishedEarly(players);
				}
			}, 120L);

			//Checks whether game play started (i.e game added to DB)
			if (bGamePlayStarted)
			{
				//Record end of game in database
				game.setTimeEnd();
				game.storeGameEndInDatabase();
			}
		}
		catch (Exception e)
		{
			Bukkit.getConsoleSender().sendMessage("[Minigames] [RiverRaceGame] terminate() - Error teriminating game");
			e.printStackTrace();
		}
	}
}