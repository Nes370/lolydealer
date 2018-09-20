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

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class Commands implements MessageCreateListener {
	//When a message is created
	@Override
	public void onMessageCreate(MessageCreateEvent e) {
		
		//If I am the author
		if(e.getMessage().getAuthor().isBotOwner() || e.getMessage().getAuthor().isYourself() || isAuthorized(e.getMessage().getAuthor().getId()) //TODO create an isAuthorized method
				//AllTimeLow == 206587507155009536L
				)
			if(e.getMessage().getContent().length() > 5 && e.getMessage().getContent().startsWith("loly.")) {
				e.getMessage().getChannel().sendMessage(e.getMessage().getContent().substring(5));
				return;
			}
			//And the message contains the logon command, in the authorized channel, turn on Direct Message logs
			else if(e.getMessage().getContent().equalsIgnoreCase("l.logOn") && e.getMessage().getChannel().getId() == Long.parseLong(Main.getAuthorizedLogChannel())) {
				Main.setDeveloperChannel(e.getMessage().getChannel());
				Main.setLogging(true);
				System.out.println("Direct Message Logging has been enabled.");
				return;
			//Else if the message contains the logoff command, in the authorized channel, turn off Direct Message logs
			} else if(e.getMessage().getContent().equalsIgnoreCase("l.logOff") && e.getMessage().getChannel().getId() == Long.parseLong(Main.getAuthorizedLogChannel())) {
				Main.setDeveloperChannel(null);
				Main.setLogging(false);
				System.out.println("Direct Message Logging has been disabled.");
				return;
			}
		
		//If this bot is the author, ignore it.
		if(e.getMessage().getAuthor().isYourself()) { return; }
		
		//If the message starts with an "l."
		String prefix = "l.";
		//TODO Some method to check if a channel has a custom prefix, then set prefix to that custom one.
		if(e.getMessage().getContent().toLowerCase().startsWith(prefix)) {
			
			//Get the current date and time
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			//Print a log to the console that includes who sent what command in which channel of which server at what date and time.
			System.out.println("At " + dtf.format(now) + ", " + e.getMessage().getAuthor() + " used \"" + e.getMessage().getContent() + "\" in " + e.getMessage().getServer() + ", " + e.getMessage().getChannel() + ".");
			if(Main.isLogging())
				new MessageBuilder().append("At " + dtf.format(now) + ", " + sendmsg(e.getMessage().getAuthor().getId(), "") + " used \"" + e.getMessage().getContent() + "\" in <#" + e.getMessage().getChannel().getId() + ">.").send(Main.getDeveloperChannel());
			
			//If the message isn't bigger than the prefix
			if(!(e.getMessage().getContent().length() > prefix.length())) {
				//Send an error message to the requester stating the command was not recognized
				e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[0][0] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
				Main.addECounter(0);
				//Print a log to the console declaring the command error
				System.out.println(Main.errors[0][0] + " error announced.");
				return;
			}
			
			//And if the character after prefix is a space
			if(Character.toString(e.getMessage().getContent().charAt(prefix.length())).equals(" ")) {
				//Send a message to the requester stating the command was not recognized
				e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
						Main.errors[1][0] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
				Main.addECounter(1);
				//Print a log to the console declaring the command error
				System.out.println(Main.errors[1][0] + " announced.");
				return;
			}
			
			boolean found = false;
			byte loc = 0;
			//A loop to check if the message contains a command
			for(int i = 0; i < Main.commands.length; i++) {
				//If the message is large enough to contain the command AND the initial string of characters matches a command
				if(e.getMessage().getContent().length() - prefix.length() >= Main.commands[i][0].length()
						&& Main.commands[i][0].equalsIgnoreCase(e.getMessage().getContent().substring(prefix.length(), prefix.length() + Main.commands[i][0].length()))) {
					//If the message is larger than the command and the character after the command isn't a space
					if(e.getMessage().getContent().length() - prefix.length() > Main.commands[i][0].length() && e.getMessage().getContent().charAt(prefix.length() + Main.commands[i][0].length()) != ' ')
						/*Do nothing*/;
					else {
						found = true;
						loc = (byte)i;
						Main.addCounter(i);
						break;
					}
				}
			}
			
			//If a command was found in the message
			if(found) {
				
				//And the command was help
				if(Main.commands[loc][0].equalsIgnoreCase("help")) {
					//Organize a list of commands
					EmbedBuilder embed = new EmbedBuilder().setTitle("List of Commands").setAuthor("Loly Dealer", null, "https://cdn.discordapp.com/avatars/455132593642536983/6bb82ec527d846631f9b511ec510bc4b.png").setColor(Color.PINK).setFooter("Developed by Nes370");
					for(int i = 0; i < Main.commands.length; i++)
						embed.addField(prefix + Main.commands[i][0], Main.commands[i][1]);
					//And send a message to the requester with that list. 
					e.getMessage().getChannel().sendMessage(embed);
					//Print a log to the console to declare the command's request.
					System.out.println("Help message printed.");
					return;
				}
				
				//Else if the command was value
				else if(Main.commands[loc][0].equalsIgnoreCase("value")) {
					String arg = "";
					//If the command was minimal in length
					if(e.getMessage().getContent().length() < prefix.length() + 7) {
						//And the message only included "l.value"
						if(!e.getMessage().getContent().equalsIgnoreCase(prefix + "value")) {
							//Send a message to the requester stating the command was not recognized
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[1] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							//Add a count to the Error counter
							Main.addECounter(1);
							//Print a log to the console declaring the command error
							System.out.println(Main.errors[1][0] + " error announced.");
							return;
						}
						//If the requester is registered to a NT account
						if(isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Set the username parameter to their registered NT account
							arg = linkedAccount(e.getMessage().getAuthor().getIdAsString());
							//Print a log to the console declaring the change in the command arguments
							System.out.println("Adjusted to \"" + prefix + "value " + arg + "\"");
						//If the requester is not registered
						} else {
							//Send a message to the requester that asks them to register to their NT account
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A registered account was not found.\nPlease register with `" + prefix + "register [username]`").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare the lack of a registered account
							System.out.println("Account not found.");
							//And end the message reaction
							return;
						}
					//Else if the message includes an @mention
					} else if(e.getMessage().getContent().contains("<@") && e.getMessage().getContent().contains(">"))	{
						//If the @mention includes a "!"
						if(e.getMessage().getContent().contains("<@!")) {
							//And the Discord ID in the @mention is registered to a NT account
							if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")))) {
								//If the command contains more text after the @mention
								if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
									//Set the string argument to the linked account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \""+ prefix + "value " + arg + "\"");
								//Else the command only contains the @mention
								} else {
									//Set the string argument to the linked account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "value " + arg + "\"");
								}
							} else {
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent()
										.substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
								System.out.println("Account not found.");
								return;
							}
						//And if it doesn't include a "!"
						} else {
							//And the Discord ID in the @mention is registered to a NT account
							if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")))) {
								//If the command contains more text after the @mention
								if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
									//Set the string argument to the linked account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent()
											.substring(e.getMessage().getContent().indexOf(">") + 1);
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "value " + arg + "\"");
								} else {
									//Set the string argument to the linked account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "value " + arg + "\"");
								}
							} else {
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent()
										.substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
								System.out.println("Account not found.");
								return;
							}
						}
					//Else set the account argument to what is written in plain text
					} else {
						arg = e.getMessage().getContent().substring(prefix.length() + 6);
						//Attempt to evaluate the account specified by arg
					} 
					try {
						e.getMessage().getChannel().sendMessage(Evaluate.main(arg).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare a successful evaluation
						System.out.println("Account evaluated.");
						//If an error is caught
					} catch (Exception e1) {
						//Print a log to the console that shows where in the code the error occurred
						e1.printStackTrace();
						//Send a message to the requester declaring the error
						e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A " + Main.errors[2][0].toLowerCase() + " error occurred.\nUse `l.help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare the requester was notified of the error
						Main.addECounter(2);
						System.out.println("Runtime error announced.");
					}
				}
					
				else if(Main.commands[loc][0].equalsIgnoreCase("compare")) {
					String arg1 = "", arg2 = "";
					//If the string is long enough to hold l.compare and one more character
					if(e.getMessage().getContent().indexOf(' ') == -1) {
						//And it doesn't match l.compare
						if(!e.getMessage().getContent().substring(0, 9).equalsIgnoreCase(prefix + "compare")) {
							//Send a message to the requester stating the command was not recognized
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[1][0] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED)
									.setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(1);
							//Print a log to the console declaring the command error
							System.out.println(Main.errors[1][0] + " error announced.");
							return;
						//Else it does match compare
						} else {
							//Send an error message stating the command needs an account to compare to
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[3][0] + ".\nEnsure that you include an account name or url to compare with.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(3);
							//Print a log to the console declaring the command error
							System.out.println(Main.errors[3][0] + " error announced.");
							return;
						}
					}
					//if the string contains one space only, meaning there's only one argument
					else if(e.getMessage().getContent().indexOf(' ') == e.getMessage().getContent().lastIndexOf(' ')) {
						//If the requester is registered to a NT account, argument 1 is set
						if(isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Set the [username] value equal to their registered NT account
							arg1 = linkedAccount(e.getMessage().getAuthor().getIdAsString());
							//Print a log to the console declaring the change in the command arguments
							System.out.println("Adjusted to \"l.compare " + arg1 + " " + e.getMessage().getContent().substring(10) + "\"");
						//If the requester is not registered, hold up
						} else {
							//Send a message to the requester that asks them to register to their NT account
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A registered account was not found for your Discord ID.\nPlease register with `l.register [username]`").setColor(Color.YELLOW)
									.setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare the lack of a registered account
							System.out.println("Account not found.");
							//And end the message reaction
							return;
						}
						//If the command includes an @mention for the second parameter
						if(e.getMessage().getContent().contains("<@") && e.getMessage().getContent().contains(">"))	{
							//If the @mention includes a "!"
							if(e.getMessage().getContent().contains("<@!")) {
								//And the Discord ID in the @mention is registered to a NT account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")))) {
									arg2 = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
									System.out.println("Adjusted to \"" + prefix + "compare " + arg1 + " " + arg2 + "\"");
								//But if the Discord ID isn't linked to an account
								} else {
									//Send a message to the requester that asks them to use a valid argument to compare with
									e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
									//Print a log to the console to declare the lack of a registered account
									System.out.println("Account not found.");
									//And end the message reaction
									return;
								}
							//And if it doesn't include a "!"
							//And the Discord ID in the @mention is registered to a NT account
							} else if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")))) {
								//Set the string argument to the linked account
								arg2 = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
								//Print a log to the console declaring the change in command arguments
								System.out.println("Adjusted to \"l.compare " + arg1 + " " + arg2 + "\"");
							} else {
								//Send a message to the requester that tells them to use a valid argument to compare with
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
								//Print a log to the console to declare the lack of a registered account
								System.out.println("Account not found.");
								//And end the message reaction
								return;
							}
						}
						//Else set the account argument to what is written in plain text
						else arg2 = e.getMessage().getContent().substring(prefix.length() + 8);
							
						//Attempt to compare the accounts specified by arg1 and arg2
						try {
							e.getMessage().getChannel().sendMessage(Evaluate.main(arg1, arg2).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare a successful comparison
							System.out.println("Accounts compared.");
						//If an error is caught
						} catch (Exception e1) {
							//Print a log to the console that shows where in the code the error occurred
							e1.printStackTrace();
							//Send a message to the requester declaring the error
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A " + Main.errors[2][0].toLowerCase() + " error occurred.\nUse `l.help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							//Add a count to the appropriate error counter
							Main.addECounter(2);
							//Print a log to the console to declare the requester was notified of the error
							System.out.println("Runtime error announced.");
							return;
						}
					}
					//TODO If the command has multiple spaces, remove excess spaces, then check if there are too many arguments, if there are two arguments, or if there is one argument
					else {
						String temp = e.getMessage().getContent();
						for(int i = 0; i < temp.length(); i++) {
							if(i == temp.length() - 1 && temp.charAt(i) == ' ') {
								temp = temp.substring(0, i);
							}
							else if(temp.charAt(i) == ' ' && temp.charAt(i + 1) == ' ') {
								temp = temp.substring(0, i) + temp.substring(i + 1);
								i--;
							}
							System.out.println(temp);
						}
					}
				}
				//Else if the command is cars
				else if(Main.commands[loc][0].equalsIgnoreCase("cars")) {
					//And the message contains only "l.cars"
					String arg = "";
					if(e.getMessage().getContent().length() < prefix.length() + 6) {
						if(!e.getMessage().getContent().equalsIgnoreCase(prefix + "cars")) {
							//Send a message to the requester stating the command was not recognized
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[1][0] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(1);
							//Print a log to the console declaring the command error
							System.out.println(Main.errors[1][0] + " error announced.");
							return;
						}
						//If the author is registered
						if(isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Set the account argument to the corresponding linked account
							arg = linkedAccount(e.getMessage().getAuthor().getIdAsString());
							//Print a log to the console declaring the change in command arguments
							System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
						//If the author is not registered
						} else {
							//Send the requester a message to register their NT account
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A registered account was not found.\nPlease register with `" + prefix + "register [username OR url]`")
									.setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare the lack of a registered account
							System.out.println("Account not found.");
							//End the message reaction
							return;
						}
					//Else if the command contains an @mention
					} else if(e.getMessage().getContent().contains("<@") && e.getMessage().getContent().contains(">")) {
						//If the @mention contains an exclamation point
						if(e.getMessage().getContent().contains("<@!")) {
							//if the enclosed Discord ID is registered to an account
							if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")))) {
								//If the command contains more text after the @mention
								if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, 
											e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
									//Print a log to the console declaring the change in command arguments 
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								//Else the command doesn't contain more text
								} else {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments 
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								}
							} else {
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user "
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".")
										.setColor(Color.YELLOW).setFooter("Status repoort for " + e.getMessage().getAuthor().getDisplayName()));
								return;
							}
						//Else if it does not contain an exclamation point
						} else {
							//If the enclosed Discord ID is registered to an account
							if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")))) {
								//If the command contains more text after the @mention
								if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">"))) 
											+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								//Else the command doesn't contain more text
								} else {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								}
							} else { 
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " 
										+ e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), 
										e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
									return;
							}
						}
					//Else set the account argument to what is written in plain text
					} else arg = e.getMessage().getContent().substring(prefix.length() + 5);
					//Attempt to rank the cars of the account specified by arg
					try {
						e.getMessage().getChannel().sendMessage(RankCars.main(arg, 0).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare successful ranking
						System.out.println("Cars ranked.");
					//If an error is caught
					} catch (Exception e1) { 
						//Print a log to the console that shows where in the code the error occurred
						e1.printStackTrace();
						//Send a message to the requester declaring the error
						e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
								"A " + Main.errors[2][0].toLowerCase() + " error occurred.\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare the requester was notified of the error
						Main.addECounter(2);
						System.out.println("Runtime error announced.");
					}
				}
					
				//Else if the command equals l.soldcars
				else if(Main.commands[loc][0].equalsIgnoreCase("soldcars")) {
					//And the message contains only "l.cars"
					String arg = "";
					if(e.getMessage().getContent().length() < prefix.length() + 10) {
						if(!e.getMessage().getContent().equalsIgnoreCase(prefix + "soldcars")) {
							//Send a message to the requester stating the command was not recognized
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[1][0] + ".\nUse `l.help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(1);
							//Print a log to the console declaring the command error
							System.out.println(Main.errors[1][0] + " error announced.");
							return;
						}
						//If the author is registered
						if(isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Set the account argument to the corresponding linked account
							arg = linkedAccount(e.getMessage().getAuthor().getIdAsString());
							//Print a log to the console declaring the change in command arguments
							System.out.println("Adjusted to \"" + prefix + "soldcars " + arg + "\"");
						//If the author is not registered
						} else { 
							//Send the requester a message to register their NT account
							//Print a log to the console to declare the lack of a registered account
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A registered account was not found.\nPlease register with `l.register [username OR url]`").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							//Print a log to the console to declare the lack of a registered account
							System.out.println("Account not found.");
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
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								//Else the command doesn't contain more text
								} else {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments 
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								}
							} else {
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status repoort for " + e.getMessage().getAuthor().getDisplayName()));
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
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								//Else the command doesn't contain more text
								} else {
									//Set the account argument to its corresponding account
									arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
									//Print a log to the console declaring the change in command arguments
									System.out.println("Adjusted to \"" + prefix + "cars " + arg + "\"");
								}
							} else { 
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A linked Nitro Type account could not be found for the Discord user " + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@"), e.getMessage().getContent().indexOf(">") + 1) + ".").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
								return;
							}
						}
					//Else set the account argument to what is written in plain text
					} else arg = e.getMessage().getContent().substring(prefix.length() + 9);
					//Attempt to rank the cars of the account specified by arg
					try {
						e.getMessage().getChannel().sendMessage(RankCars.main(arg, 1).setFooter("Results for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare successful ranking
						System.out.println("Cars ranked.");
					//If an error is caught
					} catch (Exception e1) { 
						//Print a log to the console that shows where in the code the error occurred
						e1.printStackTrace();
						//Send a message to the requester declaring the error
						e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A " + Main.errors[2][0].toLowerCase() + " error occurred.\nUse `l.help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
						//Print a log to the console to declare the requester was notified of the error
						Main.addECounter(2);
						System.out.println("Runtime error announced.");
					}						
				}
				
				//Else if the command is register
				else if(Main.commands[loc][0].equalsIgnoreCase("register")) {
					//If the author is already registered
					if(isRegistered(e.getMessage().getAuthor().getIdAsString()))
						//Send a message to the requester to tell them they are already registered
						e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription("A previous registration to the Nitro Type account **" 
									+ linkedAccount(e.getMessage().getAuthor().getIdAsString())
									+ "** already exists.\nUse `" + prefix + "unregister` if you wish to link a different account.").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
						//Else the author is unregistered, and the message is long enough to include an account parameter 
						else if(e.getMessage().getContent().length() > prefix.length() + 10) {
							if(e.getMessage().getContent().length() > prefix.length() + 34 && e.getMessage().getContent().substring(prefix.length() + 9).startsWith("www.nitrotype.com/racer/")) {
								if(link(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getContent().substring(prefix.length() + 33))) {
									e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
											"You are registered to the Nitro Type account **" + e.getMessage().getContent().substring(prefix.length() + 33) + "**.").setColor(Color.GREEN).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
									return;
								}
							}
							else if(e.getMessage().getContent().length() > prefix.length() + 42 && e.getMessage().getContent().substring(prefix.length() + 9).startsWith("https://www.nitrotype.com/racer/")) {
								if(link(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getContent().substring(prefix.length() + 41))) { 
									e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
											"You are registered to the Nitro Type account **" + e.getMessage().getContent().substring(prefix.length() + 41) + "**.").setColor(Color.GREEN).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
									return;
								}
							}
							//Register the account, and if successful
							//(e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(","))
							else if(link(e.getMessage().getAuthor().getIdAsString(), e.getMessage().getContent().substring(prefix.length() + 9))) {
								//Send a message to the requester stating which account they registered under
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
										"You are registered to the Nitro Type account **" + e.getMessage().getContent().substring(prefix.length() + 9) + "**.").setColor(Color.GREEN).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName())); 
							}
							else {
								e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
									Main.errors[4][0] +".\nPlease try again, or contact [Nes370](" //TODO this error looks funky
									+ Main.getDeveloperID() + ") to attempt to resolve this issue.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
								Main.addECounter(4);
								System.out.println(Main.errors[4][0] + " error announced.");
							}
								//Else send the requester a message to include an account parameter
						} else {
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(Main.errors[3][0] + ".\nEnsure that you include an account name or url to link that account. `" + prefix.length() + "register [username OR url]`").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(3);
							System.out.println(Main.errors[3][0] + " error announced.");
							return;
						}
					}
					
					//Else if the command is unregister
					else if(Main.commands[loc][0].equalsIgnoreCase("unregister")) {
						//If the user is not registered to an account
						if(!isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Send a message to the requester that they are already unregistered
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
									"No previous registration exists for this Discord ID.\nUse `" + prefix + "register [username OR url]` if you wish to link an account.").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							return;
						}
						//Else unregister the requester from their NT, and if successful
						else if(unlink(e.getMessage().getAuthor().getIdAsString())) {
							//Send a message to the requester stating that they are no longer registered.
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
									"You are no longer registered to a Nitro Type account.").setColor(Color.GREEN).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							return;
						}
						//Else the registration failed send a message to the requester informing them of the error
						else {
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
								"Un" + Main.errors[4][0].toLowerCase() +".\nPlease try again, or contact [Nes370](" 
								+ Main.getDeveloperID() + ") to attempt to resolve this issue.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
							Main.addECounter(4);
							System.out.println("Un" + Main.errors[4][0].toLowerCase() + " error announced.");
						}
					}
					
					//Else if the command is verify
					else if(Main.commands[loc][0].equalsIgnoreCase("l.verify")) {
						//If the user is not registered to an account
						if(!isRegistered(e.getMessage().getAuthor().getIdAsString())) {
							//Send a message to the requester that they are not registered
							e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(
									"No previous registration exists for this Discord ID.\nUse `l.register [username OR url]` if you wish to link an account.").setColor(Color.YELLOW).setFooter("Status report for " + e.getMessage().getAuthor().getDisplayName()));
							return;
						//If the user is already verified
						} else if(verifiedStatus(e.getMessage().getAuthor().getIdAsString()) == 2) {
							//send error message
						//If the user is pending verification
						} else if(verifiedStatus(e.getMessage().getAuthor().getIdAsString()) == 1) {
							//Perform verification function
							//Verification function will look up a user's cars
							//If they have at least 5 vehicles, it will select one that is not in use for them to switch to.
							//Then it will log {discord ID,Nitro Type username,verification time stamp, verified t/f} to VerifiedRegister.txt
							//The user will be prompted to use l.verify again
							//If a user takes more than 5 minutes to verify, it will generate a new desired car.
							//
						}
					}
					
					//Else if the command is info
					else if(Main.commands[loc][0].equalsIgnoreCase("info")) {
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
								commandStr += "**" + Main.commands[i][0] + "**: " + counter[i] + "\n";
						int[] eCounter = Main.getECounter();
						String errorStr = "";
						for(int j = 0; j < eCounter.length; j++)
							if(eCounter[j] != 0)
								errorStr += "**" + Main.errors[j][0] + "**: " + eCounter[j] + "\n";
						EmbedBuilder embed = new EmbedBuilder().setTitle("Loly Dealer").setDescription("A Discord bot for Nitro Type users.")
								.setAuthor("About", "https://discord.gg/KNWzUPn", "https://cdn.discordapp.com/avatars/237676240931258378/e3fafdd9e33b147d87b3460b136e1ae2.png")
								//.setThumbnail(new File("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Nes.png"))
								.addField("Creation", "Loly Dealer was created by Nes370. She is written in Java and uses [BtoBastian's Javacord library](https://github.com/BtoBastian/Javacord/tree/v_3). You can read her source code on [GitHub](https://github.com/Nes370/lolydealer)")
								.addField("Contact", "If you wish to add her to a server, please contact " + Main.getDeveloperID() + ".").setColor(Color.PINK)
								.addInlineField("Runtime", "" +  dayStr + hourStr + minuteStr + secondStr)
								.setFooter("Developed by Nes370");
						if(commandStr.length() != 0)
							embed.addInlineField("Commands", commandStr);
						if(errorStr.length() != 0)
							embed.addInlineField("Errors", errorStr);
						
						e.getMessage().getChannel().sendMessage(embed);
						//Print a log to the console declaring the information was sent
						System.out.println("Bot information sent.");
						return;
					}	
					else if(Main.commands[loc][0].equalsIgnoreCase("cascade")) {
						String translation = "", text = e.getMessage().getContent();
						for(int i = 9; i < text.length(); i++) {
							translation += codex(text.charAt(i), 0);
						}
						e.getMessage().getChannel().sendMessage(translation);
					}
					else if(Main.commands[loc][0].equalsIgnoreCase("qwerty")) {
						String translation = "", text = e.getMessage().getContent();
						for(int i = 9; i < text.length(); i++) {
							translation += codex(text.charAt(i), 1);
						}
						e.getMessage().getChannel().sendMessage(translation);
					}
				//Else the command is not implemented
				} else {
					//Send a message to the requester stating the command was not recognized
					e.getMessage().getChannel().sendMessage(new EmbedBuilder().setDescription(//sendmsg(e.getMessage().getAuthor().getId(), 
							Main.errors[1][0] + ".\nUse `" + prefix + "help` for a list of commands.").setColor(Color.RED).setFooter("Error message for " + e.getMessage().getAuthor().getDisplayName()));
					Main.addECounter(1);
					//Print a log to the console declaring the command error
					System.out.println(Main.errors[1][0] + " announced.");
					return;
				}
		} 
		//TODO team, wcapply, wcstats 
		
	}
	

	private char codex(char c, int k) {
		char[][] codex = {{'q', 'p'}, {'w', 'g'}, {'e', 'a'}, {'r', 'y'}, {'t', 'u'}, {'y', 'x'}, {'u', 'k'}, {'i', 'h'}, {'o', 's'}, {'p', 'c'}, 
				{'a', 'w'}, {'s', 'o'}, {'d', 'n'}, {'f', 'e'}, {'g', 'm'}, {'h', 'l'}, {'j', 'r'}, {'k', 'i'}, {'l', 'd'},
				{'z', 'b'}, {'x', 'q'}, {'c', 'z'}, {'v', 'v'}, {'b', 'j'}, {'n', 't'}, {'m', 'f'}};		
		if(Character.isUpperCase(c))
			for(int i = 0; i < codex.length; i++)
				if(codex[i][k] == Character.toLowerCase(c))
					return Character.toUpperCase(codex[i][(k + 1) % 2]);
		for(int i = 0; i < codex.length; i++)
			if(codex[i][k] == c)
				return codex[i][(k + 1) % 2];
		return c;
	}


	private boolean isAuthorized(long id) {
		// TODO Auto-generated method stub
		return false;
	}


	private int verifiedStatus(String idAsString) {
		// TODO Auto-generated method stub
		return 0;
	}

	boolean unlink(String discordId) {
		File register = new File("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Register.txt");
		try {
			File temp = File.createTempFile("file", ".txt", register.getParentFile());
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(register), "UTF-8"));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
			for(String line; (line = in.readLine()) != null;) {
				if(line.contains('{' + discordId)) 
					line = line.replace(line.substring(line.indexOf('{' + discordId), line.indexOf('{' + discordId) + line.substring(line.indexOf('{' + discordId)).indexOf("}") + 1), "");
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
				if(line.contains('{' + discordId)) {
					in.close();
					return line.substring(line.indexOf('{' + discordId) + line.substring(line.indexOf('{' + discordId)).indexOf(",") + 1, 
							line.indexOf('{' + discordId) + line.substring(line.indexOf('{' + discordId)).indexOf("}"));
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
				if(line.contains('{' + discordId)) {
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
