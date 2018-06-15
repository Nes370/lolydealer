package com.github.nes370.lolydealer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class Commands implements MessageCreateListener {
	//When a message is created
	@Override
	public void onMessageCreate(MessageCreateEvent e) {
		//If this bot is the author, ignore it.
		if(e.getMessage().getAuthor().isYourself()) {
			return;
		}
		//If the message starts with an "l."
		if(e.getMessage().getContent().toLowerCase().startsWith("l.")) {
			if(!(e.getMessage().getContent().length() > 2)) {
				//Send a message to the requester stating the command was not recognized
				e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Empty command.\nUse `l.help` for a list of commands."));
				//Print a log to the console declaring the command error
				System.out.println("Empty command error announced.");
				return;
			}
			//And if the character after "l." isn't a space
			if(!Character.toString(e.getMessage().getContent().charAt(2)).equals(" ")) {	
				//Check if the message contains a command
				boolean found = false;
				byte loc = 0;
				for(int i = 0; i < Main.commands.length; i++) {
					//if the message is big enough to contain the command
					if(e.getMessage().getContent().length() >= Main.commands[i][0].length() 
							//And the initial string of characters matches a command
							&& Main.commands[i][0].equalsIgnoreCase(e.getMessage().getContent().substring(0, Main.commands[i][0].length()))) {
						if(e.getMessage().getContent().length() > Main.commands[i][0].length()
								&& e.getMessage().getContent().charAt(Main.commands[i][0].length()) != ' '); 
						else {
							found = true;
							loc = (byte)i;
							Main.addCounter(i);
							break;
						}
					}
				}
				//Get the current date and time
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
				LocalDateTime now = LocalDateTime.now();
				//Print a log to the console that includes who sent what command in which channel of which server at what date and time.
				System.out.println(e.getMessage().getAuthor() + " used \"" + e.getMessage().getContent() + "\" in " + e.getMessage().getChannel() + " of "
						+ e.getMessage().getServer() + " at " + dtf.format(now));
				//If a command was found in the message
				if(found) {
					//And the command was help
					if(Main.commands[loc][0].equalsIgnoreCase("l.help")) {
						//Organize a list of the current commands
						EmbedBuilder embed = new EmbedBuilder().setTitle("List of Commands")
								.setAuthor("Loly Dealer", null, "https://cdn.discordapp.com/avatars/455132593642536983/6bb82ec527d846631f9b511ec510bc4b.png")
								.setColor(Color.PINK).setFooter("Developed by Nes370")
								.setThumbnail(new File("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Loly Dealer.jpg"));
						for(int i = 0; i < Main.commands.length; i++)
							embed.addField(Main.commands[i][0], Main.commands[i][1]);
						//And send a message to the requester with that list. 
						e.getMessage().getChannel().sendMessage(embed);
						//Print a log to the console to declare the command's request.
						System.out.println("Help message printed.");
					}
					//Else if the command was value
					else if(Main.commands[loc][0].equalsIgnoreCase("l.value")) {
						//And the message only included "l.value"
						String arg = "";
						if(e.getMessage().getContent().length() < 9) {
							if(!e.getMessage().getContent().equalsIgnoreCase("l.value")) {
								//Send a message to the requester stating the command was not recognized
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Unknown command.\nUse `l.help` for a list of commands."));
								//Print a log to the console declaring the command error
								System.out.println("Command error announced.");
								return;
							}
							//If the requester is registered to a NT account
							if(isRegistered(e.getMessage().getAuthor().getId() + "")) {
								//Set the [username] value equal to their registered NT account
								arg = linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
										(e.getMessage().getAuthor() + "").indexOf(",")));
								//Print a log to the console declaring the change in the command arguments
								System.out.println("Adjusted to \"l.value " + arg + "\"");
							//If the requester is not registered
							} else {
								//Send a message to the requester that asks them to register to their NT account
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A registered account was not found.\nPlease register with `l.register [username]`"));
								//Print a log to the console to declare the lack of a registered account
								System.out.println("Account not found");
								//And end the message reaction
								return;
							}
						//Else if the message includes an @mention
						} else if(e.getMessage().getContent().contains("<@") && e.getMessage().getContent().contains(">"))	{
							//If the @mention includes a "!"
							if(e.getMessage().getContent().contains("<@!")) {
								//And the Discord ID in the @mention is registered to a NT account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
										e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
												e.getMessage().getContent().indexOf(">"))) 
												+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									//Else the command only contains the @mention
									} else {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3,
												e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								} else { e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A linked Nitro Type account could not be found for the Discord user " 
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"),
										e.getMessage().getContent().indexOf(">") + 1) + "."));
									return;
								}
							//And if it doesn't include a "!"
							} else {
								//And the Discord ID in the @mention is registered to a NT account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
										e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
												e.getMessage().getContent().indexOf(">"))) 
												+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									} else {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
												e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								} else { e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A linked Nitro Type account could not be found for the Discord user " 
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), 
										e.getMessage().getContent().indexOf(">") + 1) + "."));
									return;
								}
							}
						//Else set the account argument to what is written in plain text
						} else {
							arg = e.getMessage().getContent().substring(8);
						//Attempt to evaluate the account specified by arg
						} try {
							e.getMessage().getChannel().sendMessage(Evaluate.main(arg).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare a successful evaluation
							System.out.println("Account evaluated.");
						//If an error is caught
						} catch (Exception e1) {
							//Print a log to the console that shows where in the code the error occurred
							e1.printStackTrace();
							//Send a message to the requester declaring the error
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
									"An error occurred.\nUse `l.help` for a list of commands."));
							//Print a log to the console to declare the requester was notified of the error
							System.out.println("Runtime error announced.");
						}
					}
					//Else if the command is cars
					else if(Main.commands[loc][0].equalsIgnoreCase("l.cars")) {
						//And the message contains only "l.cars"
						String arg = "";
						if(e.getMessage().getContent().length() < 8) {
							if(!e.getMessage().getContent().equalsIgnoreCase("l.cars")) {
								//Send a message to the requester stating the command was not recognized
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Unknown command.\nUse `l.help` for a list of commands."));
								//Print a log to the console declaring the command error
								System.out.println("Command error announced.");
								return;
							}
							//If the author is registered
							if(isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
									(e.getMessage().getAuthor() + "").indexOf(",")))) {//If the author is registered
								//Set the account argument to the corresponding linked account
								arg = linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
										(e.getMessage().getAuthor() + "").indexOf(","))); //Set the string argument to the linked account
								//Print a log to the console declaring the change in command arguments
								System.out.println("Adjusted to \"l.cars " + arg + "\"");
							//If the author is not registered
							} else {
								//Send the requester a message to register their NT account
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A registered account was not found.\nPlease register with `l.register [username]`"));
								//Print a log to the console to declare the lack of a registered account
								System.out.println("Account not found");
								//End the message reaction
								return;
							}
						//Else if the command contains an @mention
						} else if(e.getMessage().getContent().contains("<@") && e.getMessage().getContent().contains(">")) {
							//If the @mention contains an exclamation point
							if(e.getMessage().getContent().contains("<@!")) {
								//if the enclosed Discord ID is registered to an account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
										e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
												e.getMessage().getContent().indexOf(">"))) 
												+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments 
										System.out.println("Adjusted to \"l.cars " + arg + "\"");
									//Else the command doesn't contain more text
									} else {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
												e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments 
										System.out.println("Adjusted to \"l.cars " + arg + "\"");
									}
								} else { e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A linked Nitro Type account could not be found for the Discord user " 
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), 
										e.getMessage().getContent().indexOf(">") + 1) + "."));
									return;
								}
							//Else if it does not contain an exclamation point
							} else {
								//If the enclosed Discord ID is registered to an account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
										e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
												e.getMessage().getContent().indexOf(">"))) 
												+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.cars " + arg + "\"");
									//Else the command doesn't contain more text
									} else {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, 
												e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.cars " + arg + "\"");
									}
								} else { e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"A linked Nitro Type account could not be found for the Discord user " 
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), 
										e.getMessage().getContent().indexOf(">") + 1) + "."));
									return;
								}
							}
						//Else set the account argument to what is written in plain text
						} else arg = e.getMessage().getContent().substring(7);
						//Attempt to rank the cars of the account specified by arg
						try {
							e.getMessage().getChannel().sendMessage(RankCars.main(arg).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare successful ranking
							System.out.println("Cars ranked.");
						//If an error is caught
						} catch (Exception e1) {
							//Print a log to the console that shows where in the code the error occurred
							e1.printStackTrace();
							//Send a message to the requester declaring the error
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
									"An error occurred.\nUse `l.help` for a list of commands."));
							//Print a log to the console to declare the requester was notified of the error
							System.out.println("Runtime error announced.");
						}
					}
					//Else if the command is register
					else if(Main.commands[loc][0].equalsIgnoreCase("l.register")) {
						//If the author is already registered
						if(isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
								(e.getMessage().getAuthor() + "").indexOf(","))))
							//Send a message to the requester to tell them they are already registered
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
									"A previous registration to the Nitro Type account **" 
									+ linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
									(e.getMessage().getAuthor() + "").indexOf(",")))) 
									+ "** already exists.\nUse `l.unregister` if you wish to link a different account.");
						//Else the author is unregistered, and the message is long enough to include an account parameter 
						else if(e.getMessage().getContent().length() > 12) {
							//Register the account, and if successful
							if(link((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
									(e.getMessage().getAuthor() + "").indexOf(",")), e.getMessage().getContent().substring(11)));
								//Send a message to the requester stating which account they registered under
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
										"You are registered to the Nitro Type account **" + e.getMessage().getContent().substring(11) + "**."));
						//Else send the requester a message to include an account parameter
						} else e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
								"Invalid argument.\nEnsure that you include an account name, `l.register [username]`, to link an account."));
					}
					//Else if the command is unregister
					else if(Main.commands[loc][0].equalsIgnoreCase("l.unregister")) {
						//If the user is not registered to an account
						if(!isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, 
								(e.getMessage().getAuthor() + "").indexOf(","))))
							//Send a message to the requester that they are already unregistered
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
									"No previous registration exists for this Discord ID.\nUse `l.register` if you wish to link an account."));
						//Else unregister the requester from their NT, and if successful
						else if(unlink(e.getMessage().getAuthor().getId() + ""))
							//Send a message to the requester stating that they are no longer registered.
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
									"You are no longer registered to a Nitro Type account."));
						//Else the registration failed send a message to the requester informing them of the error
						else e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), 
								"The account was unable to be unregistered. Please try again, or contact " 
								+ Main.getDeveloperID() + " to attempt to resolve this issue."));
					}
					//Else if the command is info
					else if(Main.commands[loc][0].equalsIgnoreCase("l.info")) {
						//Send a message to the requester about the bot
						long currentTime = System.currentTimeMillis();
						long runtime = currentTime - Main.getLoginStamp();
						long days = runtime / 86400000, hours = runtime % 86400000 / 3600000, minutes = runtime % 86400000 % 3600000 / 60000, seconds = runtime % 86400000 % 3600000 % 60000 / 1000;
						String dayStr, hourStr, minuteStr, secondStr;
						if(days == 0)
							dayStr = "";
						else if(days == 1)
							dayStr = "1 day ";
						else dayStr = days + " days ";
						if(hours == 0)
							hourStr = "";
						else if(hours == 1)
							hourStr = "1 hour ";
						else hourStr = hours + " hours ";
						if(minutes == 0)
							minuteStr = "";
						else if(minutes == 1)
							minuteStr = "1 minute ";
						else minuteStr = minutes + " minutes ";
						if(seconds == 0)
							secondStr = "";
						else if(seconds == 1)
							secondStr = "1 second ";
						else secondStr = seconds + " seconds ";
						int[] counter = Main.getCounter();
						String commandStr = "";
						for(int i = 0; i < counter.length; i++)
							if(counter[i] != 0)
								commandStr += "**" + Main.commands[i][0].substring(2) + "**: " + counter[i] + "\n";
						
						e.getMessage().getChannel().sendMessage(new EmbedBuilder().setTitle("Loly Dealer").setDescription("A Discord bot for Nitro Type users.")
								.setAuthor("About", "https://discordapp.com/users/" + Main.getBotID().substring(2, Main.getBotID().length() - 1), "https://cdn.discordapp.com/avatars/237676240931258378/e3fafdd9e33b147d87b3460b136e1ae2.png")
								.setThumbnail(new File("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Nes.png"))
								.addField("https://github.com/Nes370/lolydealer", "Loly Dealer is made by Nes370. She is written in Java and uses BtoBastian's Javacord library.")
								.addField("https://discordapp.com/users/" + Main.getDeveloperID().substring(3, Main.getDeveloperID().length() - 1), 
										"If you wish to add her to a server, please contact me using the link above.").setColor(Color.PINK)
								.addInlineField("Runtime", "" +  dayStr + hourStr + minuteStr + secondStr)
								.addInlineField("Commands", commandStr).setFooter("Developed by Nes370"));
						
						//Print a log to the console declaring the information was sent
						System.out.println("Bot information sent.");
					}
				//Else the command is not implemented
				} else {
					//Send a message to the requester stating the command was not recognized
					e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Unknown command.\nUse `l.help` for a list of commands."));
					//Print a log to the console declaring the command error
					System.out.println("Command error announced.");
				}
			} else {
				//Send a message to the requester stating the command was not recognized
				e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Unknown command.\nUse `l.help` for a list of commands."));
				//Print a log to the console declaring the command error
				System.out.println("Command error announced.");
			}
		}
	}

	boolean unlink(String discordId) {
		File register = new File("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Register.txt");
		try {
			File temp = File.createTempFile("file", ".txt", register.getParentFile());
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(register), "UTF-8"));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
			for(String line; (line = in.readLine()) != null;) {
				if(line.contains(discordId)) 
					line = line.replace(line.substring(line.indexOf(discordId) - 1, 
							line.indexOf(discordId) + line.substring(line.indexOf(discordId) - 1).indexOf("}")), "");
				out.print(line);
			}
			out.close();
			in.close();
			if(register.delete())
				if(temp.renameTo(register)) {
					System.out.println("The account was removed.");
					return true;
				}
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println("An error occurred when attempting to remove the account from Register.txt");
		}
		System.out.println("Failed to remove linked account");
		return false;
	}

	boolean link(String discordId, String account) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Register.txt", true));
			writer.write("{" + discordId + "," + account + "}");
			writer.close();
			System.out.println("The account was linked.");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An error occured when attempting to write to Register.txt");
		}
		System.out.println("Failed to link account.");
		return false;
	}

	String linkedAccount(String discordId) {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Register.txt"));
			String line;
			while((line = in.readLine()) != null)
				if(line.contains(discordId)) {
					in.close();
					return line.substring(line.indexOf(discordId) + line.substring(line.indexOf(discordId)).indexOf(",") + 1, 
							line.indexOf(discordId) + line.substring(line.indexOf(discordId)).indexOf("}"));
				}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An error occured when attempting to read from Register.txt");
		}
		System.out.println("Failed to obtain linked account");
		return null;
	}

	boolean isRegistered(String discordId) {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Register.txt"));
			String line;
			while((line = in.readLine()) != null)
				if(line.contains(discordId)) {
					in.close();
					return true;
				}
			in.close();
		} catch (IOException e) {
				e.printStackTrace();
				System.out.println("An error occured when attempting to read from Register.txt");
		}
		return false;
	}

	String sendmsg(long userId, String msg) {
		return "<@" + userId + "> " + msg;
	}
}
