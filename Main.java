package com.github.nes370.lolydealer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Loly Dealer, AKA Little Lamborgotti
 * 
 * <P>A Discord bot for Nitro Type users.
 * 
 * <P>Originally created to calculate account values, this bot features a full range of functionality.
 * </br>There are commands to look up user statistics, team information and leaderboards.
 * </br>It also has functions to bind Discord users to their Nitro Type accounts to allow for easier access of information, as well as doubling as a form of verification for private servers.
 * </br>This bot also is capable of providing a security check when new users join a server, to identify potential abuse based upon the account's creation date, profile picture, name and other info.
 * 
 * @version 2.20200930.0031
 * @author Nes370
 */
public class Main {

	private static String[][] commands = { //r is required, o is optional, p is previous required
			// General Commands
		{"help", "Show a list of bot commands and how they are used.",	"o[Command]", null, "General"},
		{"info", "Learn about this bot.", null, null, "General"},
		{"ping", "Check the bot's message latency.", null, null, "General"},
			// Registration Commands
		{"register", "Register an account to your Discord ID.",	"r[Username or URL]", null, "Registration"},
		{"unregister", "Remove a linked account from your Discord ID.",	null, null, "Registration"},
		{"verify", "Verify ownership of your linked account.", null, null, "Registration"},
		{"update", "Update your roles.", null, null, "Registration"},
			// Nitro Type Commands
		{"stats", "Display statistics of an account", "o[Racer]", null, "Nitro Type"},
		{"value", "Evaluate an account.", "o[Racer]", "p[Bonus]", "Nitro Type"},
		{"experience", "Display experience stats for a given level.", "o[Level]", null, "Nitro Type"},
		{"rank", "Display information about a ranking.", "o[Time]", "o[Rank]", "Nitro Type"},
		{"racelogs", "Inspect a racer's recent races.", "o[Racer]", null, "Nitro Type"},
		{"team", "Display team information.", "o[Team]", null, "Nitro Type"},
		{"leaderboard", "Display the lead racers of a scoreboard.", "o[Board]", "p[Time]", "Nitro Type"},
		{"teamboard", "Display the lead teams of a scoreboard.", "o[Board]", "p[Time]", "Nitro Type"},
		{"cacheboard", "Using stored data, show a particular leaderboard.", "r[Stat]", "o[Team]", "Nitro Type"},
		{"id", "Look up a user's Nitro Type User ID.", "o[Racer]", null, "Nitro Type"},
		//TODO {"compare", "Compare two accounts.", "r[username OR url OR @registeredDiscord]", "o[username OR url OR @registeredDiscord]"},
		//TODO {"garage", "List the cars owned by an account by value.", "o[username OR url OR @registeredDiscord]", "p[list length]"},
		//TODO {"sold", "List the cars sold from an account by value.", "o[username OR url OR @registeredDiscord]", "p[list length]"},
		//TODO {"carinfo", "Learn about a Nitro Type car.", "r[carID OR car name]", null},
			//Entertainment Commands
		{"minesweeper", "Start a game of Minesweeper.", "o[Mines]", "p[Difficulty]", "Entertainment"}
		
	}, adminCommands = {
		{"setprefix", "Change the bot prefix for this channel or specified channel.", "r[prefix]", "o[#channel]"},
		{"sudoregister", "Register a user to a Nitro Type account", "r[@discord]", "r[username OR url]"},
		// Use unregister @user to unregister the specified user.
		// Use update @user to update the specified user.
		{"puppet", "Send a message to a channel.", "r[#channel]", "r[message]"},
		{"serverlist", "Display a list of servers the bot is in.", null, null},
		{"leaveserver", "Have the bot leave a server.", "r[serverID]", null},
		{"shutdown", "Disconnect the bot, then turn off the bot.", null, null},
		{"restart", "Disconnect the bot, then reconnect.", null, null},
		{"clearcookies", "Reset cookies.", null, null},
		{"sendbio", "Send a biography embed", "r[message]", "r[message]"}
	};
	
