package com.github.nes370.lolydealer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.Invite;
import org.javacord.api.entity.server.invite.InviteBuilder;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Overwatch implements ServerMemberJoinListener {
	
	public Server server;
	public TextChannel logChannel;
	public ServerChannel inviteChannel;
	public JSONObject inviteLinks;
	public long lastJoinStamp, lastID;
	
	@SuppressWarnings("unchecked")
	public Overwatch(Optional<Server> server, Optional<Channel> channel, Optional<Channel> inviteChannel) {
		
		this.server = server.get();
		this.logChannel = channel.get().asTextChannel().get();
		this.inviteChannel = inviteChannel.get().asServerChannel().get();
		this.lastID = 0;
		inviteLinks = new JSONObject();
		
		try {
			Collection<RichInvite> e = this.server.getInvites().get();
			RichInvite[] invites = e.toArray(new RichInvite[e.size()]);
			for(int i = 0; i < invites.length; i++) {
				JSONObject invite = new JSONObject();
				//invite.put("code", invites[i].getCode());
				if(invites[i].getInviter() != null) {
					invite.put("inviterId", "<@" + invites[i].getInviter().getId() + ">");
					invite.put("inviter", invites[i].getInviter().getDisplayName(this.server));
				} else {
					invite.put("inviterId", "Unknown");
					invite.put("inviter", "Unknown");
				}
				invite.put("uses", invites[i].getUses());
				invite.put("creationTimestamp", invites[i].getCreationTimestamp());
				invite.put("channelId", invites[i].getChannelId());
				invite.put("channel", invites[i].getChannelName());
				invite.put("maxAge", invites[i].getMaxAgeInSeconds());
				invite.put("maxUses", invites[i].getMaxUses());
				inviteLinks.put(invites[i].getCode(), invite);
				
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	@SuppressWarnings({ "unchecked", "null" })
	@Override
	public void onServerMemberJoin(ServerMemberJoinEvent event) {
		// TODO Auto-generated method stub
		String suspect = "";
		String name = event.getUser().getName();
		EmbedBuilder logMessage = new EmbedBuilder().setFooter("Member joined").setColor(Color.GREEN);
		
		@SuppressWarnings("unused")
		boolean notImmune = false; //joined using a temp invite link
		boolean wasKicked = false;
		boolean wasBanned = false;
		boolean found = false;
		Invite temp = null;
		double nsfw_score = 0.0;
		if(server.getId() == event.getServer().getId()) {
			String report = "<@" + event.getUser().getId() + ">, " + event.getUser().getDiscriminatedName() + ", joined at " + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault())) + " PST";
			try {
				Collection<RichInvite> e = event.getServer().getInvites().get();
				RichInvite[] invites = e.toArray(new RichInvite[e.size()]);
				for(int i = 0; i < invites.length; i++) {
					if(inviteLinks.get(invites[i].getCode()) != null)  {
						if((int) ((JSONObject) inviteLinks.get(invites[i].getCode())).get("uses") < invites[i].getUses()) {
							if(invites[i].getMaxAgeInSeconds() == 0 || invites[i].getMaxAgeInSeconds() > 86399)//invites[i].getCode().equals("gbh3DDA") || invites[i].getCode().equals("KNWzUPn"))
								notImmune = true;
							((JSONObject) inviteLinks.get(invites[i].getCode())).put("uses", invites[i].getUses());
							report += " using the invite code " + invites[i].getCode() + " created by " + (String) ((JSONObject) inviteLinks.get(invites[i].getCode())).get("inviterId") + " at " + Main.dtf.format(LocalDateTime.ofInstant(invites[i].getCreationTimestamp(), ZoneId.systemDefault())) + " PST usage #" + invites[i].getUses();
							found = true;
						}
					} else {
						JSONObject invite = new JSONObject();
						//invite.put("code", invites[i].getCode());
						if(invites[i].getInviter() != null) {
							invite.put("inviterId", "<@" + invites[i].getInviter().getId() + ">");
							invite.put("inviter", invites[i].getInviter().getDisplayName(server));
						} else {
							invite.put("inviterId", "Unknown");
							invite.put("inviter", "Unknown");
						}
						invite.put("uses", invites[i].getUses());
						invite.put("creationTimestamp", invites[i].getCreationTimestamp());
						invite.put("channelId", invites[i].getChannelId());
						invite.put("channel", invites[i].getChannelName());
						invite.put("maxAge", invites[i].getMaxAgeInSeconds());
						invite.put("maxUses", invites[i].getMaxUses());
						inviteLinks.put(invites[i].getCode(), invite);
						if(invites[i].getUses() > 0) {
							if(invites[i].getMaxAgeInSeconds() == 0 || invites[i].getMaxAgeInSeconds() > 86399)//invites[i].getCode().equals("gbh3DDA") || invites[i].getCode().equals("KNWzUPn"))
								notImmune = true;
							report += " using a new invite code " + invites[i].getCode() + " created by " + (String) ((JSONObject) inviteLinks.get(invites[i].getCode())).get("inviterId") + " at " + Main.dtf.format(LocalDateTime.ofInstant(invites[i].getCreationTimestamp(), ZoneId.systemDefault())) + " PST";
							found = true;
						} else {
							String linkReport = "Invite link " + invites[i].getCode() + " created by " + (String) ((JSONObject) inviteLinks.get(invites[i].getCode())).get("inviterId") + " at " + Main.dtf.format(LocalDateTime.ofInstant(invites[i].getCreationTimestamp(), ZoneId.systemDefault())) + " PST detected.";
							if(invites[i].getMaxUses() != 0)
								 linkReport += " It has " + (invites[i].getMaxUses() - invites[i].getUses()) + " uses remaining.";
							if(invites[i].getMaxAgeInSeconds() > 1)
								linkReport += " It will expire at " + Main.dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(invites[i].getCreationTimestamp().toEpochMilli() + invites[i].getMaxAgeInSeconds() * 1000), ZoneId.systemDefault())) + " PST."; 
							logChannel.sendMessage(new EmbedBuilder().setFooter("New Invite Link").setDescription(linkReport).setColor(new Color(148, 0, 211)));
							System.out.println(linkReport);
						}
					}
				}
				if(!found)
					report += " using a one-use or now expired invite link";
				report += ".";
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(System.currentTimeMillis() - event.getUser().getCreationTimestamp().toEpochMilli() < 3600000) {
				suspect += "Suspected guest account: Account was created " + (System.currentTimeMillis() - event.getUser().getCreationTimestamp().toEpochMilli()) / 60000 + " minutes ago.\n";
				logMessage.setColor(Color.YELLOW);
				if(System.currentTimeMillis() - event.getUser().getCreationTimestamp().toEpochMilli() < 300000)
					event.getUser().sendMessage("Hello. It looks like you are using Discord with a guest account. Don't forget to set it up with an email address and password so you won't have to register again next time you stop by the Nitro Type server.");
			}
			
			if(name.toLowerCase().contains("nigger") || name.toLowerCase().contains("fuck")) {
				suspect += "Suspected troll: Name contains offensive language.\n";
			}
			if(name.toLowerCase().contains("ban")) {
				suspect += "Suspected ban evasion: name mentions ban.\n";
			}
			if(name.toLowerCase().contains("kick") || name.toLowerCase().contains("mute")) {
				suspect += "Suspected evasion: Name mentions kick or mute.\n";
			}
			if(name.toLowerCase().contains(".tv") || name.toLowerCase().contains(".ly") || name.toLowerCase().contains(".be") || name.toLowerCase().contains(".com") || name.toLowerCase().contains(".gg") || name.toLowerCase().contains(".net")) {
				suspect += "Suspected link advertising: Name contains .com, .be, .tv, .ly, .gg or .net.\n";
			}
			if(event.getUser().getId() == lastID
					|| event.getUser().getId() == 260964625724604417L //DarkMagicianGirl9900
					|| event.getUser().getId() == 560528911176564746L //Asssa123
					) {
				notImmune = false;
			} else if(System.currentTimeMillis() - lastJoinStamp > 30000) {
				lastID = event.getUser().getId();
				lastJoinStamp = System.currentTimeMillis();
			} else {
				notImmune = true;
				lastID = event.getUser().getId();
				lastJoinStamp = System.currentTimeMillis();
			}
			// bot names
			
			if(!event.getUser().hasDefaultAvatar()) {
				URL url;
				try {
					url = new URL("https://api.deepai.org/api/nsfw-detector");
					HttpURLConnection c = (HttpURLConnection) url.openConnection();
					c.setRequestMethod("POST");
					c.setRequestProperty("Api-Key", Main.getDeepAIKey()); 
					Map<String, String> parameters = new HashMap<>();
					parameters.put("image", event.getUser().getAvatar().getUrl().toString());
					c.setDoOutput(true);
					DataOutputStream out = new DataOutputStream(c.getOutputStream());
					out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
					out.flush();
					out.close();
					BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
					String inputLine;
					StringBuffer content = new StringBuffer();
					while((inputLine = in.readLine()) != null) {
						content.append(inputLine);
					}
					in.close();
					c.disconnect();
					System.out.println(content.toString());
					nsfw_score = (double) ((JSONObject) ((JSONObject) new JSONParser().parse(content.toString())).get("output")).get("nsfw_score");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Failed attempt to check for nudity.");
					e.printStackTrace();
				}
			}	
			
			if(nsfw_score > 0.90) {
				try {
					event.getUser().sendMessage("You were banned from the server for the following reason: Innapropriate avatar.").get();
					server.banUser(event.getUser(), 0, "Over 90% chance of NSFW avatar.").get();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logMessage.addField("Inappropriate Avatar", "User avatar is " + ((int) (nsfw_score * 100000)) / 1000.0  + "% likely to contain nudity.");
				wasBanned = true;
			} else if(nsfw_score > 0.50) {
				try {
					System.out.println("Suspected NSFW");
					if(notImmune) {
						temp = new InviteBuilder(inviteChannel).setMaxAgeInSeconds(3600).setMaxUses(1).create().get();
						System.out.println("Invite primed");
						event.getUser().sendMessage("You were kicked from the server for the following reason: Innapropriate avatar. If you believe this was in error, please use the following invite link to rejoin our server within the next hour.\nhttps://discord.gg/" + temp.getCode()).get();
						System.out.println("User DMed");
						server.kickUser(event.getUser(), "Suspected bot").get();
						System.out.println("User kicked");
						wasKicked = true;
						System.out.println("wasKicked set true");
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				logMessage.addField("Inappropriate Avatar", "User avatar is " + ((int) (nsfw_score * 100000)) / 1000.0  + "% likely to contain nudity.").setColor(Color.YELLOW);
			} else logMessage.addField("Appropriate Avatar", "User avatar is " + ((int) (nsfw_score * 100000)) / 1000.0 + "% likely to contain nudity.");
			
			//TODO bot names
			/*
			if(name.length() > 4) try {
				Integer.parseInt(name.substring(name.length() - 3));
				suspect += "Suspected bot: Name contains numerical digits consistent with a known bot naming scheme.\n";
				System.out.println("Suspected bot");
				if(!wasKicked && !wasBanned && notImmune) {
					temp = new InviteBuilder(inviteChannel).setMaxAgeInSeconds(3600).setMaxUses(1).create().get();
					System.out.println("Invite primed");
					event.getUser().sendMessage("You were kicked from the server for the following reason: Suspected bot. If you believe this was in error, please use the following invite link to rejoin our server within the next hour.\nhttps://discord.gg/" + temp.getCode()).get();
					System.out.println("User DMed");
					server.kickUser(event.getUser(), "Suspected bot").get();
					System.out.println("User kicked");
					wasKicked = true;
					System.out.println("wasKicked set true.");
				}
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
			} 
			*/
			logMessage.setDescription(report);
			if(!suspect.equals(""))
				logMessage.addField("Suspicious user", suspect).setColor(Color.YELLOW);
			
			if(wasBanned)
				logMessage.addField("User was banned", "<:akkoaaaaa:575423920266674176> Over 90% chance of NSFW avatar.");
			
			if(wasKicked)
				logMessage.addField("User was kicked", "Temporary invite code " + temp.getCode() + " was sent via DM.").setColor(Color.RED);
				
			logChannel.sendMessage(logMessage);
			System.out.println(report);
			
			if((!wasBanned || !wasKicked) && server.getId() == 564880536401870858L)
				server.getTextChannelById(564881373039689735L).get().sendMessage(new EmbedBuilder().setDescription("Type the command `L.register [username]` to begin the registration process.\nIf you have a previously verified registration, use the command `L.update`.").setColor(Color.GREEN));
		}
	}	
}