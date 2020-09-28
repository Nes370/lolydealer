package com.github.nes370.lolydealer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class Commands implements MessageCreateListener {
	
	final long NITRO_TYPE_SERVER = 564880536401870858L,
			   _1X1_SERVER = 558349199230631946L,
			   ASCENSION_SERVER = 641680366528233514L,
			   BOK_SERVER = 641017355551506444L,
			   TBZ_SERVER = 648283326620368897L,
			   NT_OLYMPICS_SERVER = 692486582011166771L,
			   PARADISE_SERVER = 758144625592107021L;
	
	@Override
	public void onMessageCreate(MessageCreateEvent e) {
		
		String prefix = getChannelPrefix(e.getMessage().getChannel());
		//variable to edit a log message
		CompletableFuture<Message> logEntry = null;
		EmbedBuilder initialEvent;
		
		if(e.getMessage().getContent().toLowerCase().startsWith(prefix.toLowerCase())) {
			
			boolean isAdmin = e.getMessage().getAuthor().isBotOwner() || isAuthorized(e.getMessage().getChannel(), e.getMessage().getAuthor());
			String[] arguments = parseCommand(prefix, e.getMessage().getContent());
			initialEvent = Main.commandLog(isAdmin, e.getMessage().getCreationTimestamp(), e.getMessage().getAuthor(), e.getMessage().getContent(), arguments, e.getMessage().getServer(), e.getMessage().getChannel());
			
			boolean useLogChannel = initialEvent != null;
			if(useLogChannel)
				logEntry = Main.getLogChannel().sendMessage(initialEvent);
			
			if(arguments.length == 0) { //Error 0 Empty command
				e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.getError(0) + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
				Main.errorLog(initialEvent, logEntry, 0, Arrays.toString(Thread.currentThread().getStackTrace()));
				return;
			}
			
			String[] command = null;
			if(isAdmin)
				for(int i = 0; i < Main.getAdminCommandsLength(); i++)
					if(arguments[0].equalsIgnoreCase(Main.getAdminCommand(i)[0])) {
						command = Main.getAdminCommand(i);
						Main.addACounter(i);
					}
			if(command == null)
				for(int i = 0; i < Main.getCommandsLength(); i++)
					if(arguments[0].equalsIgnoreCase(Main.getCommand(i)[0])) {
						command = Main.getCommand(i);
						Main.addCounter(i);
					}
			
			if(command == null) { //Error 1 Unrecognized command
				e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.getError(1) + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
				Main.errorLog(initialEvent, logEntry, 1, Arrays.toString(Thread.currentThread().getStackTrace()));
				return;
			}
			
			String errorMessage;
			if((errorMessage = checkArguments(arguments, command, prefix, e)) != null) { //Error 2 Invalid Arguments
				e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(errorMessage).setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
				Main.errorLog(initialEvent, logEntry, 2, Arrays.toString(Thread.currentThread().getStackTrace()));
				return;
			}
			
			List<EmbedBuilder> embed = new ArrayList<EmbedBuilder>();
			
			switch(command[0]) {
			
			// Admin Commands
				case "shutdown":
					System.exit(0);
				
				case "restart":
					String[] args = {Main.getResourcePath()};
					Main.main(args);
					break;
				
				case "clearcookies":
					Main.clearCookies();
					break;
					
				case "serverlist":
					embed.addAll(serverList());
					break;
					
				case "leaveserver":
					if(arguments.length == 2)
						embed.add(leaveServer(arguments[1]));
					break;
			
				case "setprefix":
					try {
						if(arguments.length == 3)
							embed.add(setChannelPrefix(arguments[2], arguments[1], e.getMessage().getAuthor(), e.getMessage().getAuthor().getDisplayName()));
						else if(arguments.length == 2 )
							embed.add(setChannelPrefix(e.getMessage().getChannel().getIdAsString(), arguments[1], e.getMessage().getAuthor(), e.getMessage().getAuthor().getDisplayName()));
					} catch (IOException ioe) {
						ioe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(Thread.currentThread().getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(6) + ".\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch(NumberFormatException nfe) {
						nfe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(Thread.currentThread().getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid Channel Id.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					}
					break;
					
				case "sudoregister":
					try {
						embed.add(register(arguments[1], arguments[2], e.getMessage().getAuthor().getDisplayName(), true));
					} catch (IOException ioe) {
						ioe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(ioe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(6) + ".\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(nfe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid Discord Id.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (StringIndexOutOfBoundsException sioobe) {
						sioobe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(sioobe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid username or URL.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					}
					break;
					
				case "sendbio":
					if(isAdmin)
						embed.add(sendBio(e.getMessage().getContent()));
					break;
					
				case "puppet":
					embed.add(puppet(arguments[1], arguments[2], initialEvent, logEntry, e.getMessage().getAuthor().getDisplayName()));
					break;
					
			// General Commands
				case "help":
					if(arguments.length == 2) {
						embed.add(help(arguments[1], isAdmin, prefix));
					} else embed.addAll(help(isAdmin, prefix));
					break;
			
				case "info":
					embed.add(info(isAdmin));
					break;
				
				case "ping":
					try {
						Message pong = e.getMessage().getChannel().sendMessage("`pong`").get();
						pong.edit("`pong " + (pong.getCreationTimestamp().toEpochMilli() - e.getMessage().getCreationTimestamp().toEpochMilli()) + " ms`");
					} catch (InterruptedException | ExecutionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					break;
			
			// Registration Commands 
				case "register":
					try {
						embed.add(register(e.getMessage().getAuthor().getIdAsString(), arguments[1], e.getMessage().getAuthor().getDisplayName(), false));
					} catch (IOException ioe) {
						ioe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(ioe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(6) + ".\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(nfe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid Discord Id.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (StringIndexOutOfBoundsException sioobe) {
						sioobe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(sioobe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid username or URL.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					}
					break;
					
				case "unregister":
					if(!isAdmin) {
						try {
							embed.add(unregister(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getAuthor().getDisplayName()));
						} catch (IOException ioe) {
							ioe.printStackTrace();
							Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(ioe.getStackTrace()));
							embed.add(new EmbedBuilder().setDescription(Main.getError(6) + ".\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
						} catch (NumberFormatException nfe) {
							nfe.printStackTrace();
							Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(nfe.getStackTrace()));
							embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid Discord Id.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
						}
					} else {
						if(arguments.length == 2)
							try {
								if(arguments[1].startsWith("<@!") && arguments[1].endsWith(">"))
									arguments[1] = arguments[1].substring(3, arguments[1].indexOf(">"));
								else if(arguments[1].startsWith("<@") && arguments[1].endsWith(">"))
									arguments[1] = arguments[1].substring(2, arguments[1].indexOf(">"));
								if(!isLong(arguments[1]))
									throw new NumberFormatException();
								User user = Main.getApi().getUserById(arguments[1]).get();
								embed.add(unregister(user.getIdAsString(), user.getDisplayName(e.getServer().get())));
							} catch (StringIndexOutOfBoundsException | IOException | InterruptedException | ExecutionException e1) {
								// TODO Handle unregister errors
								e1.printStackTrace();
							}
						else
							try {
								embed.add(unregister(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getAuthor().getDisplayName()));
							} catch (StringIndexOutOfBoundsException | IOException e1) {
								// TODO Handle unregister errors
								e1.printStackTrace();
							}
					}
					break;
					
				case "verify":
					embed.add(verify(e.getMessage().getAuthor().getIdAsString(), prefix, initialEvent, logEntry, e.getMessage().getAuthor().getDisplayName()));
					break;
					
				case "update":
					if(!isAdmin) {
						try {
							embed.add(update(e.getMessage().getAuthor().asUser().get(), e.getMessage().getServer().get()));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException | InterruptedException | ExecutionException e1) {
							// TODO Handle admin update errors
							e1.printStackTrace();
						}
					} else {
						if(arguments.length == 2)
							try {
								if(arguments[1].startsWith("<@!") && arguments[1].endsWith(">"))
									arguments[1] = arguments[1].substring(3, arguments[1].indexOf(">"));
								else if(arguments[1].startsWith("<@") && arguments[1].endsWith(">"))
									arguments[1] = arguments[1].substring(2, arguments[1].indexOf(">"));
								if(!isLong(arguments[1]))
									throw new NumberFormatException();
								embed.add(update(Main.getApi().getUserById(arguments[1]).get(), e.getMessage().getServer().get()));
							} catch (StringIndexOutOfBoundsException | IOException | ParseException | InterruptedException | ExecutionException e1) {
								// TODO Handle update errors
								e1.printStackTrace();
							}
						else
							try {
								embed.add(update(e.getMessage().getAuthor().asUser().get(), e.getMessage().getServer().get()));
							} catch (StringIndexOutOfBoundsException | IOException | ParseException | InterruptedException | ExecutionException e1) {
								// TODO Handle update errors
								e1.printStackTrace();
							}
					}
					break;
					
			// Nitro Type Commands	
				case "cacheboard":
					if(arguments.length == 3) {
						embed.add(cacheboard(arguments[1], arguments[2]));
					} else embed.add(cacheboard(arguments[1], null));
					break;
					
				case "value":
					try {
						if(arguments.length == 3)
							embed.add(value(arguments[1], arguments[2]));
						else if(arguments.length == 2)
							embed.add(value(arguments[1], null));
						else embed.add(value(e.getMessage().getAuthor().getIdAsString(), null));
					} catch (IOException ioe) {
						//problem accessing racer info or cars info
						ioe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(ioe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(6) + ".\nThe requested information could not be retrieved from Nitro Type.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (ParseException pe) {
						//problem with data retrieved from site
						pe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(pe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid input in the form of `l.value [username OR url OR @registeredDiscord OR #userID]`. If you want to use `l.value` without any parameters, use the command `l.register [username]` first.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					} catch (StringIndexOutOfBoundsException sioobe) {
						sioobe.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(sioobe.getStackTrace()));
						embed.add(new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid input in the form of `l.value [username OR url OR @registeredDiscord OR #userID]`. If you want to use `l.value` without any parameters, use the command `l.register [username]` first.").setColor(Color.RED).setFooter("Error for " + e.getMessage().getAuthor().getDisplayName()));
					}
					break;
					
				case "leaderboard":
					if(arguments.length == 3)
						try {
							embed.add(leaderboard(arguments[1], arguments[2]));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else if(arguments.length == 2)
						try {
							embed.add(leaderboard(arguments[1], null));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else
						try {
							embed.add(leaderboard(null, null));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					break;
					
				case "teamboard":
					if(arguments.length == 3)
						try {
							embed.add(teamboard(arguments[1], arguments[2]));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else if(arguments.length == 2)
						try {
							embed.add(teamboard(arguments[1], null));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else
						try {
							embed.add(teamboard(null, null));
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					break;
					
				case "team":
					if(arguments.length == 2)
						try {
							embed.add(team(e.getMessage().getAuthor(), arguments[1]));
						} catch (IOException | ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else try {
						embed.add(team(e.getMessage().getAuthor(), null));
					} catch (IOException | ParseException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					break;
					
				case "rank":
					if(arguments.length == 3)
						try {
							embed.add(rank(e.getMessage().getAuthor().getId(), arguments[1], arguments[2]));
						} catch (StringIndexOutOfBoundsException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else if(arguments.length == 2)
						try {
							embed.add(rank(e.getMessage().getAuthor().getId(), arguments[1], null));
						} catch (StringIndexOutOfBoundsException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					else
						try {
							embed.add(rank(e.getMessage().getAuthor().getId(), null,  null));
						} catch (StringIndexOutOfBoundsException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (ParseException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					break;
					
				case "stats":
					if(arguments.length == 2)
						try {
							embed.add(stats(arguments[1]));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					else
						try {
							embed.add(stats(e.getMessage().getAuthor().getIdAsString()));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					break;
					
				case "racelogs":
					if(arguments.length == 2)
						try {
							embed.add(raceLogs(arguments[1]));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					else
						try {
							embed.add(raceLogs(e.getMessage().getAuthor().getIdAsString()));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					break;
					
				case "experience":
					if(arguments.length == 3) {
						embed.add(experience(arguments[1], arguments[2]));
					} else if(arguments.length == 2) {
						try {
							embed.add(experience(e.getMessage().getAuthor().getId(), arguments[1]));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else {
						try {
							embed.add(experience(e.getMessage().getAuthor().getId(), null));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					break;
					
				case "id":
					if(arguments.length == 2)
						try {
							embed.add(findID(arguments[1], e.getMessage().getServer().get()));
						} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (ExecutionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					else {
							try {
								embed.add(findID(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getServer().get()));
							} catch (StringIndexOutOfBoundsException | IOException | ParseException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
					}
					break;
					
			// Entertainment Commands
				case "minesweeper":
					if(arguments.length == 3)
						embed.add(minesweeper(arguments[1], arguments[2]));
					else if(arguments.length == 2)
						embed.add(minesweeper(arguments[1], "normal"));
					else embed.add(minesweeper("10", "normal"));
					break;
			
			// Unimplemented		
				case "racer":
					// TODO
					if(arguments.length == 2)
						embed.add(racer(arguments[1]));
					else embed.add(racer(e.getMessage().getAuthor().getIdAsString()));
					break;
					
				case "compare":
					//TODO
					if(arguments.length == 3)
						embed.add(compare(arguments[1], arguments[2]));
					else embed.add(compare(e.getMessage().getAuthor().getIdAsString(), arguments[1]));
					break;
					
				case "garage":
					//TODO
					if(arguments.length == 3)
						embed.addAll(garage(arguments[1], arguments[2]));
					else if(arguments.length == 2)
						embed.addAll(garage(arguments[1], null));
					else embed.addAll(garage(e.getMessage().getAuthor().getIdAsString(), null));
					break;
					
				case "sold":
					//TODO
					if(arguments.length == 3)
						embed.addAll(sold(arguments[1], arguments[2]));
					else if(arguments.length == 2)
						embed.addAll(sold(arguments[1], null));
					else embed.addAll(sold(e.getMessage().getAuthor().getIdAsString(), null));
					
			}
			
			if(embed.size() > 0)
			for(int i = 0; i < embed.size(); i++)
				e.getMessage().getChannel().sendMessage(embed.get(i));
			
		} else return;
		
	}
	
	/**
	 * 
	 * @param racer
	 * @param server
	 * @return
	 * @throws StringIndexOutOfBoundsException
	 * @throws IOException
	 * @throws ParseException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@SuppressWarnings("unchecked")
	private EmbedBuilder findID(String racer, Server server) throws StringIndexOutOfBoundsException, IOException, ParseException, InterruptedException, ExecutionException {
		
		System.out.printf("findID(%s, %s)%n", racer, server.getName());
		
		// TODO determine if argument is a Discord ID
		long userID;
		String username, displayName = null;
		//boolean discordParameter = false;
		
		if(racer.startsWith("<@!") && racer.endsWith(">"))
			racer = racer.substring(3, racer.indexOf(">"));
		else if(racer.startsWith("<@") && racer.endsWith(">"))
			racer = racer.substring(2, racer.indexOf(">"));
		if(isLong(racer) && racer.length() == 18) {
			JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
			for(int i = 0; i < register.size(); i++) {
				JSONObject e = (JSONObject) register.get(i);
				if(((Long) e.get("discordId")).longValue() == Long.parseLong(racer)) {
					if(e.containsKey("UserID")) {
						userID = ((Long) e.get("UserID")).longValue();
						username = (String) e.get("username");
					} else { //give it an entry
						JSONObject racerInfo = getRacerInfo(racer);
						userID = ((Long) racerInfo.get("userID")).longValue();
						username = (String) racerInfo.get("username");
						e.put("UserID", userID);
						e.put("username", username);
						register.remove(i);
						register.add(e);
						FileWriter file = new FileWriter(Main.getResourcePath() + "Register.json");
						file.write(register.toJSONString());
						file.close();
						displayName = (String) racerInfo.get("displayName");
						if(displayName == null || displayName.equals(""))
							displayName = username;
					}
					User user = Main.getApi().getUserById(racer).get();
					EmbedBuilder embed = new EmbedBuilder()
							.addField("Discord ID", racer)
							.addField("Discord Username", user.getDiscriminatedName())
							.addField("Discord Display Name", user.getDisplayName(server))
							.addField("Discord Mention", "<@" + racer + ">")
							.addField("Nitro Type ID", Long.toString(userID) + " [🔗](https://test.nitrotype.com/api/players/" + Long.toString(userID) + ")")
							.addField("Nitro Type Username", username + " [🔗](https://www.nitrotype.com/racer/" + username + ")")
							.setColor(Color.GREEN);
					if(displayName != null)
						embed.addField("Nitro Type Display Name", displayName);
					return embed;
				}
			}
		} //assume it is a username
		JSONObject racerInfo = getRacerInfo(racer);
		userID = ((Long) racerInfo.get("userID")).longValue();
		username = (String) racerInfo.get("username");
		displayName = (String) racerInfo.get("displayName");
		if(displayName == null || displayName.equals(""))
			displayName = username;
		return new EmbedBuilder()
				.addField("Nitro Type ID", Long.toString(userID) + " [🔗](https://test.nitrotype.com/api/players/" + Long.toString(userID) + ")")
				.addField("Nitro Type Username", username + " [🔗](https://www.nitrotype.com/racer/" + username + ")")
				.addField("Nitro Type Display Name", displayName)
				.setColor(Color.GREEN);
	}
	
	/**
	 * 
	 * @param string
	 * @return
	 */
	private EmbedBuilder sendBio(String string) {
		
		System.out.println("sendBio(formatted text)");
		
		if(string.startsWith("l.sendbio "))
			string = string.substring(10);
		if(string.startsWith("```"))
			string = string.substring(3);
		if(string.endsWith("```"))
			string = string.substring(0, string.length() - 3);
		JSONObject bio = null;
		try {
			bio = (JSONObject) new JSONParser().parse(string);
		} catch(ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(bio.containsKey("embed")) {
			bio = (JSONObject) bio.get("embed");
			EmbedBuilder embed = new EmbedBuilder();
			if(bio.containsKey("title")) {
				embed.setTitle((String) bio.get("title"));
				System.out.println((String) bio.get("title") + " title set");
			}
			if(bio.containsKey("description")) {
				embed.setDescription((String) bio.get("description"));
				System.out.println((String) bio.get("description") + " description set");
			}
			if(bio.containsKey("url")) {
				embed.setUrl((String) bio.get("url"));
				System.out.println((String) bio.get("url") + " url set");
			}
			if(bio.containsKey("color")) {
				embed.setColor(new Color((int) (long) bio.get("color")));
				System.out.println((int) (long) bio.get("color") + " color set");
			}
			if(bio.containsKey("timestamp"));
				//nothing
			if(bio.containsKey("footer"));
				//nothing
			if(bio.containsKey("thumbnail")) {
				embed.setThumbnail((String) ((JSONObject) bio.get("thumbnail")).get("url"));
				System.out.println((String) ((JSONObject) bio.get("thumbnail")).get("url") + " thumbnail set");
			}
			if(bio.containsKey("image"));
				//nothing
			if(bio.containsKey("author")) {
				JSONObject author = (JSONObject) bio.get("author");
				embed.setAuthor((String) author.get("name"), (String) author.get("url"), (String) author.get("icon_url"));
				System.out.println((String) author.get("name") + " " + (String) author.get("url") + " " + (String) author.get("icon_url") + " author set");
			}
			if(bio.containsKey("fields")) {
				JSONArray fields = (JSONArray) bio.get("fields");
				for(Object field : fields) {
					if(((JSONObject) field).containsKey("inline"))
						embed.addInlineField((String) ((JSONObject) field).get("name"), (String) ((JSONObject) field).get("value"));
					else embed.addField((String) ((JSONObject) field).get("name"), (String) ((JSONObject) field).get("value"));
					System.out.println((String) ((JSONObject) field).get("name") + " " + (String) ((JSONObject) field).get("value") + " field set");
				}
			}
			return embed;
		}
		return null;
	}
	//@SuppressWarnings("unchecked")
	private EmbedBuilder cacheboard(String stat, String team) {
		
		System.out.printf("cacheboard(%s, %s)%n", stat, team);
		
		Main.bookToShelf(Main.getResourcePath() + "Book.json");
		JSONObject shelf = (JSONObject) Main.readJSON(Main.getResourcePath() + "Shelf.json");
		System.out.println("Data size: " + shelf.size());
		stat = stat.toLowerCase();
		int type = 0;
		if(stat.contains("races")) type = 1;			//racesPlayed
		else if(stat.contains("accuracy")) type = 2; //if(racesPlayed > 500) typed - errs / typed
		else if(stat.contains("first")) type = 3; 	//placed1
		else if(stat.contains("second"))  type = 4;	//placed2
		else if(stat.contains("third")) type = 5;	//placed3
		else if(stat.contains("spent")) type = 19;
		else if(stat.contains("money")) type = 6;	//money
		else if(stat.contains("high")) type = 7; 	//highestSpeed
		else if(stat.contains("speed"))  type = 8;	//avgSpeed
		else if(stat.contains("active")) type = 18; 
		else if(stat.contains("age")) type = 9;		//createdStamp formatted
		else if(stat.contains("nonbonus")) type = 25; //TODO
		else if(stat.contains("bonus")) type = 26; //TODO
		else if(stat.contains("experience") || stat.contains("level")) type = 10;	//sorted by experience, but display levels too
		else if(stat.contains("sold")) type = 20;
		else if(stat.contains("totalcars")) type = 21;
		else if(stat.contains("cars")) type = 11;		//totalCars
		else if(stat.contains("time")) type = 12;		//playTime
		else if(stat.contains("view")) type = 13;	//profileViews
		else if(stat.contains("achievement")) type = 14;	//achievementPoints
		else if(stat.contains("used")) type = 17; //nitrosUsed
		else if(stat.contains("nitro")) type = 15;	//nitros
		else if(stat.contains("session")) type = 16;	//longestSeassion
		else if(stat.contains("rank")) type = 22;
		else if(stat.contains("density")) type = 23;
		else if(stat.contains("value")) type = 24;
		//else if(stat.contains("point")) type = 27; //TODO
		ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		System.out.println("Created list container, type: " + type);
		JSONArray carInfo = null;
		if(type == 24)
			try {
				carInfo = getCarInfo();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		for(Object key : shelf.keySet()) {
			JSONObject data = (JSONObject) shelf.get(key);
			String name;
			String username = name = (String) data.get("username");
			if(data.get("displayName") != null && !data.get("displayName").equals(""))
				name = (String) data.get("displayName");
			boolean filter = false;
			if(team != null)
				filter = true;
			String tag = null;
			if(data.get("tag") != null && !data.get("tag").equals("")) {
				tag = (String) data.get("tag");
				if(team != null && team.equalsIgnoreCase(tag))
					filter = false;
			}
			if(filter) {} else if(type == 1) {
				long racesPlayed = 0;
				if(data.get("racesPlayed") != null) {
					racesPlayed = (long) data.get("racesPlayed");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", racesPlayed);
				list.add(entry);
			} else if(type == 2) { //acc
				double accuracy = 0.0;
				long typed = 0;
				long errs = 0;
				JSONArray raceLogs = (JSONArray) data.get("raceLogs");
				if(raceLogs.size() > 0) {
					for(int i = 0; i < raceLogs.size(); i++) {
						JSONObject log = (JSONObject) raceLogs.get(i);
						if(log.get("typed") != null)
							typed += (long) log.get("typed");
						if(log.get("errs") != null)
							errs += (long) log.get("errs");
					}
				}
				if(errs != 0) {
					accuracy = ((int) ((typed - errs) * 100000.0 / typed)) / 1000.0;
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", accuracy);
				list.add(entry);
			} else if(type == 3) {
				long placed1 = 0;
				if(data.get("placed1") != null) {
					placed1 = (long) data.get("placed1");
				}
				long racesPlayed = 0;
				if(data.get("racesPlayed") != null) {
					racesPlayed = (long) data.get("racesPlayed");
				}
				if(racesPlayed < 30) placed1 = 0;
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", ((int) (placed1 * 100000.0 / racesPlayed)) / 1000.0);
				entry.put("value2", placed1);
				entry.put("value3", racesPlayed);
				list.add(entry);
			} else if(type == 4) { //placed2
				long placed2 = 0;
				if(data.get("placed2") != null) {
					placed2 = (long) data.get("placed2");
				}
				long racesPlayed = 0;
				if(data.get("racesPlayed") != null) {
					racesPlayed = (long) data.get("racesPlayed");
				}
				if(racesPlayed < 30) placed2 = 0;
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", ((int) (placed2 * 100000.0 / racesPlayed)) / 1000.0);
				entry.put("value2", placed2);
				entry.put("value3", racesPlayed);
				list.add(entry);
			} else if(type == 5) {
				long placed3 = 0;
				if(data.get("placed3") != null) {
					placed3 = (long) data.get("placed3");
				}
				long racesPlayed = 0;
				if(data.get("racesPlayed") != null) {
					racesPlayed = (long) data.get("racesPlayed");
				}
				if(racesPlayed < 30) placed3 = 0;
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", ((int) (placed3 * 100000.0 / racesPlayed)) / 1000.0);
				entry.put("value2", placed3);
				entry.put("value3", racesPlayed);
				list.add(entry);
			} else if(type == 6) {
				long money = 0;
				if(data.get("money") != null) {
					money = (long) data.get("money");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", money);
				list.add(entry);
			}  else if(type == 7) {
				long highestSpeed = 0;
				if(data.get("highestSpeed") != null) {
					highestSpeed = (long) data.get("highestSpeed");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", highestSpeed);
				list.add(entry);
			}  else if(type == 8) {
				long avgSpeed = 0;
				if(data.get("avgSpeed") != null) {
					avgSpeed = (long) data.get("avgSpeed");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", avgSpeed);
				list.add(entry);
			} else if(type == 9) {
				long createdStamp = 0;
				if(data.get("createdStamp") != null) {
					createdStamp = (long) data.get("createdStamp");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", createdStamp);
				list.add(entry);
			} else if(type == 10) {
				long experience = 0;
				if(data.get("experience") != null) {
					experience = (long) data.get("experience");
				}
				long level = 0;
				if(data.get("level") != null) {
					level = (long) data.get("level");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", experience);
				entry.put("value2", level);
				list.add(entry);
			} else if(type == 11) {
				long totalCars = 0;
				if(data.get("totalCars") != null) {
					totalCars = (long) data.get("totalCars");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", totalCars);
				list.add(entry);
			} else if(type == 12) {
				long playTime = 0;
				if(data.get("playTime") != null) {
					playTime = (long) data.get("playTime");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", playTime);
				list.add(entry);
			} else if(type == 13) {
				long profileViews = 0;
				if(data.get("profileViews") != null) {
					profileViews = (long) data.get("profileViews");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", profileViews);
				list.add(entry);
			} else if(type == 14) {
				long achievementPoints = 0;
				if(data.get("achievementPoints") != null) {
					achievementPoints = (long) data.get("achievementPoints");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", achievementPoints);
				list.add(entry);
			} else if(type == 15) {
				long nitros = 0;
				if(data.get("nitros") != null) {
					nitros = (long) data.get("nitros");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", nitros);
				list.add(entry);
			} else if(type == 16) {
				long longestSession = 0;
				if(data.get("longestSession") != null) {
					longestSession = (long) data.get("longestSession");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", longestSession);
				list.add(entry);
			} else if(type == 17) {
				long nitrosUsed = 0;
				if(data.get("nitrosUsed") != null) {
					nitrosUsed = (long) data.get("nitrosUsed");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", nitrosUsed);
				list.add(entry);
			} else if(type == 18) {
				long createdStamp = 0;
				if(data.get("createdStamp") != null) {
					createdStamp = (long) data.get("createdStamp");
				}
				long recentRaces = 0;
				if(data.get("racingStats") != null) {
					JSONArray racingStats = (JSONArray) data.get("racingStats");
					if(racingStats.size() > 0)
						for(int i = 0; i < racingStats.size(); i++) {
							JSONObject racingStat = (JSONObject) racingStats.get(i);
							recentRaces += (long) racingStat.get("played");
						}
				}
				if(recentRaces == 0)
					createdStamp = System.currentTimeMillis() / 1000;
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", createdStamp);
				list.add(entry);
			} else if(type == 19) {
				long moneySpent = 0;
				if(data.get("moneySpent") != null) {
					moneySpent = (long) data.get("moneySpent");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", moneySpent);
				list.add(entry);
			} else if(type == 20) {
				long soldCars = 0;
				if(data.get("cars") != null) {
					soldCars = ((JSONArray) data.get("cars")).size() - (Long) data.get("totalCars");
				//	if(cars.size() > 0)	for(int i = 0; i < cars.size(); i++) { if(!(boolean) ((JSONArray) cars.get(i)).get(1)) soldCars++; }
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", soldCars);
				list.add(entry);
			} else if(type == 21) {
				long cars = 0;
				if(data.get("cars") != null)
					cars = ((JSONArray) data.get("cars")).size();
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", cars);
				list.add(entry);
			} else if(type == 22) {
				long totalRank = 0;
				int boards = 0;
				if(data.get("racingStats") != null) {
					JSONArray racingStats = (JSONArray) data.get("racingStats");
					if(racingStats.size() > 0)
						for(int i = 0; i < racingStats.size(); i++) {
							JSONObject racingStat = (JSONObject) racingStats.get(i);
							if(racingStat.get("rank") != null) {
								totalRank += (long) racingStat.get("rank");
								boards++;
							}
						}
				}
				if(totalRank == 0 || boards == 0) {
					totalRank = Integer.MAX_VALUE;
					boards = 1;
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", totalRank * 1.0 / boards);
				list.add(entry);
			} else if(type == 23) {
				double raceDensity = 0.0;
				long racesPlayed = 0;
				long createdStamp = 0;
				if(data.get("racesPlayed") != null)
					racesPlayed = (long) data.get("racesPlayed");
				if(data.get("createdStamp") != null)
					createdStamp = (long) data.get("createdStamp");
				if(racesPlayed > 0 && createdStamp > 0)
					raceDensity = racesPlayed / ((System.currentTimeMillis() / 1000 - createdStamp) / 86400.0);
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", raceDensity);
				list.add(entry);
			} else if(type == 24) {
				long money = (long) data.get("money"), 
						experience = (long) data.get("experience"), 
						racesPlayed = (long) data.get("racesPlayed"),
						placed1 = (long) data.get("placed1"),
						placed2 = (long) data.get("placed2"),
						placed3 = (long) data.get("placed3"),
						nitros = (long) data.get("nitros"), 
						longestSession = (long) data.get("longestSession"),
						highestSpeed = (long) data.get("highestSpeed");
				byte gold = 0;
				if(((String) data.get("membership")).equals("gold"))
					gold++;
				double age = ((System.currentTimeMillis() / 1000) - (Long) data.get("createdStamp")) / 31536000.0;
				JSONArray cars = (JSONArray) data.get("cars");
				long liquidCars = 0, subjectiveCars = 0;
				
				for(int i = 0; i < cars.size(); i++) {
					JSONArray car = (JSONArray) cars.get(i);
					if(((String) car.get(1)).equals("owned")) {
						liquidCars += 0.6 * carValue(carInfo, (long) car.get(0));
						subjectiveCars += 0.4 * carValue(carInfo, (long) car.get(0)) * Math.pow(2, (System.currentTimeMillis() / 1000 - lastObtainable((int) (long) car.get(0))) / 31536000.0);
					} else if(System.currentTimeMillis() / 1000 - 41688324 > lastObtainable((int) (long) car.get(0)))
						subjectiveCars += carValue(carInfo, (long) car.get(0)) * (0.4 * Math.pow(2, (System.currentTimeMillis() / 1000 - lastObtainable((int) (long) car.get(0))) / 31536000.0) - 1);
				}
				
				long liquid = money + liquidCars;
				long subjective = (long) ((gold * 10000000) + experience + (racesPlayed * 750 + placed1 * 500 + placed2 * 333 + placed3 * 167) + (longestSession * 2000) + subjectiveCars + (50000 * Math.pow(2, (highestSpeed - 40) / 20.56) - 12981) + (50000 * Math.pow(2, age))) + (nitros * 500);
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				entry.put("value", liquid + subjective);
				list.add(entry);
			} else if(type == 25 || type == 26) {//nonbonus
				long experience = 0;
				if(data.get("experience") != null) {
					experience = (long) data.get("experience");
				}
				long racesPlayed = 0;
				if(data.get("racesPlayed") != null) {
					racesPlayed = (long) data.get("racesPlayed");
				}
				long placed3 = 0;
				if(data.get("placed3") != null) {
					placed3 = (long) data.get("placed3");
				}
				long placed2 = 0;
				if(data.get("placed2") != null) {
					placed2 = (long) data.get("placed2");
				}
				long placed1 = 0;
				if(data.get("placed1") != null) {
					placed1 = (long) data.get("placed1");
				}
				final Map<String, Object> entry = new HashMap<>();
				entry.put("name", name);
				entry.put("username", username);
				entry.put("tag", tag);
				if(type == 25)
					entry.put("value", racesPlayed * 1000 + placed3 * 30 + placed2 * 50 + placed1 * 100);
				else if(type == 26)
					entry.put("value", experience - (racesPlayed * 1000 + placed3 * 30 + placed2 * 50 + placed1 * 100));
				entry.put("value2", experience);
				list.add(entry);
			} //else if(type == 27) {}
		}
		System.out.println("Compiled List");
		Comparator<Map<String, Object>> longCompare = new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> a, Map<String, Object> b) {
				return Long.compare((long) a.get("value"), (long) b.get("value"));
			}
		};
		Comparator<Map<String, Object>> doubleCompare = new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> a, Map<String, Object> b) {
				return Double.compare((double) a.get("value"), (double) b.get("value"));
			}
		};
		int size = list.size();
		if(size > 25) size = 25;
		EmbedBuilder embed = new EmbedBuilder();
		String filter = "";
		if(team != null)
			filter = team.toUpperCase() + " ";
		if(type == 1) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Races [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Total Races Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
			System.out.println("Compiled Embed");
		} else if(type == 2) {
			Collections.sort(list, doubleCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + (double) data.get("value") + "%** Accuracy [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Last 30 Races Accuracy Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 3) {
			Collections.sort(list, doubleCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + (double) data.get("value") + "%** First Place Finishes, **" + (long) data.get("value2") + "** of **" + (long) data.get("value3") + "** races [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "First Place Finish Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 4) {
			Collections.sort(list, doubleCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + (double) data.get("value") + "%** Second Place Finishes, **" + (long) data.get("value2") + "** of **" + (long) data.get("value3") + "** races [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Second Place Finish Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 5) {
			Collections.sort(list, doubleCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + (double) data.get("value") + "%** Third Place Finishes, **" + (long) data.get("value2") + "** of **" + (long) data.get("value3") + "** races [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Third Place Finish Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 6) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**$" + numberToText((long) data.get("value")) + "** Cash Owned [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Money Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 7) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** WPM [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "High Speed Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 8) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** WPM [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Average Speed Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 9) {
			Collections.sort(list, longCompare);
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli((long) data.get("value") * 1000), ZoneId.systemDefault())) + "** [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Age Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 10) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "Level **" + numberToText((long) data.get("value2")) + "**, **" + numberToText((long) data.get("value")) + "** Experience [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Level Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 11) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Cars Owned [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Cars Owned Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 12) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "";
				long secs = (long) data.get("value");
				if(secs / 31557600 > 0)
					body += "**" + secs / 31557600 + "** years ";
				if(secs % 31557600 / 86400 > 0)
					body += "**" + secs % 31557600 / 86400 + "** days ";
				if(secs % 31557600 % 86400 / 3600 > 0)
					body += "**" + secs % 31557600 % 86400 / 3600 + "** hours ";
				if(secs % 31557600 % 86400 % 3600 / 60 > 0)
					body += "**" + secs % 31557600 % 86400 % 3600 / 60 + "** minutes ";
				if(secs % 31557600 % 86400 % 3600 % 60 > 0)
					body += "**" + secs % 31557600 % 86400 % 3600 % 60 + "** seconds ";
				body += "played [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Time Played Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 13) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Profile Views [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Profile Views Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 14) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Achievement Points [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Achievement Points Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 15) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Nitros Owned [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Nitros Owned Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 16) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Races [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Longest Session Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 17) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Nitros Used [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Nitros Used Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 18) {
			Collections.sort(list, longCompare);
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli((long) data.get("value") * 1000), ZoneId.systemDefault())) + "** [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Oldest Active Player Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 19) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**$" + numberToText((long) data.get("value")) + "** Cash Spent [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Money Spent Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 20) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Cars Sold [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Cars Sold Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 21) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Cars Owned or Sold [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Cars Owned or Sold Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 22) {
			Collections.sort(list, doubleCompare);
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "Average Rank **" + ((int) ((double) data.get("value") * 1000.0)) / 1000.0 + "** [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Average Rank Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 23) {
			Collections.sort(list, doubleCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + ((int) ((double) data.get("value") * 1000.0)) / 1000.0 + "** Races/Day [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Race Density Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 24) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**$" + numberToText((long) data.get("value")) + "** [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Account Value Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 25) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Non-Bonus Experience, **" + numberToText((long) data.get("value2")) + "** Total Experience [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Non-Bonus Experience Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		} else if(type == 26) {
			Collections.sort(list, longCompare.reversed());
			System.out.println("Sorted list");
			for(int i = 0; i < size; i++) {
				Map<String, Object> data = list.get(i);
				String head = i + 1 + ". ";
				if(data.get("tag") != null)
					head += "[" + data.get("tag") + "] ";
				head += data.get("name");
				String body = "**" + numberToText((long) data.get("value")) + "** Bonus Experience, **" + numberToText((long) data.get("value2")) + "** Total Experience [\\🔗](https://www.nitrotype.com/racer/" + data.get("username") + ")";
				embed.setTitle(filter + "Bonus Experience Cacheboard").addField(head, body).setColor(new Color(0xa368e6));
			}
		}
		System.out.println("Compiled Embed");
		return embed;
	}
	
	/**
	 * 
	 * @param id
	 * @param time
	 * @param targetRank
	 * @return
	 * @throws StringIndexOutOfBoundsException
	 * @throws IOException
	 * @throws ParseException
	 */
	private EmbedBuilder rank(long id, String time, String targetRank) throws StringIndexOutOfBoundsException, IOException, ParseException {
		
		System.out.printf("rank(%d, %s, %s)%n", id, time, targetRank);
		
		if(time == null) time = "season";
		JSONObject racerInfo = getRacerInfo("" + id);
		JSONArray racingStats = (JSONArray) racerInfo.get("racingStats");
		long rank = 0;
		long played = 0;
		long typed = 0;
		long errs = 0;
		double secs = 0;
		if(racingStats.size() > 0)
		A: for(int i = 0; i < racingStats.size(); i++) {
			JSONObject data = (JSONObject) racingStats.get(i);
			if(((String) data.get("board")).equalsIgnoreCase(time)) {
				rank = (long) data.get("rank");
				played = (long) data.get("played");
				typed = (long) data.get("typed");
				errs = (long) data.get("errs");
				secs = Double.parseDouble((String) data.get("secs"));
				break A;
			}
		}
		if(time == null)
			time = "season";
		int rank2 = (int) (rank - 1);
		if(rank2 < 1) rank2 = 2;
		if(targetRank != null) {
			try {
				 rank2 = Integer.parseInt(targetRank);
			} catch(NumberFormatException nfe) {
				throw new NumberFormatException();
			}
		}
		if(rank2 > 100) {
			rank2 = 100;
		}
		
		int points = (int) (played * (100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed + 0.5);
		JSONObject scoreboardInfo = getScoreboard("points", time, "racer", null);
		JSONObject data2 = (JSONObject) ((JSONArray) scoreboardInfo.get("scores")).get(rank2 - 1);
		String name = (String) data2.get("displayName");
		if(name == null || name.equals(""))
			name = (String) data2.get("username");

		long points2 = (long) data2.get("points");
		
		EmbedBuilder embed = new EmbedBuilder();
		
		String description = "In the " + time + " leaderboard, you are at rank **" + numberToText(rank) + "** with **" + numberToText(points) + "** points.\n**"
				+ name + "** is at rank **" + rank2 + "** with **" + numberToText(points2) + "** points.\n";
		if(rank2 < rank || rank == 0) {
			description += "You need **" + numberToText(points2 - points) + "** more points, or about **" 
					+ numberToText((int) ((points2 - points) / (100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed + 0.5))
					+ "** more races to overtake them";
		} else description += "You have a lead of **" + numberToText(points - points2) + "** points, or about **" 
					+ numberToText((int) ((points - points2) / (100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed + 0.5)) + "** races.";
		
		return embed.setDescription(description).setColor(Color.GREEN);
	}
	
	/**
	 * 
	 * @param argument
	 * @param isAdmin
	 * @param prefix
	 * @return
	 */
	private EmbedBuilder help(String argument, boolean isAdmin, String prefix) {
		
		System.out.printf("help(%s, %b, %s)%n", argument, isAdmin, prefix);
		
		EmbedBuilder embed = new EmbedBuilder();
		System.out.println("for()");
		for(int i = 0; i < Main.getCommandsLength(); i++) {
			String[] command = Main.getCommand(i);
			if(command[0].equalsIgnoreCase(argument)) {
				System.out.println("match found");
				embed.setAuthor("Help");
				if(command[2] == null) {
					return embed.addField(command[0], command[1] + "\n" + prefix + command[0]).setColor(Color.YELLOW);
				} else if(command[3] == null) {
					embed.addField(command[0], command[1] + "\n" + prefix + command[0] + " " + command[2].substring(1));
					String argTitle = "Required parameter";
					if(command[2].startsWith("o")) {
						argTitle = "Optional parameter";
					}
					String argDesc = "**" + command[2].substring(2, command[2].length() - 1) + "**```Diff\n" + describeParameter(command[2]) + "```";
					return embed.addField(argTitle, argDesc).setColor(Color.YELLOW);
				} else {
					embed.addField(command[0], command[1] + "\n" + prefix + command[0] + " " + command[2].substring(1) + " " + command[3].substring(1));
					String argTitle = "Required parameter";
					if(command[2].startsWith("o")) {
						argTitle = "Optional parameter";
					}
					String argDesc = "**" + command[2].substring(2, command[2].length() - 1) + "**```Diff\n" + describeParameter(command[2]) + "```";
					embed.addField(argTitle, argDesc);
					argTitle = "Required parameter";
					if(command[3].startsWith("p"))
						argTitle = "Optional parameter, previous required";
					argDesc = "**" + command[3].substring(2, command[3].length() - 1) + "**```Diff\n" + describeParameter(command[3]) + "```";
					return embed.addField(argTitle, argDesc).setColor(Color.YELLOW);
				}
			}
		}
		return null;
	}
	
	private String describeParameter(String parameter) {
		
		System.out.printf("describeParameter(%s)%n", parameter);
		
		String result = "";
		if(parameter.contains("Command"))
			result += "-Command\nA command name without its prefix.\nEx: help\n";
		if(parameter.contains("Bonus"))
			result += "-Bonus\nA number without commas to be added.\nEx: 10000\n";
		if(parameter.contains("Level"))
			result += "-Level\nA number below 10000 without commas.\nEx: 300\n";
		if(parameter.contains("Rank"))
			result += "-Rank\nA number between 1 and 100.\nEx: 23\n";
		if(parameter.contains("Team"))
			result += "-Team\nA team tag, combination of 2 to 4 letters and numbers.\nEx: LOLY\n";
		if(parameter.contains("Board"))
			result += "-Board\nA leaderboard type. Possible boards are points, speed, hof.\n";
		if(parameter.contains("Time"))
			result += "-Time\nA designation of time for a leaderboard. Possible times are daily, weekly, monthly, season. Additionally, a season number can be appended after season to lookup a particular season's leaderboard. However, the numbers do not align exactly with actual season numbers.\n";
		if(parameter.contains("Mines"))
			result += "-Mines\nA number of mines to be hidden in the grid.\n";
		if(parameter.contains("Difficulty"))
			result += "-Difficulty\nThe difficulty setting for the game. Possible difficulties are easy, normal, hard, impossible.\n";
		if(parameter.contains("Prefix"))
			result += "-Prefix\nA short character sequence that signals the bot to perform a command.\nEx: L.\n";
		if(parameter.contains("Username") || parameter.contains("Racer") || parameter.contains("Level"))
			result += "-Username\nA unique string of letters, numbers and some additional characters for a Nitro Type account.\nEx: nes370\n";
		if(parameter.contains("URL") || parameter.contains("Racer") || parameter.contains("Level"))
			result += "-URL\nA link to a user's racer page.\nEx: https://www.nitrotype.com/racer/nes370\n";
		if(parameter.contains("User ID") || parameter.contains("Racer") || parameter.contains("Level"))
			result += "-User ID\nA unique number assigned to each Nitro Type user, preceded by #.\nEx: #1139474\n";
		if(parameter.contains("Discord ID") || parameter.contains("Racer") || parameter.contains("Level"))
			result += "-Discord ID\nA unique 18-digit number assigned to each Discord user. The user must be registered with this bot for Nitro Type Commands.\nEx: 237676240931258378\n";
		if(parameter.contains("Mention") || parameter.contains("Racer") || parameter.contains("Level"))
			result += "-Mention\nA Discord mention is visually represented by a blue highlighted text in the format @Name#1234, and is internally formatted as <@Discord ID>.\nEx: <@237676240931258378>\n";
		if(parameter.contains("Channel"))
			result += "-Channel\nA Discord channel is visually represented by a blue highlighted text in the format #channel-name, and is internally formatted as <#Channel ID>.\nEx: <#564881373039689735>\n";
		if(parameter.contains("Message"))
			result += "-Message\nAny string of characters.\n";
		if(parameter.contains("Stat"))
			result += "-Stat\nA statistic used to order a leaderboard.\nAccepted values (omitted):\n+races (played)\n+first (place)\n+second (place)\n+third (place)\n+accuracy\n+(money) spent\n+high (speed)\n+(avg) speed\n+(oldest) active (account)\n+(oldest) age (account)\n+(most) nonbonus (experience)\n+(most) bonus (experience)\n+(total) experience\n+level\n+totalcars\n+sold (cars)\n+(owned) cars\n+(play) time\n+(profile) view (count)\n+achievement (points)\n+used (nitros)\n+(longest) session\n+rank\n+density\n+value";
		return result;
	}
	
	private EmbedBuilder leaveServer(String serverID) {
		
		System.out.printf("leaveServer(%s)%n", serverID);
		
		EmbedBuilder embed = new EmbedBuilder();
		try {
			Main.getApi().getServerById(serverID).get().leave().get();
		} catch (Exception e) {
			return embed.setDescription("Did not leave server " + serverID).setColor(Color.RED);
		}
		return embed.setDescription("Left server " + serverID).setColor(Color.GREEN);
	}
	
	private Collection<? extends EmbedBuilder> serverList() {
		
		System.out.println("serverList()");
		
		List<EmbedBuilder> embedList = new ArrayList<EmbedBuilder>();
		Collection<Server> list = Main.getApi().getServers();
		EmbedBuilder message = new EmbedBuilder();
		embedList.add(message);
		//int userCount = 0;
		list.stream().forEach(s -> {
				if(s.getIcon().isPresent()) {
					embedList.add(new EmbedBuilder().setAuthor(s.getName(), "https://discordapp.com/channels/" +  s.getIdAsString(), "")
							.addInlineField("Owner", s.getOwner().get().getDiscriminatedName() + "\n" + s.getOwner().get().getMentionTag())
							.addInlineField("Members", "" + s.getMemberCount())
							.addInlineField("Joined", Main.dtf.format(LocalDateTime.ofInstant(s.getJoinedAtTimestamp(Main.getApi().getYourself()).get(), ZoneId.systemDefault())))
							.addInlineField("Server ID", s.getIdAsString())
							.setThumbnail(s.getIcon().get())
							.setFooter("Server created").setTimestamp(s.getCreationTimestamp()));
				} else {
					embedList.add(new EmbedBuilder().setAuthor(s.getName(), "https://discordapp.com/channels/" +  s.getIdAsString(), "")
							.addInlineField("Owner", s.getOwner().get().getDiscriminatedName() + "\n" + s.getOwner().get().getMentionTag())
							.addInlineField("Members", "" + s.getMemberCount())
							.addInlineField("Joined", Main.dtf.format(LocalDateTime.ofInstant(s.getJoinedAtTimestamp(Main.getApi().getYourself()).get(), ZoneId.systemDefault())))
							.addInlineField("Server ID", s.getIdAsString())
							.setFooter("Server created").setTimestamp(s.getCreationTimestamp()));
				}
				//userCount += s.getMemberCount();
		});
		message.setDescription("Little Lamborgotti is in " + list.size() + " servers.");
		return embedList;
	}
	
	/**
	 * 
	 * @param discordID
	 * @param argument
	 * @return
	 * @throws StringIndexOutOfBoundsException
	 * @throws IOException
	 * @throws ParseException
	 */
	private EmbedBuilder experience(long discordID, String argument) throws StringIndexOutOfBoundsException, IOException, ParseException {
		
		System.out.printf("experience(%d, %s)%n", discordID, argument);
		
		JSONObject racerInfo = getRacerInfo("" + discordID);
		long experience = (long) racerInfo.get("experience");
		long level = (long) racerInfo.get("level"),
				placed1 = (long) racerInfo.get("placed1"),
				placed2 = (long) racerInfo.get("placed2"),
				placed3 = (long) racerInfo.get("placed3");
		long racesPlayed = (long) racerInfo.get("racesPlayed");
		
		String description = "You are at level **" + numberToText(level) + "** and you have **" + numberToText(experience) + "** experience.";
		
		long targetLevel = level + 1;
		boolean secondUser = false;
		if(argument != null) { //user provided an argument
			try { //try to parse it as a number
				targetLevel = Long.parseLong(argument);
			} catch(NumberFormatException nfe) {
				secondUser = true;
			}
			if(targetLevel > 10000 || secondUser) {// if the level is too big, then set it to max value of 10000
				JSONObject racerInfo2 = getRacerInfo("" + argument);
				String displayName2 = (String) racerInfo2.get("displayName");
				long experience2 = (long) racerInfo2.get("experience");
				targetLevel = (long) racerInfo2.get("level");
				description += "\n**" + displayName2 + "** is at level **" + numberToText(targetLevel) + "** and has **" + numberToText(experience2) + "** experience.";
			}
		}

		if(level < targetLevel) 
			description += "\nYou need **" + numberToText(exptolvl(targetLevel) - experience) + "** more experience, or about **"
			+ numberToText((exptolvl(targetLevel) - experience) * racesPlayed / (racesPlayed * 1000 + placed1 * 100 + placed2 * 50 + placed3 * 30)) 
			+ "** more races to reach level **" + numberToText(targetLevel) + "**.";
		else description += "\nYou have **" + numberToText(experience - exptolvl(targetLevel)) + "** more experience than needed for level **" + numberToText(targetLevel) + "**.";
		
		return new EmbedBuilder().setDescription(description).setColor(Color.GREEN);
	}
	
	/**
	 * 
	 * @param one
	 * @param two
	 * @return
	 * @throws NumberFormatException
	 */
	private EmbedBuilder experience(String one, String two) throws NumberFormatException {
		
		System.out.printf("experience(%s, %s)%n", one, two);
		
		long a = Long.parseLong(one);
		long b = Long.parseLong(two);
		
		a %= 10000;
		b %= 10000;
		
		if(a > b) {
			long c = a;
			a = b;
			b = c;
		}
		
		if(a == b)
			return new EmbedBuilder().setDescription("Level " + numberToText(a) + " has " + numberToText(exptolvl(a)) + " experience.");
		
		return new EmbedBuilder().setDescription("Level " + numberToText(a) + " has " + numberToText(exptolvl(a)) + " experience.\n" 
				+ "Level " + numberToText(b) + " requires " + numberToText(exptolvl(b)) + " experience.\n"
				+ numberToText(exptolvl(b) - exptolvl(a)) + " is needed to move from level " + numberToText(a) + " to " + numberToText(b) + ".");
		
	}
	
	/**
	 * 
	 * @param user
	 * @return
	 * @throws StringIndexOutOfBoundsException
	 * @throws IOException
	 * @throws ParseException
	 */
	private EmbedBuilder raceLogs(String user) throws StringIndexOutOfBoundsException, IOException, ParseException {
		
		System.out.printf("raceLogs(%s)%n", user);
		
		JSONObject racerInfo = getRacerInfo(user);
		
		JSONArray raceLogs = (JSONArray) racerInfo.get("raceLogs");
		
		if(raceLogs == null) {
			racerInfo = getRacerInfo("#" + (long) racerInfo.get("userID"));
			raceLogs = (JSONArray) racerInfo.get("raceLogs");
		}
		
		byte gold = 0;
		if(((String) racerInfo.get("membership")).equals("gold"))
			gold++;
		
		String title = "", tag = "", displayName = (String) racerInfo.get("username");
		if(racerInfo.get("title") != null)
			title = (String) racerInfo.get("title");
		if(racerInfo.get("tag") != null)
			tag = "[" + (String) racerInfo.get("tag") + "]";
		if(racerInfo.get("displayName") != null && !((String) racerInfo.get("displayName")).equals(""))
			displayName = (String) racerInfo.get("displayName");
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		
		EmbedBuilder embed = new EmbedBuilder().setAuthor(tag + " " + displayName + "\n" + title, "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
		
		if(gold != 0)
			embed.setColor(new Color(0xFFD700));
		else embed.setColor(Color.PINK);
			//embed.addField("Units", "Time, Interval (seconds), Speed (wpm), Accuracy (%)");
		String data = "```\n";
		long time = 0, lastTime;
		//int ten = 0;
		for(int i = 0; i < raceLogs.size(); i++) { // TODO might be broken
			JSONObject raceLog = (JSONObject) raceLogs.get(i);
			long typed = (long) raceLog.get("typed");
			long errs = (long) raceLog.get("errs");
			double acc = (int) ((typed - errs) * 10000.0 / typed) / 100.0;
			long wpm = (long) raceLog.get("value");
			if(i == 0) {
				time = (long) raceLog.get("stamp");
				data += dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time * 1000), ZoneId.systemDefault())) + "	       ";
				
			} else if(i % 10 == 0) { 
				data += "```";
				embed.addField(/*(ten + 1) + "-" + (ten + 10)*/ "Time                                                  Interval   Speed   Accuracy", data);
				//ten += 10;
				data = "```\n";
				lastTime = time;
				time = (long) raceLog.get("stamp");
				data += dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time * 1000), ZoneId.systemDefault())) + "	";
				data += (time - lastTime);
				for(int j = ("" + (time - lastTime)).length(); j < 7; j++) {
					data += " ";
				}
			}  else {
				lastTime = time;
				time = (long) raceLog.get("stamp");
				data += dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time * 1000), ZoneId.systemDefault())) + "	";
				data += (time - lastTime);
				for(int j = ("" + (time - lastTime)).length(); j < 7; j++) {
					data += " ";
				}
			}
			data += wpm;
			for(int j = ("" + wpm).length(); j < 7; j++) {
				data += " ";
			}
			data += acc + "\n";
			
		}
		
		data += "```";
		
		embed.addField(/*(ten + 1) + "-" + raceLogs.size()*/ "Time                                                  Interval   Speed   Accuracy", data);
		
		if(racerInfo.containsKey("tagColor")) {
			String color = (String) racerInfo.get("tagColor");
			if(color != null && !color.equals("") && !color.equals("null"))
				embed.setColor(Color.decode("#" + color));
		}
		
		return embed;
	}
	
	/**
	 * 
	 * @param messageAuthor
	 * @param tag
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private EmbedBuilder team(MessageAuthor messageAuthor, String tag) throws IOException, ParseException {
		
		System.out.printf("team(%s, %s)%n", messageAuthor.getName(), tag);
		
		if(tag == null) {
			JSONObject racerInfo = getRacerInfo(messageAuthor.getIdAsString());
			tag = (String) racerInfo.get("tag");
		}
		
		JSONObject teamJSON = getTeamInfo(tag);
		JSONObject info = (JSONObject) teamJSON.get("info");
		JSONArray members = (JSONArray) teamJSON.get("members");
		
		String banned = "";
		int banCount = 0;
		String banInfo = "";
		
		String officers = "";
		
		for(int i = 0; i < members.size(); i++) {
			
			JSONObject member = (JSONObject) members.get(i);
			String name = (String) member.get("displayName");
			if(name == null || name.equals("null") || name.equals(""))
				name = (String) member.get("username");
			
			if(((String) member.get("role")).equals("officer")) {
				if((long) info.get("userID") == (long) member.get("userID"))
					officers = "Captain **" + name + "** [\\🔗](https://www.nitrotype.com/racer/" + (String) member.get("username") + ")\n" + officers;
				else officers += "Officer **" + name + "** [\\🔗](https://www.nitrotype.com/racer/" + (String) member.get("username") + ")\n";
			}
			
			if(((String) member.get("status")).equals("banned")) {
				banned += "**" + name + "**" + " [\\🔗](https://test.nitrotype.com/api/players/" + (long) member.get("userID") + ")\n";
				banCount++;
			}
			
		}
		
		if(banCount > 0)
			banInfo += "\n**" + banCount + "** banned members";
		
		String enrollment = (String) info.get("enrollment");
		enrollment = enrollment.substring(0, 1).toUpperCase() + enrollment.substring(1);
		
		EmbedBuilder embed = new EmbedBuilder()
				.setAuthor("[" + (String) info.get("tag") + "] " + (String) info.get("name"), "https://www.nitrotype.com/team/" + tag, "")
				.setDescription((String) info.get("otherRequirements"))
				.addInlineField("Info", "**" + (long) info.get("members") + "** members" 
						+ banInfo
						+ "\nCreated at **" + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli((long) info.get("createdStamp") * 1000), ZoneId.systemDefault()))
						+ "**\n**" + numberToText((long) info.get("profileViews")) + "** team views")
				.addInlineField("Enrollment", "**" + enrollment + "**\nMinimum races **" + (long) info.get("minRaces") + "**\nMinimum speed **" + (long) info.get("minSpeed") + " wpm**")
				.addInlineField("Leaders", officers)
				.setColor(Color.decode("#" + (String) info.get("tagColor")));
		
		if(banCount > 0)
			embed.addInlineField("Banned", banned);
		
		JSONArray racingStats = (JSONArray) teamJSON.get("stats");
		if(!racingStats.isEmpty()) {
			String description = "";
			for(int i = 0; i < racingStats.size(); i++) {
				JSONObject stat = (JSONObject) racingStats.get(i);
				String board = (String) stat.get("board");
				if(board.equals("alltime"))
					board = "all-time";
				long // rank = (long) stat.get("rank"), // DEPRECATED
						played = (long) stat.get("played"),
						typed = (long) stat.get("typed"),
						errs = (long) stat.get("errs");
				double secs = Double.parseDouble("" + (Long) stat.get("secs"));
				description += "**" + board.substring(0, 1).toUpperCase() + board.substring(1) + "** "
						// + "Rank " + numberToText(rank) // DEPRECATED
						+ "\n" + numberToText((int) (played * (100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed + 0.5)) 
						+" points (" + ((long) ((100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed * 1000)) / 1000.0 + " pts / race)"
						+ "\n" + numberToText(played) + " races (" + fractionToText(typed - errs, typed) + " acc, " + ((long) (typed / secs / 5 * 60 * 1000)) / 1000.0 + " wpm)\n";
			}
			embed.addInlineField("Leaderboards", description);
		}
		
		return embed;
	}
	
	/**
	 * Determines if a user is reigstered and verified. If they are, then the bot will apply server-specific actions, such as nickname alteration and role assignment.
	 * 
	 * @param user
	 * @param server
	 * @return Success or Failure embed
	 * @throws StringIndexOutOfBoundsException
	 * @throws IOException
	 * @throws ParseException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private EmbedBuilder update(User user, Server server) throws StringIndexOutOfBoundsException, IOException, ParseException, InterruptedException, ExecutionException {
		
		System.out.printf("update(%s, %s)%n", user.getName(), server.getName());
		
		//Find related username
		String username = "";
		boolean verified = false;
		boolean match = false;
		
		JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
		
		for(int i = 0; i < register.size(); i++) {
			
			JSONObject entry = (JSONObject) register.get(i);
			
			if(((Long) entry.get("discordId")).longValue() == Long.parseLong(user.getIdAsString())) {
				username = (String) entry.get("username");
				verified = (boolean) entry.get("verified");
				match = true;
			}
			
		}
		
		//Check if verified
		if(!match) {
			//tell user to to use l.register
			return new EmbedBuilder().setDescription("<@" + user.getIdAsString() + "> is not registered to a NT account.").setColor(Color.YELLOW);
		} else if(!verified) {
			//tell user to use l.verify
			return new EmbedBuilder().setDescription("<@" + user.getIdAsString() + "> has not been verified yet.").setColor(Color.YELLOW);
		} else {
		
			//Fetch racer data
			JSONObject racerJSON = getRacerInfo(username);
		
			//Apply applicable roles for server
			if(server.getId() == NITRO_TYPE_SERVER) {
				
				List<Role> roles = user.getRoles(server);
				
				user.addRole(server.getRolesByName("Registered").get(0)).get();
				user.addRole(server.getRoleById(654804415747850241L).get());
				user.addRole(server.getRoleById(654801298297847838L).get());
				user.addRole(server.getRoleById(654802074034503681L).get());
				
				roleSwitch(((String) racerJSON.get("membership")).equals("gold"), server.getRolesByName("Gold").get(0), roles, user);
				
				roleSwitch((long) racerJSON.get("money") >= 100_000_000L, server.getRolesByName("Wealthy").get(0), roles, user);
				
				roleSwitch((long) racerJSON.get("longestSession") >= 800, server.getRolesByName("Sessionist").get(0), roles, user);
				
				{
					boolean v1veteran, v2veteran;
				
					v1veteran = (long) racerJSON.get("createdStamp") < 1430179200L; //1430179200000 v2 released
				
					if(!v1veteran)
						v2veteran = (long) racerJSON.get("createdStamp") < 1559692800L; //1559692800000 v3 released
					else v2veteran = false;
				
					Role v1veteranRole = server.getRolesByName("v1 Veteran").get(0);
					Role v2veteranRole = server.getRolesByName("v2 Veteran").get(0);
				
					roleSwitch(v1veteran, v1veteranRole, roles, user);
					roleSwitch(v2veteran, v2veteranRole, roles, user);
				}
				
				{
					String tag = (String) racerJSON.get("tag");
				
					roleSwitch(tag != null && tag.equalsIgnoreCase("DV0RAK"), server.getRolesByName("DV0RAK").get(0), roles, user);
					roleSwitch(tag != null && tag.equalsIgnoreCase("N8TE"), server.getRolesByName("N8TE").get(0), roles, user);	
					roleSwitch(tag != null && tag.equalsIgnoreCase("SSH"), server.getRolesByName("SSH").get(0), roles, user);
					
					String name = "";
					if(tag != null && !tag.equals("") && !tag.equals("null"))
						name += "[" + tag + "] ";
					String displayName = (String) racerJSON.get("displayName");
					if(displayName != null && !displayName.equals("") && !displayName.equals("null")) {
						name += displayName;
					} else name += (String) racerJSON.get("username");
					
					user.updateNickname(server, name);
				}
				
				{
					long avgSpeed = (long) racerJSON.get("avgSpeed");
					
					Role[] speed = {
							server.getRolesByName("1-10 wpm").get(0),
							server.getRolesByName("11-20 wpm").get(0),
							server.getRolesByName("21-30 wpm").get(0),
							server.getRolesByName("31-40 wpm").get(0),
							server.getRolesByName("41-50 wpm").get(0),
							server.getRolesByName("51-60 wpm").get(0),
							server.getRolesByName("61-70 wpm").get(0),
							server.getRolesByName("71-80 wpm").get(0),
							server.getRolesByName("81-90 wpm").get(0),
							server.getRolesByName("91-100 wpm").get(0),
							server.getRolesByName("101-110 wpm").get(0),
							server.getRolesByName("111-120 wpm").get(0),
							server.getRolesByName("121-130 wpm").get(0),
							server.getRolesByName("131-140 wpm").get(0),
							server.getRolesByName("141-150 wpm").get(0),
							server.getRolesByName("151-160 wpm").get(0),
							server.getRolesByName("161-170 wpm").get(0),
							server.getRolesByName("171-180 wpm").get(0),
							server.getRolesByName("181-190 wpm").get(0),
							server.getRolesByName("191-199 wpm").get(0),
							server.getRolesByName("200+ wpm").get(0)
					};
					
					for(int i = 0; i < speed.length; i++) {
						if(i * 10 + 1 <= avgSpeed && i * 10 + 10 >= avgSpeed) {
							if(!roles.contains(speed[i]))
								user.addRole(speed[i]);
						} else if(avgSpeed > 199) {
							if(i != 20) {
								if(roles.contains(speed[i]))
									user.removeRole(speed[i]);
							} else if(!roles.contains(speed[20])) {
								user.addRole(speed[20]);
							}
						} else {
							if(roles.contains(speed[i]))
								user.removeRole(speed[i]);
						}
					}
				}
				
				{
					long racesPlayed = (long) racerJSON.get("racesPlayed");
					
					Role[] races = {
							server.getRolesByName("\"I <3 Typing!\"").get(0),
							server.getRolesByName("\"I Really Love Typing\"").get(0),
							server.getRolesByName("\"Bonkers About Typing\"").get(0),
							server.getRolesByName("\"Bananas About Typing\"").get(0),
							server.getRolesByName("\"You've Gotta Be Kidding\"").get(0),
							server.getRolesByName("\"Corsair\"").get(0),
							server.getRolesByName("\"Pirc\"").get(0),
							server.getRolesByName("\"Carrie\"").get(0),
							server.getRolesByName("\"Anne\"").get(0),
							server.getRolesByName("\"Lackin' Nothin'\"").get(0),
							server.getRolesByName("\"Outback Officer\"").get(0),
							server.getRolesByName("\"I Love Shoes 2\"").get(0),
							server.getRolesByName("\"I Love Shoes 12.5\"").get(0),
							server.getRolesByName("\"I Love Shoes 15.0\"").get(0),
							server.getRolesByName("\"I Love Shoes 20.0\"").get(0),
							server.getRolesByName("\"The Wildest of Flowers\"").get(0),
							server.getRolesByName("\"The Wild Legend\"").get(0)
					};
					
					roleSwitch(racesPlayed < 100, races[0], roles, user);
					roleSwitch(racesPlayed >= 100 && racesPlayed < 500, races[1], roles, user);
					roleSwitch(racesPlayed >= 500 && racesPlayed < 1000, races[2], roles, user);
					roleSwitch(racesPlayed >= 1000 && racesPlayed < 5000, races[3], roles, user);
					roleSwitch(racesPlayed >= 5000 && racesPlayed < 10_000, races[4], roles, user);
					roleSwitch(racesPlayed >= 10_000 && racesPlayed < 20_000, races[5], roles, user);
					roleSwitch(racesPlayed >= 20_000 && racesPlayed < 30_000, races[6], roles, user);
					roleSwitch(racesPlayed >= 30_000 && racesPlayed < 40_000, races[7], roles, user);
					roleSwitch(racesPlayed >= 40_000 && racesPlayed < 50_000, races[8], roles, user);
					roleSwitch(racesPlayed >= 50_000 && racesPlayed < 75_000, races[9], roles, user);
					roleSwitch(racesPlayed >= 75_000 && racesPlayed < 100_000, races[10], roles, user);
					roleSwitch(racesPlayed >= 100_000 && racesPlayed < 125_000, races[11], roles, user);
					roleSwitch(racesPlayed >= 125_000 && racesPlayed < 150_000, races[12], roles, user);
					roleSwitch(racesPlayed >= 150_000 && racesPlayed < 200_000, races[13], roles, user);
					roleSwitch(racesPlayed >= 200_000 && racesPlayed < 250_000, races[14], roles, user);
					roleSwitch(racesPlayed >= 250_000 && racesPlayed < 500_000, races[15], roles, user);
					roleSwitch(racesPlayed >= 500_000, races[16], roles, user);
				}
				
				JSONArray raceLogs = (JSONArray) racerJSON.get("raceLogs");
				
				if(raceLogs == null) {
					racerJSON = getRacerInfo("#" + (long) racerJSON.get("userID"));
					raceLogs = (JSONArray) racerJSON.get("raceLogs");
				}
				
				long typed = 0, errs = 0;
				for(int i = 0; i < raceLogs.size(); i++) {
					JSONObject race = (JSONObject) raceLogs.get(i);
					typed += (long) race.get("typed");
					errs += (long) race.get("errs");
				}
				
				double acc = (typed - errs) * 100.0 / typed;
				
				Role[] accuracy = {
						server.getRolesByName("<75% Accuracy").get(0),
						server.getRolesByName("76-80% Accuracy").get(0),
						server.getRolesByName("81-85% Accuracy").get(0),
						server.getRolesByName("86-90% Accuracy").get(0),
						server.getRolesByName("91-92% Accuracy").get(0),
					 	server.getRolesByName("93-94% Accuracy").get(0),
					 	server.getRolesByName("95-96% Accuracy").get(0),
					 	server.getRolesByName("97% Accuracy").get(0),
					 	server.getRolesByName("98% Accuracy").get(0),
					 	server.getRolesByName(">99% Accuracy").get(0)
				};
				
				roleSwitch(acc < 75.5, accuracy[0], roles, user);
				roleSwitch(acc >= 75.5 && acc < 80.5, accuracy[1], roles, user);
				roleSwitch(acc >= 80.5 && acc < 85.5, accuracy[2], roles, user);
				roleSwitch(acc >= 85.5 && acc < 90.5, accuracy[3], roles, user);
				roleSwitch(acc >= 90.5 && acc < 92.5, accuracy[4], roles, user);
				roleSwitch(acc >= 92.5 && acc < 94.5, accuracy[5], roles, user);
				roleSwitch(acc >= 94.5 && acc < 96.5, accuracy[6], roles, user);
				roleSwitch(acc >= 96.5 && acc < 97.5, accuracy[7], roles, user);
				roleSwitch(acc >= 97.5 && acc < 98.5, accuracy[8], roles, user);
				roleSwitch(acc >= 98.5, accuracy[9], roles, user);
				
			} else if(server.getId() == _1X1_SERVER) { //1X1 server
				String tag = (String) racerJSON.get("tag");
				boolean oneX1 = false;
				if(tag != null && tag.equalsIgnoreCase("1X1")) {
					oneX1 = true;
				}
					
				List<Role> roles = user.getRoles(server);
				
				Role oneX1Role = server.getRolesByName("1x1 member").get(0);
				if(oneX1) {
					if(!roles.contains(oneX1Role))
						user.addRole(oneX1Role);
				} else if(roles.contains(oneX1Role))
					user.removeRole(oneX1Role);
			} else if(server.getId() == ASCENSION_SERVER) { //ASCENSION server
				boolean member = false;
				if(((String) racerJSON.get("tag")).equalsIgnoreCase("ASCEND"))
					member = true;
				Role memberRole = server.getRolesByName("Member").get(0),
						guestRole = server.getRolesByName("Guest").get(0);
				List<Role> roles = user.getRoles(server);
				if(member) {
					if(!roles.contains(memberRole))
						user.addRole(memberRole);
					if(roles.contains(guestRole))
						user.removeRole(guestRole);
				} else {
					if(!roles.contains(guestRole))
						user.addRole(guestRole);
					if(roles.contains(memberRole))
						user.removeRole(memberRole);
				}
			} else if(server.getId() == BOK_SERVER) { //BOK server
				List<Role> roles = user.getRoles(server);
				Role registered = server.getRolesByName("Registered").get(0);
				if(!roles.contains(registered))
					user.addRole(registered);
				
				boolean isMember = false;
				
				String name = "";
				String tag = (String) racerJSON.get("tag");
				if(tag != null && !tag.equals("") && !tag.equals("null")) {
					name += "[" + tag + "] ";
					if(tag.equals("BOK"))
						isMember = true;
				}
				
				Role member = server.getRolesByName("BOK member").get(0),
						visitor = server.getRolesByName("Visitor").get(0);
				if(isMember) {
					if(!roles.contains(member))
						user.addRole(member);
					if(roles.contains(visitor))
						user.removeRole(visitor);
				} else {
					if(!roles.contains(visitor))
						user.addRole(visitor);
					if(roles.contains(member))
						user.removeRole(member);
				}
				
				String displayName = (String) racerJSON.get("displayName");
				if(displayName != null && !displayName.equals("") && !displayName.equals("null")) {
					name += displayName;
				} else name += (String) racerJSON.get("username");
	//			if(!user.getNickname(server).equals(name))
					user.updateNickname(server, name);
			} else if(server.getId() == TBZ_SERVER) { //TBZ server
				List<Role> roles = user.getRoles(server);
				
				Role TBZRole = server.getRolesByName("Team member").get(0);
				
				String tag = (String) racerJSON.get("tag");
				boolean tbz = false;
				if(tag != null && tag.equalsIgnoreCase("XBS")) //changed from TBZ
					tbz = true;
				
				if(tbz) {
					if(!roles.contains(TBZRole))
						user.addRole(TBZRole);
				} else if(roles.contains(TBZRole))
					user.removeRole(TBZRole);
				
				JSONArray raceLogs = (JSONArray) racerJSON.get("raceLogs");
				
				if(raceLogs == null) {
					racerJSON = getRacerInfo("#" + (long) racerJSON.get("userID"));
					raceLogs = (JSONArray) racerJSON.get("raceLogs");
				}
				
				long typed = 0, errs = 0;
				for(int i = 0; i < raceLogs.size(); i++) {
					JSONObject race = (JSONObject) raceLogs.get(i);
					typed += (long) race.get("typed");
					errs += (long) race.get("errs");
				}
				double acc = (typed - errs) * 100.0 / typed;
				
				Role[] accuracy = new Role[6];
	
				accuracy[0] = server.getRolesByName("ACC70-75%").get(0);
				accuracy[1] = server.getRolesByName("ACC76-80%").get(0);
				accuracy[2] = server.getRolesByName("ACC81-85%").get(0);
				accuracy[3] = server.getRolesByName("ACC86-90%").get(0);
				accuracy[4] = server.getRolesByName("ACC91-95%").get(0);
				accuracy[5] = server.getRolesByName("ACC96-100%").get(0);
				
				if(acc < 75.5) {
					if(!roles.contains(accuracy[0]))
						user.addRole(accuracy[0]);
				} else {
					if(roles.contains(accuracy[0]))
						user.removeRole(accuracy[0]);
				}
				if(acc >= 75.5 && acc < 80.5) {
					if(!roles.contains(accuracy[1]))
						user.addRole(accuracy[1]);
				} else {
					if(roles.contains(accuracy[1]))
						user.removeRole(accuracy[1]);
				}
				if(acc >= 80.5 && acc < 85.5) {
					if(!roles.contains(accuracy[2]))
						user.addRole(accuracy[2]);
				} else {
					if(roles.contains(accuracy[2]))
						user.removeRole(accuracy[2]);
				}
				if(acc >= 85.5 && acc < 90.5) {
					if(!roles.contains(accuracy[3]))
						user.addRole(accuracy[3]);
				} else {
					if(roles.contains(accuracy[3]))
						user.removeRole(accuracy[3]);
				}
				if(acc >= 90.5 && acc < 95.5) {
					if(!roles.contains(accuracy[4]))
						user.addRole(accuracy[4]);
				} else {
					if(roles.contains(accuracy[4]))
						user.removeRole(accuracy[4]);
				}
				if(acc >= 95.5) {
					if(!roles.contains(accuracy[5]))
						user.addRole(accuracy[5]);
				} else {
					if(roles.contains(accuracy[5]))
						user.removeRole(accuracy[5]);
				}
				
				long avgSpeed = (long) racerJSON.get("avgSpeed");
				
				Role[] speed = new Role[7];
	
				speed[0] = server.getRolesByName("0-25WPM").get(0);
				speed[1] = server.getRolesByName("26-50WPM").get(0);
				speed[2] = server.getRolesByName("51-75WPM").get(0);
				speed[3] = server.getRolesByName("76-100WPM").get(0);
				speed[4] = server.getRolesByName("101-125WPM").get(0);
				speed[5] = server.getRolesByName("126-150WPM").get(0);
				speed[6] = server.getRolesByName("151WPM+").get(0);
				
				if(avgSpeed < 26) {
					if(!roles.contains(speed[0]))
						user.addRole(speed[0]);
				} else {
					if(roles.contains(speed[0]))
						user.removeRole(speed[0]);
				}
				if(avgSpeed >= 26 && avgSpeed < 51) {
					if(!roles.contains(speed[1]))
						user.addRole(speed[1]);
				} else {
					if(roles.contains(speed[1]))
						user.removeRole(speed[1]);
				}
				if(avgSpeed >= 51 && avgSpeed < 76) {
					if(!roles.contains(speed[2]))
						user.addRole(speed[2]);
				} else {
					if(roles.contains(speed[2]))
						user.removeRole(speed[2]);
				}
				if(avgSpeed >= 76 && avgSpeed < 101) {
					if(!roles.contains(speed[3]))
						user.addRole(speed[3]);
				} else {
					if(roles.contains(speed[3]))
						user.removeRole(speed[3]);
				}
				if(avgSpeed >= 101 && avgSpeed < 126) {
					if(!roles.contains(speed[4]))
						user.addRole(speed[4]);
				} else {
					if(roles.contains(speed[4]))
						user.removeRole(speed[4]);
				}
				if(avgSpeed >= 126 && avgSpeed < 151) {
					if(!roles.contains(speed[5]))
						user.addRole(speed[5]);
				} else {
					if(roles.contains(speed[5]))
						user.removeRole(speed[5]);
				}
				if(avgSpeed >= 151) {
					if(!roles.contains(speed[6]))
						user.addRole(speed[6]);
				} else {
					if(roles.contains(speed[6]))
						user.removeRole(speed[6]);
				}
				
				long level = (long) racerJSON.get("level");
				
				Role[] levels = new Role[14];
				
				levels[0] = server.getRolesByName("Level0-25").get(0);
				levels[1] = server.getRolesByName("Level26-50").get(0);
				levels[2] = server.getRolesByName("Level51-75").get(0);
				levels[3] = server.getRolesByName("Level76-100").get(0);
				levels[4] = server.getRolesByName("Level101-200").get(0);
				levels[5] = server.getRolesByName("Level201-300").get(0);
				levels[6] = server.getRolesByName("Level301-400").get(0);
				levels[7] = server.getRolesByName("Level401-500").get(0);
				levels[8] = server.getRolesByName("Level501-600").get(0);
				levels[9] = server.getRolesByName("Level601-700").get(0);
				levels[10] = server.getRolesByName("Level701-800").get(0);
				levels[11] = server.getRolesByName("Level801-950").get(0);
				levels[12] = server.getRolesByName("Level951-1100").get(0);
				levels[13] = server.getRolesByName("Level1100+").get(0);
				
				if(level < 26) {
					if(!roles.contains(levels[0]))
						user.addRole(levels[0]);
				} else {
					if(roles.contains(levels[0]))
						user.removeRole(levels[0]);
				}
				if(level >= 26 && level < 51) {
					if(!roles.contains(levels[1]))
						user.addRole(levels[1]);
				} else {
					if(roles.contains(levels[1]))
						user.removeRole(levels[1]);
				}
				if(level >= 51 && level < 76) {
					if(!roles.contains(levels[2]))
						user.addRole(levels[2]);
				} else {
					if(roles.contains(levels[2]))
						user.removeRole(levels[2]);
				}
				if(level >= 76 && level < 101) {
					if(!roles.contains(levels[3]))
						user.addRole(levels[3]);
				} else {
					if(roles.contains(levels[3]))
						user.removeRole(levels[3]);
				}
				if(level >= 101 && level < 201) {
					if(!roles.contains(levels[4]))
						user.addRole(levels[4]);
				} else {
					if(roles.contains(levels[4]))
						user.removeRole(levels[4]);
				}
				if(level >= 201 && level < 301) {
					if(!roles.contains(levels[5]))
						user.addRole(levels[5]);
				} else {
					if(roles.contains(levels[5]))
						user.removeRole(levels[5]);
				}
				if(level >= 301 && level < 401) {
					if(!roles.contains(levels[6]))
						user.addRole(levels[6]);
				} else {
					if(roles.contains(levels[6]))
						user.removeRole(levels[6]);
				}
				if(level >= 401 && level < 501) {
					if(!roles.contains(levels[7]))
						user.addRole(levels[7]);
				} else {
					if(roles.contains(levels[7]))
						user.removeRole(levels[7]);
				}
				if(level >= 501 && level < 601) {
					if(!roles.contains(levels[8]))
						user.addRole(levels[8]);
				} else {
					if(roles.contains(levels[8]))
						user.removeRole(levels[8]);
				}
				if(level >= 601 && level < 701) {
					if(!roles.contains(levels[9]))
						user.addRole(levels[9]);
				} else {
					if(roles.contains(levels[9]))
						user.removeRole(levels[9]);
				}
				if(level >= 701 && level < 801) {
					if(!roles.contains(levels[10]))
						user.addRole(levels[10]);
				} else {
					if(roles.contains(levels[10]))
						user.removeRole(levels[10]);
				}
				if(level >= 801 && level < 951) {
					if(!roles.contains(levels[11]))
						user.addRole(levels[11]);
				} else {
					if(roles.contains(levels[11]))
						user.removeRole(levels[11]);
				}
				if(level >= 951 && level < 1101) {
					if(!roles.contains(levels[12]))
						user.addRole(levels[12]);
				} else {
					if(roles.contains(levels[12]))
						user.removeRole(levels[12]);
				}
				if(level >= 1101) {
					if(!roles.contains(levels[13]))
						user.addRole(levels[13]);
				} else {
					if(roles.contains(levels[13]))
						user.removeRole(levels[13]);
				}
				
				String name = "";
				if(tag != null && !tag.equals("") && !tag.equals("null"))
					name += "[" + tag + "] ";
				String displayName = (String) racerJSON.get("displayName");
				if(displayName != null && !displayName.equals("") && !displayName.equals("null")); 
				else displayName = (String) racerJSON.get("username");
				name += displayName;
				
				//if(!user.getNickname(server).equals(displayName))
				user.updateNickname(server, name);
			} else if(server.getId() == 619938390611066930L) { // ?
				List<Role> roles = user.getRoles(server);
				
				Role registered = server.getRolesByName("Verified").get(0);
				if(!roles.contains(registered))
					user.addRole(registered);
				
				long avgSpeed = (long) racerJSON.get("avgSpeed");
				
				Role[] speed = new Role[17];
				speed[0] = server.getRolesByName("200+ WPM").get(0);
				speed[1] = server.getRolesByName("191-200 WPM").get(0);
				speed[2] = server.getRolesByName("181-190 WPM").get(0);
				speed[3] = server.getRolesByName("171-180 WPM").get(0);
				speed[4] = server.getRolesByName("161-170 WPM").get(0);
				speed[5] = server.getRolesByName("151-160 WPM").get(0);
				speed[6] = server.getRolesByName("141-150 WPM").get(0);
				speed[7] = server.getRolesByName("131-140 WPM").get(0);
				speed[8] = server.getRolesByName("121-130 WPM").get(0);
				speed[9] = server.getRolesByName("111-120 WPM").get(0);
				speed[10] = server.getRolesByName("101-110 WPM").get(0);
				speed[11] = server.getRolesByName("91-100 WPM").get(0);
				speed[12] = server.getRolesByName("80-90 WPM").get(0);
				speed[13] = server.getRolesByName("65-79 WPM").get(0);
				speed[14] = server.getRolesByName("50-64 WPM").get(0);
				speed[15] = server.getRolesByName("25-49 WPM").get(0);
				speed[16] = server.getRolesByName("0-24 WPM").get(0);
				
				if(avgSpeed > 200) {
					if(!roles.contains(speed[0]))
						user.addRole(speed[0]);
				} else if(roles.contains(speed[0]))
					user.removeRole(speed[0]);
				
				if(avgSpeed <= 200 && avgSpeed > 190) {
					if(!roles.contains(speed[1]))
						user.addRole(speed[1]);
				} else if(roles.contains(speed[1]))
					user.removeRole(speed[1]);
				
				if(avgSpeed <= 190 && avgSpeed > 180) {
					if(!roles.contains(speed[2]))
						user.addRole(speed[2]);
				} else if(roles.contains(speed[2]))
					user.removeRole(speed[2]);
				
				if(avgSpeed <= 180 && avgSpeed > 170) {
					if(!roles.contains(speed[3]))
						user.addRole(speed[3]);
				} else if(roles.contains(speed[3]))
					user.removeRole(speed[3]);
				
				if(avgSpeed <= 170 && avgSpeed > 160) {
					if(!roles.contains(speed[4]))
						user.addRole(speed[4]);
				} else if(roles.contains(speed[4]))
					user.removeRole(speed[4]);
				
				if(avgSpeed <= 160 && avgSpeed > 150) {
					if(!roles.contains(speed[5]))
						user.addRole(speed[5]);
				} else if(roles.contains(speed[5]))
					user.removeRole(speed[5]);
				
				if(avgSpeed <= 150 && avgSpeed > 140) {
					if(!roles.contains(speed[6]))
						user.addRole(speed[6]);
				} else if(roles.contains(speed[6]))
					user.removeRole(speed[6]);
				
				if(avgSpeed <= 140 && avgSpeed > 130) {
					if(!roles.contains(speed[7]))
						user.addRole(speed[7]);
				} else if(roles.contains(speed[7]))
					user.removeRole(speed[7]);
				
				if(avgSpeed <= 130 && avgSpeed > 120) {
					if(!roles.contains(speed[8]))
						user.addRole(speed[8]);
				} else if(roles.contains(speed[8]))
					user.removeRole(speed[8]);
				
				if(avgSpeed <= 120 && avgSpeed > 110) {
					if(!roles.contains(speed[9]))
						user.addRole(speed[9]);
				} else if(roles.contains(speed[9]))
					user.removeRole(speed[9]);
				
				if(avgSpeed <= 110 && avgSpeed > 100) {
					if(!roles.contains(speed[10]))
						user.addRole(speed[10]);
				} else if(roles.contains(speed[10]))
					user.removeRole(speed[10]);
				
				if(avgSpeed <= 100 && avgSpeed > 90) {
					if(!roles.contains(speed[11]))
						user.addRole(speed[11]);
				} else if(roles.contains(speed[11]))
						user.removeRole(speed[11]);
			
				if(avgSpeed <= 90 && avgSpeed > 79) {
					if(!roles.contains(speed[12]))
						user.addRole(speed[12]);
				} else if(roles.contains(speed[12]))
					user.removeRole(speed[12]);
				
				if(avgSpeed <= 79 && avgSpeed > 64) {
					if(!roles.contains(speed[13]))
						user.addRole(speed[13]);
				} else if(roles.contains(speed[13]))
					user.removeRole(speed[13]);
				
				if(avgSpeed <= 64 && avgSpeed > 49) {
					if(!roles.contains(speed[14]))
						user.addRole(speed[14]);
				} else if(roles.contains(speed[14]))
					user.removeRole(speed[14]);
				
				if(avgSpeed <= 49 && avgSpeed > 24) {
					if(!roles.contains(speed[15]))
						user.addRole(speed[15]);
				} else if(roles.contains(speed[15]))
					user.removeRole(speed[15]);
				
				if(avgSpeed <= 24) {
					if(!roles.contains(speed[16]))
						user.addRole(speed[16]);
				} else if(roles.contains(speed[16]))
					user.removeRole(speed[16]);
				
				
				JSONArray raceLogs = (JSONArray) racerJSON.get("raceLogs");
				
				if(raceLogs == null) {
					racerJSON = getRacerInfo("#" + (long) racerJSON.get("userID"));
					raceLogs = (JSONArray) racerJSON.get("raceLogs");
				}
				
				long typed = 0, errs = 0;
				for(int i = 0; i < raceLogs.size(); i++) {
					JSONObject race = (JSONObject) raceLogs.get(i);
					typed += (long) race.get("typed");
					errs += (long) race.get("errs");
				}
				double acc = (typed - errs) * 100.0 / typed;
				
				Role[] accuracy = new Role[14];
				accuracy[0] = server.getRolesByName("99%+ Accuracy").get(0);
				accuracy[1] = server.getRolesByName("98% Accuracy").get(0);
				accuracy[2] = server.getRolesByName("97% Accuracy").get(0);
				accuracy[3] = server.getRolesByName("96% Accuracy").get(0);
				accuracy[4] = server.getRolesByName("95% Accuracy").get(0);
				accuracy[5] = server.getRolesByName("94% Accuracy").get(0);
				accuracy[6] = server.getRolesByName("92-93% Accuracy").get(0);
				accuracy[7] = server.getRolesByName("89-91% Accuracy").get(0);
				accuracy[8] = server.getRolesByName("85-88% Accuracy").get(0);
				accuracy[9] = server.getRolesByName("81-84% Accuracy").get(0);
				accuracy[10] = server.getRolesByName("76-80% Accuracy").get(0);
				accuracy[11] = server.getRolesByName("71-75% Accuracy").get(0);
				accuracy[12] = server.getRolesByName("61-70% Accuracy").get(0);
				accuracy[13] = server.getRolesByName(">60% Accuracy").get(0);
				
				//if(acc >= 99) {
				//	if(!roles.contains(accuracy[0]))
				//		user.addRole(accuracy[0]);
				//} else if(roles.contains(accuracy[0]))
				//	user.removeRole(accuracy[0]);
				roleSwitch(acc >= 99, accuracy[0], roles, user);
				roleSwitch(acc < 99 && acc >= 98, accuracy[1], roles, user);
				roleSwitch(acc < 98 && acc >= 97, accuracy[2], roles, user);
				roleSwitch(acc < 97 && acc >= 96, accuracy[3], roles, user);
				roleSwitch(acc < 96 && acc >= 95, accuracy[4], roles, user);
				roleSwitch(acc < 95 && acc >= 94, accuracy[5], roles, user);
				roleSwitch(acc < 94 && acc >= 92, accuracy[6], roles, user);
				roleSwitch(acc < 92 && acc >= 89, accuracy[7], roles, user);
				roleSwitch(acc < 89 && acc >= 85, accuracy[8], roles, user);
				roleSwitch(acc < 85 && acc >= 81, accuracy[9], roles, user);
				roleSwitch(acc < 81 && acc >= 76, accuracy[10], roles, user);
				roleSwitch(acc < 76 && acc >= 71, accuracy[11], roles, user);
				roleSwitch(acc < 71 && acc >= 61, accuracy[12], roles, user);
				roleSwitch(acc < 61, accuracy[13], roles, user);
				
				Role[] cars = new Role[12];
				cars[0] = server.getRolesByName("The Wildflower").get(0);
				cars[1] = server.getRolesByName("Mercedex GT 20.0").get(0);
				cars[2] = server.getRolesByName("McLaro SHS 15.0").get(0);
				cars[3] = server.getRolesByName("McLaro SLR 12.5").get(0);
				cars[4] = server.getRolesByName("McLaro SLR").get(0);
				cars[5] = server.getRolesByName("Bimmer").get(0);
				cars[6] = server.getRolesByName("Lacan").get(0);
				cars[7] = server.getRolesByName("Mantaray").get(0);
				cars[8] = server.getRolesByName("Wach").get(0);
				cars[9] = server.getRolesByName("Pirc").get(0);
				cars[10] = server.getRolesByName("Corsair").get(0);
				cars[11] = server.getRolesByName("Mini Sherman").get(0);
				
				boolean wf = false, merc = false, mcl15 = false, mcl12 = false,	mcl = false, bim = false, 
						lacan = false, manta = false, wach = false, pirc = false, corsair = false, mini = false;
				
				JSONArray garage = (JSONArray) racerJSON.get("cars");
				for(int i = 0; i < garage.size(); i++) {
					long id = (long) ((JSONArray) garage.get(i)).get(0);
					if(id == 166) wf = true;
					else if(id == 153) merc = true;
					else if(id == 137) mcl15 = true;
					else if(id == 129) mcl12 = true;
					else if(id == 124) mcl = true;
					else if(id == 95) bim = true;
					else if(id == 107) lacan = true;
					else if(id == 105) manta = true;
					else if(id == 89) wach = true;
					else if(id == 77) pirc = true;
					else if(id == 68) corsair = true;
					else if(id == 51) mini = true;
				}
				
				roleSwitch(wf, cars[0], roles, user);
				roleSwitch(merc, cars[1], roles, user);
				roleSwitch(mcl15, cars[2], roles, user);
				roleSwitch(mcl12, cars[3], roles, user);
				roleSwitch(mcl, cars[4], roles, user);
				roleSwitch(bim, cars[5], roles, user);
				roleSwitch(lacan, cars[6], roles, user);
				roleSwitch(manta, cars[7], roles, user);
				roleSwitch(wach, cars[8], roles, user);
				roleSwitch(pirc, cars[9], roles, user);
				roleSwitch(corsair, cars[10], roles, user);
				roleSwitch(mini, cars[11], roles, user);
				
			} else if(server.getId() == 681701444617109518L) { // ?
				List<Role> roles = user.getRoles(server);
				
				Role registered = server.getRolesByName("Verified").get(0);
				if(!roles.contains(registered))
					user.addRole(registered);
				
			} else if(server.getId() == NT_OLYMPICS_SERVER) { //NT Olympics
				
				String tag = (String) racerJSON.get("tag");
				String name = "";
				if(tag != null && !tag.equals("") && !tag.equals("null"))
					name += "[" + tag + "] ";
				String displayName = (String) racerJSON.get("displayName");
				if(displayName != null && !displayName.equals("") && !displayName.equals("null")) {
					name += displayName;
				} else name += (String) racerJSON.get("username");
				user.updateNickname(server, name);
				
				boolean gold = ((String) racerJSON.get("membership")).equals("gold");
				
				Role registeredRole = server.getRoleById(704752440381145189L).get(),
						goldRole = server.getRoleById(704752643205103636L).get(),
						olympianRole = server.getRoleById(705580968022507561L).get(),
						audienceRole = server.getRoleById(705581215025070160L).get(),
						choosingRole = server.getRoleById(705771730828001341L).get();
				
				List<Role> roles = user.getRoles(server);
				
				if(!roles.contains(registeredRole))
					user.addRole(registeredRole);
				roleSwitch(gold, goldRole, roles, user);
				roleSwitch(!roles.contains(olympianRole) && !roles.contains(audienceRole), choosingRole, roles, user);
				
			} else if(server.getId() == PARADISE_SERVER) {
				
				List<Role> roles = user.getRoles(server);
				
				{	
					Role verifiedRole = server.getRoleById(758144626011275275L).get(),
						 NTCategoryRole = server.getRoleById(758144626011275276L).get(),
						 serverCategoryRole = server.getRoleById(758144625982046225L).get(),
						 pingCategoryRole = server.getRoleById(758144625592107024L).get();
				
					roleSwitch(true, verifiedRole, roles, user);
					roleSwitch(true, NTCategoryRole, roles, user);
					roleSwitch(true, serverCategoryRole, roles, user);
					roleSwitch(true, pingCategoryRole, roles, user);
				}
				
				roleSwitch(((String) racerJSON.get("membership")).equals("gold"), server.getRolesByName("Gold").get(0), roles, user);
				
				roleSwitch((long) racerJSON.get("money") >= 75_000_000L, server.getRolesByName("Wealthy").get(0), roles, user);
				
				roleSwitch((long) racerJSON.get("longestSession") >= 1000, server.getRolesByName("Sessionist").get(0), roles, user);
				
				{
					boolean v1veteran, v2veteran;
				
					v1veteran = (long) racerJSON.get("createdStamp") < 1430179200L; //1430179200000 v2 released
				
					if(!v1veteran)
						v2veteran = (long) racerJSON.get("createdStamp") < 1559692800L; //1559692800000 v3 released
					else v2veteran = false;
				
					Role v1veteranRole = server.getRolesByName("v1 Veteran").get(0);
					Role v2veteranRole = server.getRolesByName("v2 Veteran").get(0);
				
					roleSwitch(v1veteran, v1veteranRole, roles, user);
					roleSwitch(v2veteran, v2veteranRole, roles, user);
				}
				
				{
					long avgSpeed = (long) racerJSON.get("avgSpeed");
				
					Role[] speedRoles = {
						server.getRolesByName("1-25 WPM").get(0),
						server.getRolesByName("26-50 WPM").get(0),
						server.getRolesByName("51-75 WPM").get(0),
						server.getRolesByName("76-100 WPM").get(0),
						server.getRolesByName("101-130 WPM").get(0),
						server.getRolesByName("131-160 WPM").get(0),
						server.getRolesByName("161-200 WPM").get(0),
						server.getRolesByName("201+ WPM").get(0)
					};
					
					roleSwitch(avgSpeed <= 25, speedRoles[0], roles, user);
					roleSwitch(avgSpeed >= 26 && avgSpeed <= 50, speedRoles[1], roles, user);
					roleSwitch(avgSpeed >= 51 && avgSpeed <= 75, speedRoles[2], roles, user);
					roleSwitch(avgSpeed >= 76 && avgSpeed <= 100, speedRoles[3], roles, user);
					roleSwitch(avgSpeed >= 101 && avgSpeed <= 130, speedRoles[4], roles, user);
					roleSwitch(avgSpeed >= 131 && avgSpeed <= 160, speedRoles[5], roles, user);
					roleSwitch(avgSpeed >= 161 && avgSpeed <= 200, speedRoles[6], roles, user);
					roleSwitch(avgSpeed >= 201, speedRoles[7], roles, user);
				}
				
				{
					JSONArray raceLogs = (JSONArray) racerJSON.get("raceLogs");
					
					if(raceLogs == null) {
						racerJSON = getRacerInfo("#" + (long) racerJSON.get("userID"));
						raceLogs = (JSONArray) racerJSON.get("raceLogs");
					}
					
					long typed = 0, errs = 0;
					for(int i = 0; i < raceLogs.size(); i++) {
						JSONObject race = (JSONObject) raceLogs.get(i);
						typed += (long) race.get("typed");
						errs += (long) race.get("errs");
					}
					
					double acc = (typed - errs) * 100.0 / typed;
					
					Role[] accuracyRoles = {
						server.getRolesByName("1-49% Accuracy").get(0),
						server.getRolesByName("50-88% Accuracy").get(0),
						server.getRolesByName("89-92% Accuracy").get(0),
						server.getRolesByName("93-95% Accuracy").get(0),
						server.getRolesByName("96-100% Accuracy").get(0),
					};
					roleSwitch(acc < 50, accuracyRoles[0], roles, user);
					roleSwitch(acc >= 50 && acc < 89, accuracyRoles[1], roles, user);
					roleSwitch(acc >= 89 && acc < 93, accuracyRoles[2], roles, user);
					roleSwitch(acc >= 93 && acc < 96, accuracyRoles[3], roles, user);
					roleSwitch(acc >= 96, accuracyRoles[4], roles, user);
				}
				
				{
					long racesPlayed = (long) racerJSON.get("racesPlayed");
					
					Role[] races = {
							server.getRolesByName("\"I <3 Typing!\"").get(0),
							server.getRolesByName("\"I Really Love Typing\"").get(0),
							server.getRolesByName("\"Bonkers About Typing\"").get(0),
							server.getRolesByName("\"Bananas About Typing\"").get(0),
							server.getRolesByName("\"You've Gotta Be Kidding\"").get(0),
							server.getRolesByName("\"Corsair\"").get(0),
							server.getRolesByName("\"Pirc\"").get(0),
							server.getRolesByName("\"Carrie\"").get(0),
							server.getRolesByName("\"Anne\"").get(0),
							server.getRolesByName("\"Lackin' Nothin'\"").get(0),
							server.getRolesByName("\"Outback Officer\"").get(0),
							server.getRolesByName("\"I Love Shoes 2\"").get(0),
							server.getRolesByName("\"I Love Shoes 12.5\"").get(0),
							server.getRolesByName("\"I Love Shoes 15.0\"").get(0),
							server.getRolesByName("\"I Love Shoes 20.0\"").get(0),
							server.getRolesByName("\"The Wildest of Flowers\"").get(0),
							server.getRolesByName("\"The Wild Legend\"").get(0)
					};
					
					roleSwitch(racesPlayed < 100, races[0], roles, user);
					roleSwitch(racesPlayed >= 100 && racesPlayed < 500, races[1], roles, user);
					roleSwitch(racesPlayed >= 500 && racesPlayed < 1000, races[2], roles, user);
					roleSwitch(racesPlayed >= 1000 && racesPlayed < 5000, races[3], roles, user);
					roleSwitch(racesPlayed >= 5000 && racesPlayed < 10_000, races[4], roles, user);
					roleSwitch(racesPlayed >= 10_000 && racesPlayed < 20_000, races[5], roles, user);
					roleSwitch(racesPlayed >= 20_000 && racesPlayed < 30_000, races[6], roles, user);
					roleSwitch(racesPlayed >= 30_000 && racesPlayed < 40_000, races[7], roles, user);
					roleSwitch(racesPlayed >= 40_000 && racesPlayed < 50_000, races[8], roles, user);
					roleSwitch(racesPlayed >= 50_000 && racesPlayed < 75_000, races[9], roles, user);
					roleSwitch(racesPlayed >= 75_000 && racesPlayed < 100_000, races[10], roles, user);
					roleSwitch(racesPlayed >= 100_000 && racesPlayed < 125_000, races[11], roles, user);
					roleSwitch(racesPlayed >= 125_000 && racesPlayed < 150_000, races[12], roles, user);
					roleSwitch(racesPlayed >= 150_000 && racesPlayed < 200_000, races[13], roles, user);
					roleSwitch(racesPlayed >= 200_000 && racesPlayed < 250_000, races[14], roles, user);
					roleSwitch(racesPlayed >= 250_000 && racesPlayed < 500_000, races[15], roles, user);
					roleSwitch(racesPlayed >= 500_000, races[16], roles, user);
				}
				
				{
					String tag = (String) racerJSON.get("tag");
					String name = "";
					if(tag != null && !tag.equals("") && !tag.equals("null"))
						name += "[" + tag + "] ";
					String displayName = (String) racerJSON.get("displayName");
					if(displayName != null && !displayName.equals("") && !displayName.equals("null")) {
						name += displayName;
					} else name += (String) racerJSON.get("username");
					
					user.updateNickname(server, name).get();
				}
				
			}
		//Show something to command user to show success or failure
		}
		
		return new EmbedBuilder().setDescription("<@" + user.getIdAsString() + ">'s roles were updated.").setColor(Color.GREEN);
		
	}
	
	private EmbedBuilder teamboard(String board, String time) throws IOException, ParseException {
		
		System.out.printf("teamboard(%s, %s)%n", board, time);
		
		int page = 0;
		String seasonID = null;
		if(time == null) {
			time = "season";
		} else if(time.startsWith("season") && time.length() > 7) {
			seasonID = time.substring(7);
			time = "season";
		}
		if(board == null)
			board = "points";
		JSONObject scoreboardInfo = getScoreboard(board, time, "teams", seasonID);
		
		JSONArray scores = (JSONArray) scoreboardInfo.get("scores");
		EmbedBuilder embed = new EmbedBuilder();

		if(seasonID == null)
			embed.setTitle("__**" + time.toUpperCase().substring(0, 1) + time.substring(1) + " " + board.toUpperCase().substring(0, 1) + board.substring(1) + " Teams Scoreboard**__");
		else embed.setTitle("__**" + seasonById(seasonID) + " " + board.toUpperCase().substring(0, 1) + board.substring(1) + " Teams Scoreboard**__");
		for(int i = 0 + (page * 25); i < 25 + (page * 25); i++) {
			JSONObject score = (JSONObject) scores.get(i);
			String tag = "[" + (String) score.get("tag") + "] ";
			String name = (String) score.get("name");
			long members = (long) score.get("members");
			long played = (long) score.get("played");
			String speed;
			try {
				speed = "" + Double.parseDouble((String) score.get("speed"));
			} catch (ClassCastException cce) {
				speed = "" + (long) score.get("speed");
			}
			long points = (long) score.get("points");
			
			String medal = "";
			if(i == 0) medal += "🥇";
			else if(i == 1) medal += "🥈";
			else if(i == 2) medal += "🥉";
			
			embed.addField((i + 1) + ". " + tag + name, numberToText(members) + " members, " + numberToText(played) + " Races, " + numberToText(points) + " Points, " + speed + " wpm Average " + medal + "[\\🔗](https://www.nitrotype.com/team/" + score.get("tag") + ")");
		}
		
		return embed.setColor(Color.ORANGE);
	}
	
	/**
	 * 
	 * @param seasonID
	 * @return
	 */
	private String seasonById(String seasonID) {
		
		System.out.printf("seasonById(%s)%n", seasonID);
		
		switch(seasonID) {
			case "0": return "Season";
			case "1": return "Season 1";
			case "3": return "Season 0.5";
			case "5": return "Season 2";
			case "6": return "Season 3";
			case "7": return "Season 4";
			case "8": return "Season 5";
			case "9": return "Season 6";
			case "10": return "Season 7";
			case "11": return "Season 8";
			case "12": return "Season 9 (Xmaxx 2017)";
			case "13": return "Season 10";
			case "14": return "Season 11";
			case "15": return "Season 12";
			case "16": return "Season 13 PAC!";
			case "17": return "Season 14";
			case "18": return "Season 15";
			case "19": return "Season 16";
			case "20": return "Back 2 School (2018)";
			case "21": return "Season 17";
			case "22": return "Season 18";
			case "23": return "Season 19";
			case "24": return "Season 20";
			case "25": return "Season 21";
			case "26": return "Season 22";
			case "27": return "Season 23 (Back 2 School)";
			case "28": return "Season 24";
			case "29": return "Season 25 (XMaxx)";
			case "30": return "Season 26";
			case "31": return "Season 27";
			
		}
		return null;
	}
	
	/**
	 * 
	 * @param board - points, speed or hof
	 * @param time - daily, weekly, monthly, or season w/ #
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private EmbedBuilder leaderboard(String board, String time) throws ParseException, IOException {

		System.out.printf("leaderboard(%s, %s)%n", board, time);
		
		String seasonID = null;
		if(time == null) {
			time = "season";
		} else if(time.startsWith("season") && time.length() > 7) {
			seasonID = time.substring(7);
			time = "season";
		}
		
		if(board == null)
			board = "points";
		
		JSONObject scoreboardInfo = getScoreboard(board, time, "racer", seasonID);
		
		//System.out.println(scoreboardInfo);
		JSONArray scores = (JSONArray) scoreboardInfo.get("scores");
		
		EmbedBuilder embed = new EmbedBuilder();
		
		if(!board.equals("hof")) {
			
			if(seasonID == null)
				embed.setTitle("__**" + time.toUpperCase().substring(0, 1) + time.substring(1) + " " + board.toUpperCase().substring(0, 1) + board.substring(1) + " Scoreboard**__");
			else embed.setTitle("__**" + seasonById(seasonID) + " " + board.toUpperCase().substring(0, 1) + board.substring(1) + " Scoreboard**__");
			
			for(int i = 0; i < 25; i++) {
				
				JSONObject score = (JSONObject) scores.get(i);
				
				String tag = "";
				if(score.get("tag") != null)
					tag = "[" + (String) score.get("tag") + "] ";
				
				String username = (String) score.get("username");
				
				String displayName = username;
				if(score.get("displayName") != null && !score.get("displayName").equals(""))
					displayName = (String) score.get("displayName");
				
				long played = (long) score.get("played");
				
				String speed;
				try {
					speed = "" + Double.parseDouble((String) score.get("speed"));
				} catch (ClassCastException cce) {
					speed = "" + (long) score.get("speed");
				}
				
				long highestSpeed = (long) score.get("highestSpeed");
				long points = (long) score.get("points");
				
				String medal = "";
				if(i == 0) medal += "🥇";
				else if(i == 1) medal += "🥈";
				else if(i == 2) medal += "🥉";
		
				embed.addField((i + 1) + ". " + tag + displayName, numberToText(played) + " Races, " + numberToText(points) + " Points, " + speed + " wpm Average, " + highestSpeed + " wpm High " + medal + "[\\🔗](https://www.nitrotype.com/racer/" + username + ")");
			
			}
			
		} else {
			
			embed.setTitle("__**Hall of Fame**__");
			String desc1 = "", desc2 = "", desc3 = "", desc4 = "", desc5 = "", desc6 = "", desc7 = "";
			for(int i = 0; i < 21; i++) {
				JSONObject score = (JSONObject) scores.get(i);
				String tag = "";
				if(score.get("tag") != null)
					tag = "[" + (String) score.get("tag") + "] ";
				String username = (String) score.get("username");
				String displayName = username;
				if(score.get("displayName") != null && !score.get("displayName").equals(""))
					displayName = (String) score.get("displayName");
				String category = (String) score.get("category");
				if(category.equals("longestSession"))
					desc3 += (long) score.get("rank") + ". " + tag + displayName + " " + numberToText((long) score.get("value")) + " Races [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else if(category.equals("money"))
					desc4 += (long) score.get("rank") + ". " + tag + displayName + " $" + numberToText((long) score.get("value")) + " [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else if(category.equals("moneySpent"))
					desc5 += (long) score.get("rank") + ". " + tag + displayName + " $" + numberToText((long) score.get("value")) + " [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else if(category.equals("nitrosUsed"))
					desc6 += (long) score.get("rank") + ". " + tag + displayName + " " + numberToText((long) score.get("value")) + " Nitros [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else if(category.equals("oldest"))
					desc7 += (long) score.get("rank") + ". " + tag + displayName + " " + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(((long) score.get("value")) * 1000), ZoneId.systemDefault())) + " [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else if(category.equals("played"))
					desc1 += (long) score.get("rank") + ". " + tag + displayName + " " + numberToText((long) score.get("value")) + " Races [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
				else desc2 += (long) score.get("rank") + ". " + tag + displayName + " " + numberToText((long) score.get("value")) + " wpm [\\🔗](https://www.nitrotype.com/racer/" + username + ")\n";
			}
			embed.addField("Most Active", desc1);
			embed.addField("Fastest Racer", desc2);
			embed.addField("Longest Session", desc3);
			embed.addField("Most Money", desc4);
			embed.addField("Biggest Spender", desc5);
			embed.addField("Nitros Used", desc6);
			embed.addField("Oldest Active Player", desc7);
		}
		return embed.setColor(Color.MAGENTA);
	}
	/**
	 * 
	 * @param board
	 * @param time
	 * @param grouping
	 * @param seasonID
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private JSONObject getScoreboard(String board, String time, String grouping, String seasonID) throws IOException, ParseException {
		
		System.out.printf("getScoreboard(%s, %s, %s, %s)%n", board, time, grouping, seasonID);
		
		System.setProperty("http.agent", "Chrome");
		
		Map<String, String> parameters = new HashMap<>();
		parameters.put("board", board);
		parameters.put("time", time);
		if(seasonID != null)
			parameters.put("seasonID", seasonID);
		parameters.put("grouping", grouping);

		URL url = new URL("https://www.nitrotype.com/api/scoreboard?" + ParameterStringBuilder.getParamsString(parameters));
		System.out.println(url.toString());
		
		url.openConnection();
		Scanner s = new Scanner(url.openStream());
		String scoreboardInfo = "";
		scoreboardInfo = s.nextLine();
		s.close();
		
		return (JSONObject) ((JSONObject) new JSONParser().parse(scoreboardInfo)).get("data");
		
	}
	
	private EmbedBuilder stats(String user) throws StringIndexOutOfBoundsException, IOException, ParseException {
		
		System.out.printf("stats(%s)%n", user);
		
		JSONObject racerInfo = getRacerInfo(user);
		
		long money = (long) racerInfo.get("money"), 
				moneySpent = (long) racerInfo.get("moneySpent"),
				level = (long) racerInfo.get("level"),
				createdStamp = (long) racerInfo.get("createdStamp"),
				profileViews = (long) racerInfo.get("profileViews"),
				experience = (long) racerInfo.get("experience"), 
				racesPlayed = (long) racerInfo.get("racesPlayed"),
				placed1 = (long) racerInfo.get("placed1"),
				placed2 = (long) racerInfo.get("placed2"),
				placed3 = (long) racerInfo.get("placed3"),
				nitros = (long) racerInfo.get("nitros"), 
				nitrosUsed = (long) racerInfo.get("nitrosUsed"),
				longestSession = (long) racerInfo.get("longestSession"),
				avgSpeed = (long) racerInfo.get("avgSpeed"),
				highestSpeed = (long) racerInfo.get("highestSpeed");
		long achievementPoints = 0;
		if(racerInfo.get("achievementPoints") != null)
			try {
				achievementPoints = (long) racerInfo.get("achievementPoints");
			} catch(ClassCastException cce) {
				achievementPoints = Long.parseLong((String) racerInfo.get("achievementPoints")); //TODO switch to long
			}
		
		String title = "", tag = "", displayName = (String) racerInfo.get("username");
		if(racerInfo.get("title") != null)
			title = (String) racerInfo.get("title");
		if(racerInfo.get("tag") != null)
			tag = "[" + (String) racerInfo.get("tag") + "]";
		if(racerInfo.get("displayName") != null && !((String) racerInfo.get("displayName")).equals(""))
			displayName = (String) racerInfo.get("displayName");
		
		byte gold = 0;
		if(((String) racerInfo.get("membership")).equals("gold"))
			gold++;
		JSONArray cars = (JSONArray) racerInfo.get("cars");
		int ownedCars = 0;
		int soldCars = 0;
		
		for(int i = 0; i < cars.size(); i++) {
			JSONArray car = (JSONArray) cars.get(i);
			if(((String) car.get(1)).equals("owned"))
				ownedCars++;
			else soldCars++;			
		}
		
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Color.PINK)
				.addInlineField("Info", "Level **" + level + "**\n**" + numberToText(experience) 
						+ "** experience\n**" + numberToText(achievementPoints) + "** achievement points\nCreated at **" + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(createdStamp * 1000), ZoneId.systemDefault())) 
						+ "**\n**" + numberToText(profileViews) + "** profile views")
				.addInlineField("Speed", "Average **" + avgSpeed + "** wpm (" + (avgSpeed * 5) + " cpm)\nHighest **" + highestSpeed + "** wpm (" + (highestSpeed * 5) + " cpm)")
				.addInlineField("Money", "**" + moneyToText(money) + "** owned\n**" + moneyToText(moneySpent) + "** spent")
				.addInlineField("Nitros", "**" + numberToText(nitros) + "** owned\n**" + numberToText(nitrosUsed) + "** used (" + ((long) (nitrosUsed * 1000.0 / racesPlayed)) / 1000.0 + " / race)")
				.addInlineField("Cars", "**" + ownedCars + "** owned\n**" + soldCars + "** sold")
				.addInlineField("Races", "Total **" + numberToText(racesPlayed) + "** (" + ((int) (racesPlayed * 1000.0 / ((System.currentTimeMillis() / 1000 - createdStamp) / 86400.0))) / 1000.0 + " races / day)"
						+ "\nLongest Session **"+ numberToText(longestSession) + "**"
						+ "\n🥇 " + numberToText(placed1) + " (**" + fractionToText(placed1, racesPlayed) + "**)"
						+ "\n🥈 " + numberToText(placed2) + " (**" + fractionToText(placed2, racesPlayed) + "**)"
						+ "\n🥉 " + numberToText(placed3) + " (**" + fractionToText(placed3, racesPlayed) + "**)"
						+ "\n**" + ((int) ((racesPlayed * 1000 + placed1 * 100 + placed2 * 50 + placed3 * 30) * 1000.0 / racesPlayed)) / 1000.0 + "** exp / race");
		
		JSONArray racingStats = (JSONArray) racerInfo.get("racingStats");
		if(!racingStats.isEmpty()) {
			String description = "";
			for(int i = 0; i < racingStats.size(); i++) {
				JSONObject stat = (JSONObject) racingStats.get(i);
				String board = (String) stat.get("board");
				if(board.equals("alltime"))
					board = "all-time";
				//long rank = 0;
				//if(stat.containsKey("rank"))
				//	rank = (long) stat.get("rank");
				long played = (long) stat.get("played"),
						typed = (long) stat.get("typed"),
						errs = (long) stat.get("errs");
				double secs = 0.0;
				try {
					secs = Double.parseDouble((String) stat.get("secs"));
				} catch(ClassCastException cce) {
					secs += (long) stat.get("secs");
				}
				description += "**" + board.substring(0, 1).toUpperCase() + board.substring(1) + "** \n"
						//+ "Rank " + numberToText(rank) + "\n" 
						+ numberToText((int) (played * (100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed + 0.5))
						+ " points (" + ((long) ((100 + typed / secs / 5 * 60 / 2) * (typed - errs) / typed * 1000)) / 1000.0 + " pts / race)"
						+ "\n" + numberToText(played) + " races (" + fractionToText(typed - errs, typed) + " acc, " + ((long) (typed / secs / 5 * 60 * 1000)) / 1000.0 + " wpm)\n";
			}
			embed.addInlineField("Leaderboards", description);
		}
		if(gold != 0)
			embed.setColor(new Color(0xFFD700));
		
		if(racerInfo.containsKey("tagColor")) {
			String color = (String) racerInfo.get("tagColor");
			if(color != null && !color.equals("") && !color.equals("null"))
				embed.setColor(Color.decode("#" + color));
		}
		
		embed.setAuthor(tag + " " + displayName + "\n" + title, "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
		return embed;
	}
	
	private EmbedBuilder minesweeper(String bombs, String difficulty) {
		
		System.out.printf("minesweeper(%s, %s)%n", bombs, difficulty);
		
		int mines;
		try {
			mines = Integer.parseInt(bombs);
		} catch (NumberFormatException nfe) {
			mines = 10;
		}
		
		int cellsPerMine = 4;
		
		if(difficulty.equalsIgnoreCase("easy"))
			cellsPerMine = 5;
		if(difficulty.equalsIgnoreCase("normal"));
		if(difficulty.equalsIgnoreCase("hard"))
			cellsPerMine = 3;
		if(difficulty.equalsIgnoreCase("impossible"))
			cellsPerMine = 2;
		
		int width = 2, height = 2;
		while(mines > width * height / cellsPerMine) {
			width++;
			height++;
		}
		
		int[] field = new int[width * height];
		
		for (int i = 0; i < mines;) {
			int position = (int) (field.length * Math.random());
			if((field[position] != -1)) {
				field[position] = -1;
				i++;
			}
		}
		
		for(int i = 0; i < field.length; i++) {
			if(field[i] != -1) { 
				if(i == 0) { //top left corner
					if(field[i + 1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
					if(field[i + width + 1] == -1)
						field[i]++;
				} else if(i == width - 1) { //top right corner
					if(field[i - 1] == -1)
						field[i]++;
					if(field[i + width - 1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
				} else if(i == width * height - width) { // bottom left corner
					if(field[i - width] == -1)
						field[i]++;
					if(field[i - width + 1] == -1)
						field[i]++;
					if(field[i + 1] == -1)
						field[i]++;
				} else if(i == width * height - 1) {
					if(field[i - width - 1] == -1)
						field[i]++;
					if(field[i - width] == -1)
						field[i]++;
					if(field[i - 1] == -1)
						field[i]++;
				} else if(i / width == 0) { //top row
					if(field[i - 1] == -1) 
						field[i]++;
					if(field[i + 1] == -1)
						field[i]++;
					if(field[i + width -1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
					if(field[i + width + 1] == -1)
						field[i]++;
				} else if(i / width == height - 1) { // bottom row
					if(field[i - 1] == -1) 
						field[i]++;
					if(field[i + 1] == -1)
						field[i]++;
					if(field[i - width -1] == -1)
						field[i]++;
					if(field[i - width] == -1)
						field[i]++;
					if(field[i - width + 1] == -1)
						field[i]++;
				} else if(i % width == 0) { //left column
					if(field[i - width] == -1) 
						field[i]++;
					if(field[i - width + 1] == -1)
						field[i]++;
					if(field[i + 1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
					if(field[i + width + 1] == -1)
						field[i]++;
				} else if(i % width == width - 1) { //right column
					if(field[i - width - 1] == -1) 
						field[i]++;
					if(field[i - width] == -1)
						field[i]++;
					if(field[i - 1] == -1)
						field[i]++;
					if(field[i + width - 1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
				} else { //middle cell
					if(field[i - width - 1] == -1)
						field[i]++;
					if(field[i - width] == -1)
						field[i]++;
					if(field[i - width + 1] == -1)
						field[i]++;
					if(field[i - 1] == -1)
						field[i]++;
					if(field[i + 1] == -1)
						field[i]++;
					if(field[i + width - 1] == -1)
						field[i]++;
					if(field[i + width] == -1)
						field[i]++;
					if(field[i + width + 1] == -1)
						field[i]++;
				}
			}
		}
		
		String text = "";
		
		for(int i = 0; i < field.length; i++) {
			text += "||:";
			if(field[i] == -1)
				text += "boom";
			else if(field[i] == 0)
				text += "zero";
			else if(field[i] == 1)
				text += "one";
			else if(field[i] == 2)
				text += "two";
			else if(field[i] == 3)
				text += "three";
			else if(field[i] == 4)
				text += "four";
			else if(field[i] == 5)
				text += "five";
			else if(field[i] == 6)
				text += "six";
			else if(field[i] == 7)
				text += "seven";
			else if(field[i] == 8)
				text += "eight";
			text += ":||";
			if(i % width == width - 1)
				text += "\n";
		}
		
		return new EmbedBuilder().setDescription(text).setColor(Color.WHITE).setFooter("Minesweeper, " + mines + " mines remaining.");
		
	}
	
	private EmbedBuilder info(boolean isAdmin) {
		
		System.out.printf("info(%b)%n", isAdmin);
		
		long runtime = System.currentTimeMillis() - Main.getLoginStamp();
		long[] time = {runtime / 86400000, runtime % 86400000 / 3600000, runtime % 86400000 % 3600000 / 60000, runtime % 86400000 % 3600000 % 60000 / 1000};
		String timeStr;
		if(time[0] == 0) timeStr = "";
		else if(time[0] == 1) timeStr = "1 day ";
		else timeStr = time[0] + " days ";
		if(time[1] == 0);
		else if(time[1] == 1) timeStr += "1 hour ";
		else timeStr += time[1] + " hours ";
		if(time[2] == 0);
		else if(time[2] == 1) timeStr += "1 minute ";
		else timeStr += time[2] + " minutes ";
		if(time[3] == 0);
		else if(time[3] == 1) timeStr += "1 second ";
		else timeStr += time[3] + " seconds ";
		int[] counter = Main.getCounter();
		String commandStr = "";
		for(int i = 0; i < counter.length; i++)
			if(counter[i] != 0)
				commandStr += "**" + Main.getCommand(i)[0] + "**: " + counter[i] + "\n";
		int[] eCounter = Main.getECounter();
		String errorStr = "";
		for(int j = 0; j < eCounter.length; j++)
			if(eCounter[j] != 0)
				errorStr += "**" + Main.getError(j) + "**: " + eCounter[j] + "\n";
		int[] aCounter = Main.getACounter();
		String adminStr = "";
		for(int k = 0; k < aCounter.length; k++)
			if(aCounter[k] != 0)
				adminStr += "**" + Main.getAdminCommand(k)[0] + "**: " + aCounter[k] + "\n";
		EmbedBuilder embed = new EmbedBuilder().setTitle("Little Lamborgotti").setDescription("A Discord bot for Nitro Type users.")
				.setAuthor("About", "https://discord.gg/KNWzUPn", "")
				.addField("Creation", "Little Lamborgotti is a Discord Bot application created by Nes370.\nShe is written in Java and runs on Java 8.\nShe uses:\n • [Javacord library](https://github.com/Javacord/Javacord) v3.1.0\n • [Fangyidong's JSON Simple library](https://github.com/fangyidong/json-simple) v1.1.1\nYou can read her source code on [GitHub](https://github.com/Nes370/lolydealer)")
				.addField("Support Development", "\nIf you'd like to support the developer, consider making a one-time donation via [PayPal](https://paypal.me/nes370) or become a patron at [Patreon](https://www.patreon.com/nes370).")
				.addField("Invite/Contact", "If you wish to add this bot to your server, please use the following link:\nhttps://discord.com/api/oauth2/authorize?client_id=455132593642536983&permissions=0&scope=bot\nIf you have any further queries, contact " + Main.getDeveloperID() + " (Nes#9856) on Discord.").setColor(new Color(30, 144, 255))
				.addInlineField("Runtime", timeStr)
				.setFooter("Developed by Nes370");
		if(isAdmin)
			embed.setColor(new Color(138, 43, 226));
			if(adminStr.length() != 0)
				embed.addInlineField("Admin Commands", adminStr);
		if(commandStr.length() != 0)
			embed.addInlineField("Commands", commandStr);
		if(errorStr.length() != 0)
			embed.addInlineField("Errors", errorStr);

		System.out.println("Information sent.");
		return embed;
	}
	
	private Collection<? extends EmbedBuilder> help(boolean isAdmin, String prefix) {
		
		System.out.printf("help(%b, %s)%n", isAdmin, prefix);
		
		List<EmbedBuilder> embedList = new ArrayList<EmbedBuilder>();
		String category = "";
		EmbedBuilder embed = null;
		for(int i = 0; i < Main.getCommandsLength(); i++) {
			String[] command = Main.getCommand(i);
			if(!command[4].equals(category)) {
				category = command[4];
				embedList.add(embed = new EmbedBuilder().setAuthor(command[4] + " Commands").setColor(Color.YELLOW));
			}
			String description = command[1] + "\n" + prefix + command[0];
			if(command[2] != null)
				description += " " + command[2].substring(1);
			if(command[3] != null)
				description += " " + command[3].substring(1);
			embed.addInlineField(command[0], description);
		}
		return embedList;
	}
	
	private EmbedBuilder unregister(String discordId, String displayName) throws IOException, NumberFormatException {
		
		System.out.printf("unregister(%s, %s)%n", discordId, displayName);
		
		if(discordId.startsWith("<@!") && discordId.endsWith(">"))
			discordId = discordId.substring(3, discordId.indexOf(">"));
		else if(discordId.startsWith("<@") && discordId.endsWith(">"))
			discordId = discordId.substring(2, discordId.indexOf(">"));
		if(!isLong(discordId))
			throw new NumberFormatException();
		
		if(Main.getApi().getServerById(NITRO_TYPE_SERVER).get().getMemberById(discordId).isPresent()) {
			Server server = Main.getApi().getServerById(NITRO_TYPE_SERVER).get();
			User user = server.getMemberById(discordId).get();
			user.removeRole(server.getRolesByName("Registered").get(0), "Unregistered");
		}
		//BOK Server
		if(Main.getApi().getServerById(641017355551506444L).get().getMemberById(discordId).isPresent()) {
			Server server = Main.getApi().getServerById(641017355551506444L).get();
			User user = server.getMemberById(discordId).get();
			user.removeRole(server.getRolesByName("Registered").get(0), "Unregistered");
		}
		//NT Olympics
		if(Main.getApi().getServerById(NT_OLYMPICS_SERVER).get().getMemberById(discordId).isPresent()) {
			Server server = Main.getApi().getServerById(NT_OLYMPICS_SERVER).get();
			User user = server.getMemberById(discordId).get();
			user.removeRole(server.getRoleById(704752440381145189L).get(), "Unregistered"); /*.getRolesByName("Olympian")*/
		}
		
		if(Main.getApi().getServerById(PARADISE_SERVER).get().getMemberById(discordId).isPresent()) {
			Server server = Main.getApi().getServerById(PARADISE_SERVER).get();
			User user = server.getMemberById(discordId).get();
			user.removeRole(server.getRolesByName("Verified").get(0), "Unregistered");
		}
		
		String verifyDeleted = "";
		JSONArray verification = (JSONArray) Main.readJSON(Main.getResourcePath() + "Verification.json");
		for(int i = 0; i < verification.size(); i++) {
			if(discordId.equals("" + (long) ((JSONObject) verification.get(i)).get("discordId"))) {
				verification.remove(i);
				FileWriter file = new FileWriter(Main.getResourcePath() + "Verification.json");
				file.write(verification.toJSONString());
				file.close();
				verifyDeleted = "\nA pending or expired verification was removed.";
			}
		}
		JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
		for(int i = 0; i < register.size(); i++)
			if(((Long) ((JSONObject) register.get(i)).get("discordId")).longValue() == Long.parseLong(discordId)) {
				register.remove(i);
				FileWriter file = new FileWriter(Main.getResourcePath() + "Register.json");
				file.write(register.toJSONString());
				file.close();
				return new EmbedBuilder().setDescription("<@" + discordId + "> is no longer associated with a Nitro Type account." + verifyDeleted).setColor(Color.GREEN).setFooter("Report for " + displayName);
			}
		return new EmbedBuilder().setDescription("An associated account could not be found for <@" + discordId + ">.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
	}
	
	@SuppressWarnings("unchecked")
	private EmbedBuilder register(String discordId, String username, String displayName, boolean verified) throws IOException, NumberFormatException, StringIndexOutOfBoundsException {
		
		System.out.printf("register(%s, %s, %s, %b)%n", discordId, username, displayName, verified);
		
		//Strip any @mentions from the id number
		if(discordId.startsWith("<@!") && discordId.endsWith(">"))
			discordId = discordId.substring(3, discordId.indexOf(">"));
		else if(discordId.startsWith("<@") && discordId.endsWith(">"))
			discordId = discordId.substring(2, discordId.indexOf(">"));
		//If the discord ID input is not a number, then return an error.
		if(!isLong(discordId))
			throw new NumberFormatException();
		if(username.startsWith("[") && username.endsWith("]"))
			username = username.substring(1, username.length() - 1);
		//Strip a URL down to the username
		if(username.contains("nitrotype.com/racer/"))
			username = username.substring(username.indexOf("nitrotype.com/racer/") + 20);
		boolean registeredWithUserID = false;
		if(username.startsWith("#"))
			registeredWithUserID = true;
		
		JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
		for(int i = 0; i < register.size(); i++) {
			JSONObject e = (JSONObject) register.get(i);
			if(((Long) e.get("discordId")).longValue() == Long.parseLong(discordId))
				return new EmbedBuilder().setDescription("<@" + discordId + "> is already registered to **" + (String) e.get("username") + "**.\nUse the command `L.unregister` if you wish to change your registered account.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
			if(registeredWithUserID) {
				if(e.containsKey("userID") && ((String) e.get("userID")).equals(username.substring(1)))
					return new EmbedBuilder().setDescription("<@" + (long) e.get("discordId") + "> is already registered to **" + (String) e.get("username") + "**.\nAsk a moderator for assistance if this registration is invalid.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
			} else if(((String) e.get("username")).equalsIgnoreCase(username))
				return new EmbedBuilder().setDescription("<@" + (long) e.get("discordId") + "> is already registered to **" + (String) e.get("username") + "**.\nAsk a moderator for assistance if this registration is invalid.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
		}
		
		JSONObject racerInfo = null;
		String description = "";
		long userID = 0;
		try {
			racerInfo = getRacerInfo(username);
			userID = (long) racerInfo.get("userID");
		} catch (StringIndexOutOfBoundsException | ParseException sioobe) {
			
			try {
				JSONArray searchResults = searchPlayers(username);
				System.out.println(searchResults.toJSONString());
				if(searchResults.size() == 1) {
					description += "The name " + username + " matches an account with the username ";
					JSONObject result = (JSONObject) searchResults.get(0);
					username = (String) result.get("username");
					description += username + ".";
					//userID = (long) result.get("userID");
					return register(discordId, username, displayName, verified).setTitle(description);
					//description += username;
				} else if(searchResults.size() > 0) {
					description += "\nIf one of these are your account, register with the username on the left.";
					System.out.println("hello");
					for(int i = 0; i < searchResults.size(); i++) {
						JSONObject result = (JSONObject) searchResults.get(i);
						description += "\n**" + ((String) result.get("username")).replaceAll("\\*", "\\\\*")
								.replaceAll("_", "\\\\_")
								.replaceAll("~", "\\\\~")
								.replaceAll("`", "\\\\`")
								.replaceAll("\\|","\\\\|") + "**";
						if(result.get("displayName") != null && !((String) result.get("displayName")).equals(""))
							description += " (" + ((String) result.get("displayName")).replaceAll("\\*", "\\\\*")
									.replaceAll("_", "\\\\_")
									.replaceAll("~", "\\~")
									.replaceAll("`", "\\`")
									.replaceAll("\\|","\\\\|") + ")";
					}
				}
				
				//if(userID == 0)
					return new EmbedBuilder().setDescription("**" + username + "** does not appear to be a valid Nitro Type username or ID." + description).setColor(Color.YELLOW).setFooter("Warning for " + displayName);
			} catch(ParseException pe2) { description += "\nAdditionally, a player search request from NT failed to parse."; }
			
			//if(userID == 0)
			return new EmbedBuilder().setDescription("**" + username + "** does not appear to be a valid Nitro Type username or ID." + description).setColor(Color.YELLOW).setFooter("Warning for " + displayName);
		}
		
		JSONObject entry = new JSONObject();
		entry.put("discordId", Long.parseLong(discordId));
		entry.put("username", username);
		entry.put("verified", verified);
		entry.put("userID", userID);
		register.add(entry);
		FileWriter file = new FileWriter(Main.getResourcePath() + "Register.json");
		file.write(register.toJSONString());
		file.close();
		if(!verified)
			return new EmbedBuilder().setDescription("<@" + discordId + "> is now registered to **" + username + "**.\nUse the command `L.verify` to verify ownership of your registered account.").setColor(Color.GREEN).setFooter("Report for " + displayName);
		else return new EmbedBuilder().setDescription("<@" + discordId + "> is now registered to **" + username + "** and verified.\nUse the command `L.update` to update roles.").setColor(Color.GREEN).setFooter("Report for " + displayName);
	
	}
	
	private Collection<? extends EmbedBuilder> sold(String user, String length) {
		// TODO Auto-generated method stub
		List<EmbedBuilder> ooorder = new ArrayList<EmbedBuilder>();
		ooorder.add(new EmbedBuilder().setDescription("Not implemented at this time."));
		return ooorder;
	}
	private Collection<? extends EmbedBuilder> garage(String user, String length) {
		// TODO Auto-generated method stub
		List<EmbedBuilder> ooorder = new ArrayList<EmbedBuilder>();
		ooorder.add(new EmbedBuilder().setDescription("Not implemented at this time."));
		return ooorder;
	}
	private EmbedBuilder compare(String user1, String user2) {
		// TODO Auto-generated method stub
		return new EmbedBuilder().setDescription("Not implemented at this time.");
	}
	
	/**
	 * 
	 * @param user - an id, <literal>@mention</literal>, a user name, or a link.
	 * @param bonus
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws StringIndexOutOfBoundsException
	 */
	private EmbedBuilder value(String user, String bonus) throws IOException, ParseException, StringIndexOutOfBoundsException {
		
		System.out.printf("value(%s, %s)%n", user, bonus);

		JSONObject racerInfo = getRacerInfo(user);
		JSONArray carInfo = getCarInfo();
		
		long money = (long) racerInfo.get("money"), 
				experience = (long) racerInfo.get("experience"), 
				racesPlayed = (long) racerInfo.get("racesPlayed"),
				placed1 = (long) racerInfo.get("placed1"),
				placed2 = (long) racerInfo.get("placed2"),
				placed3 = (long) racerInfo.get("placed3"),
				nitros = (long) racerInfo.get("nitros"), 
				longestSession = (long) racerInfo.get("longestSession"),
				highestSpeed = (long) racerInfo.get("highestSpeed");
		byte gold = 0;
		if(((String) racerInfo.get("membership")).equals("gold"))
			gold++;
		double age = ((System.currentTimeMillis() / 1000) - (Long) racerInfo.get("createdStamp")) / 31536000.0;
		JSONArray cars = (JSONArray) racerInfo.get("cars");
		long liquidCars = 0, subjectiveCars = 0;
		
		for(int i = 0; i < cars.size(); i++) {
			JSONArray car = (JSONArray) cars.get(i);
			if(((String) car.get(1)).equals("owned")) {
				liquidCars += 0.6 * carValue(carInfo, (long) car.get(0));
				subjectiveCars += 0.4 * carValue(carInfo, (long) car.get(0)) * Math.pow(2, (System.currentTimeMillis() / 1000 - lastObtainable((int) (long) car.get(0))) / 31536000.0);
			} else if(System.currentTimeMillis() / 1000 - 41688324 > lastObtainable((int) (long) car.get(0)))
				subjectiveCars += carValue(carInfo, (long) car.get(0)) * (0.4 * Math.pow(2, (System.currentTimeMillis() / 1000 - lastObtainable((int) (long) car.get(0))) / 31536000.0) - 1);
		}
		
		long bonusValue = 0;
		if(isLong(bonus))
			bonusValue += Long.parseLong(bonus);
		
		long liquid = money + liquidCars;
		long subjective = (long) ((gold * 10000000) + experience + (racesPlayed * 750 + placed1 * 500 + placed2 * 333 + placed3 * 167) + (longestSession * 2000) + subjectiveCars + (50000 * Math.pow(2, (highestSpeed - 40) / 20.56) - 12981) + (50000 * Math.pow(2, age))) + bonusValue + (nitros * 500);
		
		EmbedBuilder embed = new EmbedBuilder().setTitle("__**Account Value**__:").setDescription("**" + moneyToText(liquid + subjective) + "**")
				.setColor(Color.PINK)
				.addInlineField("Liquid", "**" + fractionToText(liquid, liquid + subjective) + "**")
				.addInlineField("Subjective", "**" +fractionToText(subjective, liquid + subjective) + "**")
				.addField("__**Liquid Value**__:", "**" + moneyToText(liquid) + "**")
				.addInlineField("Cash", "**" + fractionToText(money, liquid) + "**" + " (" + moneyToText(money) + ")")
				.addInlineField("Cars", "**" + fractionToText(liquidCars, liquid) + "**" + " (" + moneyToText(liquidCars) + ")")
				.addField("__**Subjective Value**__:", "**" + moneyToText(subjective) + "**");
		if(gold != 0)
			embed.addInlineField("Gold", "**" + fractionToText(gold * 10000000, subjective) + "**" + " (" + moneyToText(gold * 10000000) + ")").setColor(new Color(0xFFD700));
		embed.addInlineField("Age", "**" + fractionToText(Math.pow(2.0, age) * 50000, subjective) + "**" + " (" + moneyToText((long)(Math.pow(2.0, age) * 50000)) + ")")
				.addInlineField("Experience", "**" + fractionToText(experience, subjective) + "**" + " (" + moneyToText(experience) + ")")
				.addInlineField("Races", "**" + fractionToText(racesPlayed * 750 + placed1 * 500 + placed2 * 333 + placed3 * 167, subjective) + "**" + " (" + moneyToText(racesPlayed * 750 + placed1 * 500 + placed2 * 333 + placed3 * 167) + ")")
				.addInlineField("Highest Speed", "**" + fractionToText((50000 * Math.pow(2, (highestSpeed - 40) / 20.56) - 12981), subjective) + "**" + " (" + moneyToText((long) (50000 * Math.pow(2, (highestSpeed - 40) / 20.56) - 12981)) + ")")
				.addInlineField("Longest Session", "**" + fractionToText(longestSession * 2000, subjective) + "**" + " (" + moneyToText(longestSession * 2000) + ")")		
				.addInlineField("Nitros", "**" + fractionToText(nitros * 500, liquid) + "**" + " (" + moneyToText(nitros * 500) + ")")
				.addInlineField("Cars", "**" + fractionToText(subjectiveCars, subjective) + "**" + " (" + moneyToText(subjectiveCars) + ")");
		if(racerInfo.get("tag") != null) {
			if(racerInfo.get("displayName") != null && !((String) racerInfo.get("displayName")).equals(""))
				embed.setAuthor("[" + (String) racerInfo.get("tag") + "] " + (String) racerInfo.get("displayName"), "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
			else embed.setAuthor("[" + (String) racerInfo.get("tag") + "] " + (String) racerInfo.get("username"), "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
		}
		else if(racerInfo.get("displayName") != null && !((String) racerInfo.get("displayName")).equals("")) 
			embed.setAuthor((String) racerInfo.get("displayName"), "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
		else embed.setAuthor((String) racerInfo.get("username"), "https://www.nitrotype.com/racer/" + (String) racerInfo.get("username"), "");
		if(bonusValue != 0)
			embed.addInlineField("Bonus", "**" + fractionToText(bonusValue, subjective) + "**" + " (" + moneyToText(bonusValue) + ")");
		
		if(racerInfo.containsKey("tagColor")) {
			String color = (String) racerInfo.get("tagColor");
			if(color != null && !color.equals("") && !color.equals("null"))
				embed.setColor(Color.decode("#" + color));
		}
		
		return embed;
		
	}

	private String fractionToText(long nominator, long denominator) {
		return (long)(10000.0 * nominator / denominator) / 100.0 + "%";
	}
	
	private static String fractionToText(double nominator, long denominator) {
		return (long)(10000.0 * nominator / denominator) / 100.0 + "%";
	}
	
	private String moneyToText(long amount) {
		String money = "" + amount;
		String text = "";
		for(int i = 0; i < money.length(); i++) {
			text = money.substring(money.length() - i - 1, money.length() - i) + text;
			if(i != money.length() - 1 && (i + 1) % 3 == 0)
				text = ',' + text;
		}
		return "$" + text;
	}
	
	private String numberToText(long number) {
		String numberText = "" + number;
		String text = "";
		for(int i = 0; i < numberText.length(); i++) {
			text = numberText.substring(numberText.length() - i - 1,  numberText.length() - i) + text;
			if(i != numberText.length() - 1 && (i + 1) % 3 == 0)
				text = ',' + text;
		}
		return text;
	}
	
	private long exptolvl(long lvl) {
		if(lvl > 100000)
			lvl = 100000;
	    long exp = 0;
	    for(long i = 0; i < lvl - 1; i++)
	        exp += 800 * i + 600;
	    return exp;
	}
	private long lastObtainable(int carID) {
		
		//System.out.printf("lastObtainable(%d)%n", carId);
		
		switch(carID) {
		
		// Admin Exclusive
			case 9: case 104: case 170: case 172: case 175: case 179: case 180: return 1315483200;
		
		// 2012 XMaxx Event
		// 2013 XMaxx Event
			case 100: return 1386028800;
		// 2014 Winter Event
			// case 113: case 114:
			case 99: return 1420070400;
		// 2015 Xmaxx Event
			case 119: case 123: return 1451779200;
		// 2016 Xmaxx Event
			case 70: case 113: case 120: case 122: case 132: case 133: case 136: return 1483228800;
		// 2017 Xmaxx Event
			case 72: case 111: case 135: case 142: case 143: case 144: case 145: case 146: return 1514851200;
		// 2018 Xmaxx Event
			case 69: case 71: case 102: case 103: case 114: case 134: case 167: case 168: case 169: return 1546300800;
			
		// 2013 Summer Event
			case 84: case 85: case 87: case 88: return 1375833600;
		// 2014 Summer Event
			case 82: case 110: return 1408924800;
		// 2015 Summer Event
			case 109: return 1441065600;
		// 2016 Summer Event
			case 116: case 125: case 127: return 1471392000;
		// 2017 Summer Event
			case 126: case 138: case 139: case 140: return 1502496000;
		// 2018 Surf n' Turf Event
			case 81: case 83: case 115: case 128: case 156: case 157: case 158: case 159: case 160: return 1532044800;
			
		// 2013 Halloween Event
		// 2014 Halloween Event
			case 98: return 1414713600;
		// 2015 Halloween Event
			case 117: return 1446336000;
		// 2016 Hallowampus Event
		// 2017 Hallowampus Event
			case 118: case 130: case 131: case 141: return 1509494400;
			
		// Popularity Contest ended by 2.0 update
			case 50: case 57: return 1430179200;
			
		// 2018 Spring Fever Event
			case 149: case 150: case 151: case 152: return 1522540800;
			
		// 2018 PAC Event
			case 154: case 155: return 1526601600;
			
		// 2018 Back 2 School Event
			// case 161: case 162: case 163: case 165:
			case 164: return 1539129600;
		// 2019 Back 2 School Event
			case 161: case 162: case 163: case 165: case 184: case 185: case 186: case 187: case 188: return 1570172400;
			
		// Nitro transactions removed June 17th 2019
			case 21: return 1560776400;
			
		// Season 21 (ended July 14th 2019)
			case 177: case 178: return 1563062400;
			
		// season 22 (ended Aug 30th 2019)
			case 181: case 182: case 183: return 1567166400;
			
			default: return System.currentTimeMillis() / 1000;
			
		}
	}
	private long carValue(JSONArray carInfo, long carID) {
		
	//	System.out.printf("carValue(JSONArray, %d)%n", carID);
		
		for(int i = 0; i < carInfo.size(); i++)
			if(carID == (long) ((JSONObject) carInfo.get(i)).get("carID"))
				return (long) ((JSONObject) carInfo.get(i)).get("price");
		return 0;
	}
	private JSONArray getCarInfo() throws MalformedURLException {

		System.out.println("getCarInfo()");
		
		System.setProperty("http.agent", "Chrome");
	//	URL u = new URL("https://www.nitrotype.com/api/cars"); //MalformedURLException
		URL u = new URL("https://www.nitrotype.com/index/0/bootstrap.js");
		try {
			u.openConnection(); //IOException
			Scanner s = new Scanner(u.openStream()); //IOException
			//String carInfo = s.nextLine();
			
			String line = s.nextLine();
			while(!line.contains("CARS") || !s.hasNext())
				line = s.nextLine();
			//System.out.println(line);
			line = line.substring(line.indexOf("CARS") + 7);
			String carInfo = line.substring(0, line.indexOf("]") + 1);
			s.close();
			//return (JSONArray) ((JSONObject) new JSONParser().parse(carInfo)).get("data");
			return (JSONArray) new JSONParser().parse(carInfo);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (ParseException pe) {
			// TODO Auto-generated catch block
			pe.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * @param user
	 * @return
	 */
	private EmbedBuilder racer(String user) {
		// User can be an id, an @mention, a user name, or a link.
		// TODO look up registered
		
		// TODO
		return new EmbedBuilder().setDescription("Not implemented at this time.");
	}
	
	/**
	 * 
	 * @param discordId
	 * @param prefix
	 * @param initialEvent
	 * @param logEntry
	 * @param displayName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private EmbedBuilder verify(String discordId, String prefix, EmbedBuilder initialEvent, CompletableFuture<Message> logEntry, String displayName) {
		
		System.out.printf("verify(%s, %s, EmbedBuilder, CompletableFuture<Message>, %s)%n", discordId, prefix, displayName);
		
		JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
		for(int i = 0; i < register.size(); i++)
			if(((Long) ((JSONObject) register.get(i)).get("discordId")).longValue() == Long.parseLong(discordId))
				if(((Boolean) ((JSONObject) register.get(i)).get("verified")).booleanValue())
					return new EmbedBuilder().setDescription("<@" + discordId + "> is already verified.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
				else {
					String warning = "";
					//TODO replace this with register.json
					JSONArray verification = (JSONArray) Main.readJSON(Main.getResourcePath() + "Verification.json");
					for(int j = 0; j < verification.size(); j++)
						if(((Long) ((JSONObject) verification.get(j)).get("discordId")).longValue() == Long.parseLong(discordId))
							if(System.currentTimeMillis() - (Long) ((JSONObject) verification.get(j)).get("startTime") < 3600000) {
								try {
									JSONObject racerJSON = getRacerInfo("#" + (long) ((JSONObject) verification.get(j)).get("userId"));
									
									//System.out.println(((Long) racerJSON.get("carID")).longValue());
									//System.out.println(((Long) ((JSONObject) verification.get(j)).get("switchCar")).longValue());
									if(((Long) racerJSON.get("carID")).longValue() == ((Long) ((JSONObject) verification.get(j)).get("switchCar")).longValue()) {
										JSONObject entry = (JSONObject) register.get(i);
										entry.remove("verified");
										entry.put("verified", true);
										register.remove(i);
										register.add(entry);
										FileWriter file = new FileWriter(Main.getResourcePath() + "Register.json");
										file.write(register.toJSONString());
										file.close();
										verification.remove(j);
										file = new FileWriter(Main.getResourcePath() + "Verification.json");
										file.write(verification.toJSONString());
										file.close();
										return new EmbedBuilder().setDescription("Ownership of **" + (String) racerJSON.get("username") + "** has been verified to <@" + discordId + ">.\nUse the command `L.update` to update your roles.").setColor(Color.GREEN).setFooter("Report for " + displayName);
									} else return new EmbedBuilder().setDescription("The active vehicle does not match the designated vehicle, **" + (String) ((JSONObject) verification.get(j)).get("carName") + "**.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
								} catch (IOException e) {
									e.printStackTrace();
									Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(Thread.currentThread().getStackTrace()));
									return new EmbedBuilder().setDescription(Main.getError(6) + ".\nEither Nitro Type could not be connected to, or an error occured while reading or writing a file.\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + displayName);
								} catch (ParseException e) {
									e.printStackTrace();
									Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(Thread.currentThread().getStackTrace()));
									return new EmbedBuilder().setDescription(Main.getError(3) + ".\nAn error occurred while parsing information from Nitro Type.").setColor(Color.RED).setFooter("Error for " + displayName);
								}
							} else {
								verification.remove(j);
								warning = "Your previous verification request has expired. ";
							}
					try {
						//System.setProperty("http.agent", "Chrome");
						//URL u = new URL("https://www.nitrotype.com/racer/" + (String) ((JSONObject) register.get(i)).get("username")); //MalformedURLException
						//u.openConnection(); //IOException
						//Scanner s = new Scanner(u.openStream()); //IOException
						//String racerInfo = "";
						//while(s.hasNext())
						//	if((racerInfo = s.nextLine()).contains("RACER_INFO"))
						//		break;
						//s.close();
						JSONObject racerJSON = getRacerInfo("#" + (long) ((JSONObject) register.get(i)).get("userID"));
						List<JSONArray> garage = new ArrayList<JSONArray>();
						//JSONObject racerJSON = (JSONObject) new JSONParser().parse(racerInfo.substring(racerInfo.indexOf("RACER_INFO:") + 11, racerInfo.length() - 1)); //ParseException
						JSONArray carList = (JSONArray) racerJSON.get("cars");
						for(int j = 0; j < carList.size(); j++) {
							if(((String) ((JSONArray) carList.get(j)).get(1)).equals("owned"))
								garage.add((JSONArray) carList.get(j));
						}
						if(garage.size() < 5)
							return new EmbedBuilder().setDescription("**" + (String) ((JSONObject) register.get(i)).get("username") + "** has only " + garage.size() + " cars.\nPlease obtain a total of 5 cars.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
						long currentCar = (Long) racerJSON.get("carID");
						long switchCar = currentCar;
						while(currentCar == switchCar)
							switchCar = (Long) (garage.get((int) (Math.random() * garage.size()))).get(0);
					
						//u = new URL("https://www.nitrotype.com/index/624/bootstrap.js"); //MalformedURLException
						//u.openConnection(); //IOException
						//s = new Scanner(u.openStream()); //IOException
						//String carInfo = "";
						//while(s.hasNext())
						//	if((carInfo = s.nextLine()).contains("'CARS'"))
						//		break;
						//s.close();
					
						//JSONArray carJSON = (JSONArray) new JSONParser().parse(carInfo.substring(carInfo.indexOf("'CARS'") + 7, carInfo.indexOf("'CARS'") + 7 + carInfo.substring(carInfo.indexOf("'CARS'") + 7).indexOf(";") - 2)); //ParseException
						JSONArray carJSON = getCarInfo();
						String switchName = null;
						for(int j = 0; j < carJSON.size(); j++)
							if((Long) ((JSONObject) carJSON.get(j)).get("carID") == switchCar)
								switchName = (String) ((JSONObject) carJSON.get(j)).get("name");
						JSONObject entry = new JSONObject();
						entry.put("discordId", Long.parseLong(discordId));
						entry.put("username", (String) ((JSONObject) register.get(i)).get("username"));
						entry.put("startTime", System.currentTimeMillis());
						entry.put("switchCar", switchCar);
						entry.put("carName", switchName);
						entry.put("userId", ((Long) racerJSON.get("userID")).longValue());
						
						verification.add(entry);
						FileWriter file = new FileWriter(Main.getResourcePath() + "Verification.json");
						file.write(verification.toJSONString());
						file.close();
						return new EmbedBuilder().setDescription(warning + "To verify ownership of the account **" + (String) ((JSONObject) register.get(i)).get("username") + "**, switch your active vehicle to **" + switchName + "** and use the command `" /*+ prefix*/ + "L." + "verify`.").setColor(Color.GREEN).setFooter("Report for " + displayName);
					} catch(IOException e) {
						e.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 6, Arrays.toString(Thread.currentThread().getStackTrace()));
						return new EmbedBuilder().setDescription(Main.getError(6) + ".\nEither Nitro Type could not be connected to, or an error occured while reading or writing a file.\nPlease contact " + Main.getDeveloperID() + " for assistance.").setColor(Color.RED).setFooter("Error for " + displayName);
					} catch(StringIndexOutOfBoundsException | ParseException e) {
						e.printStackTrace();
						Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(Thread.currentThread().getStackTrace()));
						return new EmbedBuilder().setDescription(Main.getError(3) + ".\nAn error occurred while parsing information from Nitro Type.").setColor(Color.RED).setFooter("Error for " + displayName);
					}
				}
		return new EmbedBuilder().setDescription("An associated account could not be found for <@" + discordId + ">.\nPlease register a Nitro Type account before using this command.");
	}
	
	/**
	 * 	
	 * @param channel
	 * @param message
	 * @param initialEvent
	 * @param logEntry
	 * @param displayName
	 * @return
	 */
	private EmbedBuilder puppet(String channel, String message, EmbedBuilder initialEvent, CompletableFuture<Message> logEntry, String displayName) {
		
		System.out.printf("puppet(%s, %s, EmbedBuilder, CompletableFuture<Message>, %s)%n", channel, message, displayName);
		
		Optional<Channel> target;
		if(isLong(channel))
			target = Main.getApi().getChannelById(channel);
		else if(channel.startsWith("<#") && channel.endsWith(">"))
			target = Main.getApi().getChannelById(channel.substring(2, channel.length() - 1));
		//failure
		else {
			Main.errorLog(initialEvent, logEntry, 3, Arrays.toString(Thread.currentThread().getStackTrace()));
			return new EmbedBuilder().setDescription(Main.getError(3) + ".\nPlease provide a valid channel.").setColor(Color.RED).setFooter("Error for " + displayName);
		}
		try {
			target.get().asTextChannel().get().sendMessage(message);
		} catch (Exception e) {
			e.printStackTrace();
			Main.errorLog(initialEvent, logEntry, 4, Arrays.toString(Thread.currentThread().getStackTrace()));
			return new EmbedBuilder().setDescription(Main.getError(4) + ".\nThe message failed to send.").setColor(Color.RED).setFooter("Error for " + displayName);
		}
		//success
		System.out.println("Message sent.");
		return new EmbedBuilder().setDescription("Message sent.").setColor(new Color(138, 43, 226)).setFooter("Report for " + displayName);
		
	}	
	
	@SuppressWarnings("unchecked")
	private EmbedBuilder setChannelPrefix(String channel, String prefix, MessageAuthor author, String displayName) throws IOException, NumberFormatException {
		
		System.out.printf("setChannelPrefix(%s, %s, %s, %s)%n", channel, prefix, author.getName(), displayName);
		
		if(channel.startsWith("<#") && channel.endsWith(">"))
			channel = channel.substring(2, channel.indexOf(">"));
		if(!isLong(channel))
			throw new NumberFormatException();
		if(Main.getApi().getChannelById(channel).isPresent() && Main.getApi().getChannelById(channel).get().asTextChannel().isPresent())
			if(!isAuthorized(Main.getApi().getChannelById(channel).get().asTextChannel().get(), author))
				return new EmbedBuilder().setDescription("<@" + author.getId() + "> is not authorized to set the prefix for <#" + channel + ">.").setColor(Color.YELLOW).setFooter("Warning for " + displayName);
		JSONArray channels = (JSONArray) Main.readJSON(Main.getResourcePath() + "Channels.json");
		for(int i = 0; i < channels.size(); i++)
			if(((Long) ((JSONObject) channels.get(i)).get("channelId")).longValue() == Long.parseLong(channel)) {
				channels.remove(i);
				JSONObject entry = new JSONObject();
				entry.put("channelId", Long.parseLong(channel));
				entry.put("prefix", prefix);
				entry.put("setBy", author.getId());
				channels.add(entry);
				FileWriter file = new FileWriter(Main.getResourcePath() + "Channels.json");
				file.write(channels.toJSONString());
				file.close();
				return new EmbedBuilder().setDescription("The prefix for <#" + channel + "> has been set to **" + prefix +"**.").setColor(Color.GREEN).setFooter("Report for " + displayName);
			}
		JSONObject entry = new JSONObject();
		entry.put("channelId", Long.parseLong(channel));
		entry.put("prefix", prefix);
		entry.put("setBy", author.getId());
		channels.add(entry);
		FileWriter file = new FileWriter(Main.getResourcePath() + "Channels.json");
		file.write(channels.toJSONString());
		file.close();
		return new EmbedBuilder().setDescription("The prefix for <#" + channel + "> has been set to **" + prefix +"**.").setColor(Color.GREEN).setFooter("Report for " + displayName);
	}
	private String getChannelPrefix(TextChannel channel) {
		
		//System.out.printf("getChannelPrefix(%s)%n", channel.toString());
		
		String prefix = "l.";
		JSONArray channels = (JSONArray) Main.readJSON(Main.getResourcePath() + "Channels.json");
		for(int i = 0; i < channels.size(); i++)
			if(((Long) ((JSONObject) channels.get(i)).get("channelId")).longValue() == channel.getId())
				prefix = (String) ((JSONObject) channels.get(i)).get("prefix");
		return prefix;
	}
	
	private boolean isLong(String string) {
		try {
			Long.parseLong(string);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}
	
	private boolean isAuthorized(TextChannel channel, MessageAuthor user) {
		
		System.out.printf("isAuthorized(%s, %s)%n", channel.toString(), user.getName());
		
		if(channel.asServerChannel().isPresent() && user.asUser().isPresent()) {
			Server server = channel.asServerChannel().get().getServer();
			
			if(server.getId() == NITRO_TYPE_SERVER) // Nitro Type server
				return server.canManageRoles(user.asUser().get())
						|| user.asUser().get().getRoles(server).contains(server.getRoleById(566369686967812112L).get()); // server support
			
			if(server.getId() == NT_OLYMPICS_SERVER) // NT Olympics
				return server.canManageRoles(user.asUser().get());
		}
		
		return false;
		
	}
	
	/**
	 * 
	 * @param prefix
	 * @param content
	 * @return
	 */
	private String[] parseCommand(String prefix, String content) {
		
		System.out.printf("parseCommand(%s, %s)%n", prefix, content);
		
		//Trims the prefix off the front
		if(content.length() > prefix.length())
			content = content.substring(prefix.length());
		else return new String[0]; //returns an empty string if there's nothing after the prefix
		for(int i = 0; i < content.length(); i++) {
			//if the character at the current index is a space
			if(content.charAt(i) == ' ') 
				//If it's the last character, trim the space off
				if(i == content.length() - 1)
					content = content.substring(0, i);
				//else if the next character is also a space, trim the current character out and adjust the index
				else if(content.charAt(i + 1) == ' ') {
					content = content.substring(0, i) + content.substring(i + 1);
					i--;
				}
		}
		int size = content.length() + 1 - content.replaceAll(" ", "").length();
		if(size > 3) size = 3;
		String[] arguments = new String[size];
		for(int i = 0; i < arguments.length; i++) {
			if(i < 2 && content.contains(" ")) {
				arguments[i] = content.substring(0, content.indexOf(' '));
				content = content.substring(content.indexOf(' ') + 1);
			} else {
				arguments[i] = content;
				break;
			}
		}
		return arguments;
	}
	
	/**
	 * 
	 * @param arguments
	 * @param command
	 * @param prefix
	 * @param e
	 * @return
	 */
	private String checkArguments(String[] arguments, String[] command, String prefix, MessageCreateEvent e) {
		
		System.out.printf("checkArguments(arguments, command, %s, message)%n", prefix);
		
		if(command[2] == null); //command has no arguments c _ _
		else if(command[2].startsWith("r")) { //command has 1 required argument c r ?
			if(command[3] == null) { //and has no second argument c r _
				if(arguments.length != 2) // Error 1 Invalid arguments
					return Main.getError(1) + ".\nYour command should match the format:\n`" + prefix + command[0] + " " + command[2].substring(1) + "`";
			} else if(command[3].startsWith("r")) {//c r r
				if(arguments.length != 3)
					return Main.getError(1) + ".\nYour command should match the format:\n`" + prefix + command[0] + " " + command[2].substring(1) + " " + command[3].substring(1) + "`";
			} else if(command[3].startsWith("o")) {//optional second argument c r o
				if(arguments.length != 2 && arguments.length != 3)
					return Main.getError(1) + ".\nYour command should match one of the formats:\n`"
							+ prefix + command[0] + " " + command[2].substring(1) + "`\n`"
							+ prefix + command[0] + " " + command[2].substring(1) + " " + command[3].substring(1) + "`";
			}
		} else if(command[2].startsWith("o")) { //command has 1 optional argument
			if(command[3] == null) { //no second argument
				if(arguments.length != 1 && arguments.length != 2)
					return Main.getError(1) + ".\nYour command should match one of the formats:\n`"
							+ prefix + command[0] + "`\n`"
							+ prefix + command[0] + " " + command[2].substring(1) + "`";
			} else if(command[3].startsWith("p")) //second argument that requires a previous argument be present
				if(arguments.length != 1 && arguments.length != 2 && arguments.length != 3) 
					return Main.getError(1) + ".\nYour command should match one of the formats:\n`"
							+ prefix + command[0] + "`\n`"
							+ prefix + command[0] + " " + command[2].substring(1) + "`\n`"
							+ prefix + command[0] + " " + command[2].substring(1) + " " + command[3].substring(1) + "`";
		}
		return null;
	}
private JSONObject getRacerInfo(String input) throws IOException, ParseException, StringIndexOutOfBoundsException {
	
	System.out.printf("getRacerInfo(%s)%n", input);
	
	if(input.startsWith("<@") && input.endsWith(">")) {
		if(input.startsWith("<@!"))
			input = input.substring(3, input.indexOf(">"));
		else input = input.substring(2, input.indexOf(">"));
	}
	if(isLong(input)) {
		JSONArray register = (JSONArray) Main.readJSON(Main.getResourcePath() + "Register.json");
		for(int i = 0; i < register.size(); i++)
			if(((Long) ((JSONObject) register.get(i)).get("discordId")).longValue() == Long.parseLong(input)) {
				// TODO use ID over username if possible
				input = (String) ((JSONObject) register.get(i)).get("username");
				break;
			}
	}
	if(input.contains("nitrotype.com/racer/"))
		input = input.substring(input.indexOf("nitrotype.com/racer/") + 20);
	
	boolean api = false;
	if(input.startsWith("#") && isLong(input.substring(1))) 
		api = true;
	
	JSONObject racerJSON;
	if(!api) {
		System.setProperty("http.agent", "Chrome");
		URL u = new URL("https://www.nitrotype.com/racer/" + input); //MalformedURLException
		u.openConnection(); //IOException
		Scanner s = new Scanner(u.openStream()); //IOException
		String racerInfo = "";
		while(s.hasNext())
			if((racerInfo = s.nextLine()).contains("RACER_INFO"))
				break;
		s.close();
		racerJSON = (JSONObject) new JSONParser().parse(racerInfo.substring(racerInfo.indexOf("RACER_INFO:") + 11, racerInfo.length() - 1)); //ParseException
	} else {
		System.setProperty("http.agent", "Chrome");
		URL u = new URL("https://test.nitrotype.com/api/players/" + input.substring(1));
		u.openConnection();
		Scanner s = new Scanner(u.openStream());
		String racerInfo = "";
		racerInfo = s.nextLine();
		s.close();
		racerJSON = (JSONObject) ((JSONObject) new JSONParser().parse(racerInfo)).get("data");
	}
	book(racerJSON);
	return racerJSON;
}
	@SuppressWarnings("unchecked")
	private void book(JSONObject racerJSON) {
		JSONObject book = (JSONObject) Main.readJSON(Main.getResourcePath() + "Book.json");
		try {
			book.put("" + (long) racerJSON.get("userID"), racerJSON);
			//new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)
			OutputStreamWriter file = new OutputStreamWriter(new FileOutputStream(Main.getResourcePath() + "Book.json"), StandardCharsets.UTF_8);
			file.write(book.toJSONString());
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	private JSONObject getTeamInfo(String tag) throws IOException, ParseException {
		
		System.out.printf("getTeamInfo(%s)%n", tag);
		
		if(tag.length() < 2 || tag.length() > 6)
			return null;
		System.setProperty("http.agent", "Chrome");
		URL u = new URL("https://www.nitrotype.com/api/teams/" + tag);
		u.openConnection();
		Scanner s = new Scanner(u.openStream());
		String teamInfo = "";
		teamInfo = s.nextLine();
		s.close();
		return (JSONObject) ((JSONObject) new JSONParser().parse(teamInfo)).get("data");
	}
	
	private void roleSwitch(boolean condition, Role role, List<Role> userRoles, User user) {
		
		System.out.printf("roleSwitch(%b, %s, List<Role>, %s)%n", condition, role.getName(), user.getName());
		
		if(condition) {
			if(!userRoles.contains(role))
				user.addRole(role);
		} else if(userRoles.contains(role))
			user.removeRole(role);
	}
	
	/**
	 * Search for players that match the given name.
	 * 
	 * @param name
	 * @return Search results
	 * @throws IOException
	 * @throws ParseException
	 */
	
	private JSONArray searchPlayers(String name) throws IOException, ParseException {
		
		Main.getLogin(false);
		
		System.setProperty("http.agent", "Chrome");
		URL url = new URL("https://www.nitrotype.com/api/players-search");
		
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setDoOutput(true);
		http.setRequestMethod("POST");
		
		http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		DataOutputStream wr = new DataOutputStream(http.getOutputStream());
		wr.write(("term=" + name + "&uhash=" + Main.getUhash()).getBytes(StandardCharsets.UTF_8));
		wr.flush();
		wr.close();

		int responseCode = http.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : term=" + name);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			System.out.println(inputLine);
			response.append(inputLine);
		}
		in.close();
		
		if(response.toString() == null)
			System.out.println("Response was null");
		else { 
			System.out.println(response.toString().length());
			System.out.println(response.toString());
		}
		JSONObject result = (JSONObject) new JSONParser().parse(response.toString());
		System.out.println(result.toJSONString());
		return (JSONArray) result.get("data");
	}
	
}