	private static String[] errors = {
		"Empty command",
		"Unrecognized command",
		"Invalid arguments",
		"Parsing failure",
		"Runtime failure",
		"Log failure",
		"File read or write failure"
	};
	
	private static int[] counter = new int[commands.length], 
						 aCounter = new int[adminCommands.length],
						 eCounter = new int[errors.length],
						 lCounter = new int[4];
	
	public static DateTimeFormatter dtf;
	private static DiscordApi api;
	private static String token, developerID, credentials, resourcePath, deepAIKey, uhash;
	private static TextChannel logChannel;
	private static boolean logFound;
	private static long loginStamp;
	
	/**
	 * Sets the bot token and logs into Discord.
	 * Then message listeners are populated.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length > 0) {
			resourcePath = args[0];
		} else resourcePath = "C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/";
		
		setToken(resourcePath + "Token.txt");
		setDeveloperID(resourcePath + "Developer.txt");
		setCredentials(resourcePath + "Credentials.txt");
		setDeepAIKey(resourcePath + "DeepAIKey.txt");
		setUhash(resourcePath + "Uhash.txt");
		bookToShelf(resourcePath + "Book.json");
		
		if(api != null) {
			api.disconnect();
		}
		
		api = new DiscordApiBuilder()
				.setToken(getToken())
				.setAllIntentsExcept(Intent.GUILD_PRESENCES)
				.login()
				.join();
		
		dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		setLogChannel(api, resourcePath + "LogChannels.txt");
		systemLog("Logged in", Instant.ofEpochMilli(loginStamp = System.currentTimeMillis()));
		
		api.updateActivity(ActivityType.PLAYING, "Nitro Type");
		api.updateStatus(UserStatus.ONLINE);
		
		api.addMessageCreateListener(new Commands());
		
		api.addServerMemberJoinListener(new Overwatch(api.getServerById(455958968871813121L), api.getChannelById(477260764852518932L), api.getChannelById(455958968871813123L))); //Land of the bots
		api.addServerMemberJoinListener(new Overwatch(api.getServerById(564880536401870858L), api.getChannelById(567230957091028995L), api.getChannelById(564881373039689735L))); //NTV3 server, #mass-surveillance, #registration
		
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		
		Timer time = new Timer();
		NewsUpdate nu = new NewsUpdate(api.getServerTextChannelById(564889485591052307L).get());
		time.schedule(nu, 0, 1800000);
		
		System.out.println(getLogin(true));
		
	}
	
	/**
	 * Checks if the bot is logged in to Nitro Type.
	 * </br>Attempts to login to Nitro Type if not.
	 * 
	 * @return logged in to Nitro Type
	 */
	public static boolean getLogin(boolean first) {
		
		if (!first) try {
			if(System.currentTimeMillis() - Long.parseLong(readText(getResourcePath() + "loginStamp.txt")) < 3600000L)
				return true;
		} catch(Exception e) {}
		
		try {
			URL u = new URL("https://www.nitrotype.com/api/login");
			
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
			byte[] postData = (credentials).getBytes(StandardCharsets.UTF_8);
		
			DataOutputStream wr = new DataOutputStream(c.getOutputStream());
			wr.write(postData);
			wr.flush();
			wr.close();
		
			int responseCode = c.getResponseCode();
			System.out.println("\nSending 'POST' request to URL : " + u);
			System.out.println("Post parameters : " + postData);
			System.out.println("Response Code : " + responseCode);
		
			BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		
			if((boolean) ((JSONObject) new JSONParser().parse(response.toString())).get("success")) {
				OutputStreamWriter file = new OutputStreamWriter(new FileOutputStream(resourcePath + "loginStamp.txt"), StandardCharsets.UTF_8);
				file.write("" + System.currentTimeMillis());
				file.close();
				return true;
			}
		} catch (ParseException e) {
			System.out.println("ParseException");
			return false;
		} catch (IOException e) {
			System.out.println("IOException");
			return false;
		}
		return false;
	}
	
	/**
	 * Stores the temporary cache (book) into the permanent cache (shelf).
	 * 
	 * @param filepath of the temporary cache
	 */
	@SuppressWarnings("unchecked")
	public static void bookToShelf(String filepath) {
		
		JSONObject book = (JSONObject) readJSON(filepath);
		if(book.isEmpty())
			return;
		
		JSONObject shelf = (JSONObject) readJSON(resourcePath + "Shelf.json");
		shelf.putAll(book);
		book.clear();
		try {
			OutputStreamWriter file = new OutputStreamWriter(new FileOutputStream(resourcePath + "Shelf.json"), StandardCharsets.UTF_8);
			file.write(shelf.toJSONString());
			file.close();
			file = new FileWriter(filepath);
			file.write(book.toJSONString());
			file.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Prints a system event to the console and to the default log channel, if one is found.
	 * 
	 * @param event
	 * @param time
	 */
	private static void systemLog(String event, Instant time) {
		
		String consoleLog = event + " at " + dtf.format(LocalDateTime.ofInstant(time, ZoneId.systemDefault())) + ".";
		System.out.println(consoleLog);
		
		addLCounter(0);
		
		if(isLogFound())
			getLogChannel().sendMessage(new EmbedBuilder().setDescription(consoleLog).setColor(Color.GREEN).setFooter("System log #" + lCounter[0]));
		
	}
	
	/**
	 * Prints an error events to the console and edits the initial message sent to the log channel.
	 * 
	 * @param initialEvent
	 * @param logEntry
	 * @param index
	 * @param stackTrace
	 */
	public static void errorLog(EmbedBuilder initialEvent, CompletableFuture<Message> logEntry, int index, String stackTrace) {
		
		System.out.println(getError(index) + " error.");
		
		addECounter(index);
		
		if(isLogFound()) {
			if(stackTrace != null)
				initialEvent.addField("Stack Trace", "```" + stackTrace + "```");
			try {
				logEntry.get().edit(initialEvent.addField("Error", getError(index)).setColor(Color.RED));
			} catch (Exception e) {
				System.out.println("Unable to edit log entry.");
				e.printStackTrace();
				addECounter(5);
			}
		}
		
	}
	
	/**
	 * Prints an event to the console and returns an embed to be provided to a log channel.
	 * 
	 * @param type		The event source: 0 is system, 1 is administrator, 2 is user
	 * @param user		The command author
	 * @param content	A description of the event or message contents
	 * @param arguments The command and arguments used in this event
	 * @param time		When the event occurred
	 * @param server	The server where the command was received 
	 * @param channel	The channel where the command was received
	 * 
	 * @return EmbedBuilder	The log embed to be sent to the log channel
	 */
	public static EmbedBuilder commandLog(boolean isAdmin, Instant time, MessageAuthor author, String message, String[] arguments, Optional<Server> server, TextChannel channel) {
		
		String consoleLog = "";
		consoleLog = "At " + dtf.format(LocalDateTime.ofInstant(time, ZoneId.systemDefault())) + ", " + author + " used \"" + message + "\" in " + server + ", " + channel + ".";
		
		System.out.println(consoleLog);
		
		int type = (isAdmin) ? 1 : 2;
		addLCounter(type);
		
		if(isLogFound()) {
			
			EmbedBuilder embed = new EmbedBuilder().setDescription("```" + consoleLog + "```");
			
			if(type == 1)
				embed.setColor(new Color(138, 43, 226)).setFooter("Administrator command log #" + lCounter[type]);
			else if (type == 2)
				embed.setColor(new Color(30, 144, 255)).setFooter("User command log #" + lCounter[type]);
			
			if(server.isPresent())
				embed.addInlineField("Server", server.get().getName());
			embed.addInlineField("Channel", "<#" + channel.getId() + ">");
			embed.addInlineField("User", "<@" + author.getId() + ">");
			
			if(arguments.length > 0)
				embed.addInlineField("Command", arguments[0]);
			if(arguments.length > 1)
				embed.addInlineField("Argument 1", arguments[1]);
			if(arguments.length > 2)
				embed.addInlineField("argument 2", arguments[2]);
			
			return embed;
		}
		
		return null;
		
	}

	/**
	 * Reads the first line from the specified file and returns it as a String.
	 * 
	 * @param filepath	The path to the file to be read
	 * @return			A String of the first line of the specified file.
	 */
	public static String readText(String filepath) {
		
		try {
			
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), StandardCharsets.UTF_8));
			String content = in.readLine();
			in.close();
			
			return content;
		
		} catch(IOException e) {
			
			System.out.println("An error occured when attempting to read from " + filepath);
			e.printStackTrace();
			addECounter(6);
			
		}
		
		return "";
	}
	
	/**
	 * Attempts to parses the given file as a JSON object.
	 * 
	 * @param filepath
	 * @return An object that can be cast to JSONObject, else null.
	 */
	public static Object readJSON(String filepath) {
		
		JSONParser jsonparser = new JSONParser();
		Object obj = null;
		
		try {
			
			InputStreamReader reader = new InputStreamReader(new FileInputStream(filepath), StandardCharsets.UTF_8);
			obj = jsonparser.parse(reader);
		
		} catch(IOException e) {
			
			System.out.println("An error occured when attempting to read from " + filepath);
			e.printStackTrace();
			addECounter(6);
		
		} catch (ParseException e) {
			
			System.out.println("An error occured when attempting to parse from " + filepath);
			e.printStackTrace();
			addECounter(3);
		
		}
		
		return obj;
	}
	
	public static void clearCookies() {
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	}
	
	public static DiscordApi getApi() {
		return Main.api;
	}
	
	public static int getCommandsLength() {
		return commands.length;
	}
	
	public static String[] getCommand(int index) {
		return commands[index];
	}
	
	public static int getAdminCommandsLength() {
		return adminCommands.length;
	}
	
	public static String[] getAdminCommand(int index) {
		return adminCommands[index];
	}
	
	public static String getError(int index) {
		return errors[index];
	}
	
	public static int[] getCounter() {
		return counter;
	}
	
	public static void addCounter(int index) {
		Main.counter[index] += 1;
	}
	
	public static int[] getACounter() { 
		return aCounter;
	}
	
	public static void addACounter(int index) {
		Main.aCounter[index] += 1;
	}
	
	public static int[] getECounter() { 
		return eCounter;
	}
	
	public static void addECounter(int index) {
		Main.eCounter[index] += 1;
	}
	
	public static int[] getLCounter() {
		return lCounter;
	}
	
	public static void addLCounter(int index) {
		Main.lCounter[index] += 1;
	}
	
	public static String getToken() {
		return token;
	}
	
	public static void setToken(String filepath) {
		token = readText(filepath);
	}
	
	public static String getDeveloperID() { 
		return developerID;
	}
	
	public static void setDeveloperID(String filepath) {
		developerID = readText(filepath);
	}
	
	public static TextChannel getLogChannel() {
		return logChannel;
	}
	
	public static void setLogChannel(DiscordApi api, String filepath) {
		api.getTextChannelById(readText(filepath)).ifPresent(logChannel -> {
			Main.logChannel = logChannel;
			setLogFound(true);
		});
	}
	
	public static boolean isLogFound() {
		return logFound;
	}
	
	public static void setLogFound(boolean logFound) {
		Main.logFound = logFound;
	}
	
	public static long getLoginStamp() {
		return loginStamp;
	}
	
	public static void setLoginStamp(long loginStamp) {
		Main.loginStamp = loginStamp;
	}

	public static String getResourcePath() {
		return resourcePath;
	}

	public static void setResourcePath(String resourcePath) {
		Main.resourcePath = resourcePath;
	}

	public static void setCredentials(String filepath) {
		credentials = readText(filepath);
	}

	public static String getDeepAIKey() {
		return deepAIKey;
	}

	public static void setDeepAIKey(String filepath) {
		deepAIKey = readText(filepath);
	}
	
	public static String getUhash() {
		return uhash;
	}
	
	public static void setUhash(String filepath) {
		uhash = readText(filepath);
	}
	
}