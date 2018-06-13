package com.github.nes370.lolydealer;

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
		if(e.getMessage().getContent().startsWith("l.") || e.getMessage().getContent().startsWith("L.")) {
			//And if the character after "l." isn't a space
			if(!Character.toString(e.getMessage().getContent().charAt(2)).equals(" ")) {	
				//Check if the message contains a command
				boolean found = false;
				byte loc = 0;
				for(int i = 0; i < Main.commands.length; i++) {
					if(e.getMessage().getContent().length() >= Main.commands[i][0].length() && Main.commands[i][0].equalsIgnoreCase(e.getMessage().getContent().substring(0, Main.commands[i][0].length()))) {
						found = true;
						loc = (byte)i;
						break;
					}
				}
				//Get the current date and time
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
				LocalDateTime now = LocalDateTime.now();									//
				//Print a log to the console that includes who sent what command in which channel of which server at what date and time.
				System.out.println(e.getMessage().getAuthor() + " used \"" + e.getMessage().getContent() + "\" in " + e.getMessage().getChannel() + " of " + e.getMessage().getServer() + " at " + dtf.format(now));
				//If a command was found in the message
				if(found) {
					//And the command was help
					if(Main.commands[loc][0].equalsIgnoreCase("l.help")) {
						//Organize a list of the current commands 
						String msg = "\n";
						for(int i = 0; i < Main.commands.length; i++)
							msg += "**" + Main.commands[i][0] + "**	" + Main.commands[i][1] + "\n";
						//And send a message to the requester with that list. 
						e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), msg));
						//Print a log to the console to declare the command's request.
						System.out.println("Help message printed.");
					} 
					//Else if the command was value
					else if(Main.commands[loc][0].equalsIgnoreCase("l.value")) {
						//And the message only included "l.value"
						String arg = "";
						if(e.getMessage().getContent().equalsIgnoreCase("l.value")) {
							//If the requester is registered to a NT account
							if(isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(",")))) {//If the author is registered
								//Set the [account] value equal to their registered NT account
								arg = linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(",")));	//set the string argument to the linked account
								//Print a log to the console declaring the change in the command arguments
								System.out.println("Adjusted to \"l.value " + arg + "\"");
							//If the requester is not registered
							} else {
								//Send a message to the requester that asks them to register to their NT account
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "A registered account was not found.\nPlease register with `l.register [account]`"));
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
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									//Else the command only contains the @mention
									} else {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								}
							//And if it doesn't include a "!"
							} else {
								//And the Discord ID in the @mention is registered to a NT account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									} else {
										//Set the string argument to the linked account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								}
							}
						//Else set the account argument to what is written in plain text
						} else {
							arg = e.getMessage().getContent().substring(8);
						//Attempt to evaluate the account specified by arg
						} try {
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), Evaluate.main(arg)));
							//Print a log to the console to declare a successful evaluation
							System.out.println("Account evaluated.");
						//If an error is caught
						} catch (Exception e1) {
							//Print a log to the console that shows where in the code the error occurred
							e1.printStackTrace();
							//Send a message to the requester declaring the error
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "An error occurred.\nUse `l.help` for a list of commands."));
							//Print a log to the console to declare the requester was notified of the error
							System.out.println("Runtime error announced.");
						}
					} 
					//Else if the command is cars
					else if(Main.commands[loc][0].equalsIgnoreCase("l.cars")) {
						//And the message contains only "l.cars"
						String arg = "";
						if(e.getMessage().getContent().equalsIgnoreCase("l.cars")) {
							//If the author is registered
							if(isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(",")))) {//If the author is registered
								//Set the account argument to the corresponding linked account
								arg = linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(","))); //Set the string argument to the linked account
								//Print a log to the console declaring the change in command arguments
								System.out.println("Adjusted to \"l.cars " + arg + "\"");
							//If the author is not registered
							} else {
								//Send the requester a message to register their NT account
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "A registered account was not found.\nPlease register with `l.register [account]`"));
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
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments 
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									//Else the command doesn't contain more text
									} else {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@!") + 3, e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments 
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								}
							//Else if it does not contain an exclamation point
							} else {
								//If the enclosed Discord ID is registered to an account
								if(isRegistered(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")))) {
									//If the command contains more text after the @mention
									if(e.getMessage().getContent().length() > e.getMessage().getContent().indexOf(">") + 1) {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">"))) + e.getMessage().getContent().substring(e.getMessage().getContent().indexOf(">") + 1);
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									//Else the command doesn't contain more text
									} else {
										//Set the account argument to its corresponding account
										arg = linkedAccount(e.getMessage().getContent().substring(e.getMessage().getContent().indexOf("<@") + 2, e.getMessage().getContent().indexOf(">")));
										//Print a log to the console declaring the change in command arguments
										System.out.println("Adjusted to \"l.value " + arg + "\"");
									}
								}
							}
						//Else set the account argument to what is written in plain text
						} else arg = e.getMessage().getContent().substring(7);
						//Attempt to rank the cars of the account specified by arg
						try {
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), RankCars.main(arg)));
							//Print a log to the console to declare successful ranking
							System.out.println("Cars ranked.");
						//If an error is caught
						} catch (Exception e1) {
							//Print a log to the console that shows where in the code the error occurred
							e1.printStackTrace();
							//Send a message to the requester declaring the error
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "An error occurred.\nUse `l.help` for a list of commands."));
							//Print a log to the console to declare the requester was notified of the error
							System.out.println("Runtime error announced.");
						}
					}
					//Else if the command is register
					else if(Main.commands[loc][0].equalsIgnoreCase("l.register")) {
						//If the author is already registered
						if(isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(","))))
							//Send a message to the requester to tell them they are already registered
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "A previous registration to the Nitro Type account **" + linkedAccount((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(",")))) + "** already exists.\nUse `l.unregister` if you wish to link a different account.");
						//Else the author is unregistered, and the message is long enough to include an account parameter 
						else if(e.getMessage().getContent().length() > 12) {
							//Register the account, and if successful
							if(link((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(",")), e.getMessage().getContent().substring(11)));
								//Send a message to the requester stating which account they registered under
								e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "You are registered to the Nitro Type account **" + e.getMessage().getContent().substring(11) + "**."));
						//Else send the requester a message to include an account parameter
						} else e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Invalid argument.\nEnsure that you include an account name, `l.register [Account]`, to link an account."));
					}
					//Else if the command is unregister
					else if(Main.commands[loc][0].equalsIgnoreCase("l.unregister")) {
						//If the user is not registered to an account
						if(!isRegistered((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4, (e.getMessage().getAuthor() + "").indexOf(","))))
							//Send a message to the requester that they are already unregistered
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "No previous registration exists for this Discord ID.\nUse `l.register` if you wish to link an account."));
						//Else unregister the requester from their NT, and if successful
						else if(unlink((e.getMessage().getAuthor() + "").substring((e.getMessage().getAuthor() + "").indexOf("id") + 4,(e.getMessage().getAuthor() + "").indexOf(","))))
							//Send a message to the requester stating that they are no longer registered.
							e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "You are no longer registered to a Nitro Type account."));
						else e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "The account was unable to be unregistered. Please try again, or contact " + Main.getDeveloperID() +" to attempt to resolve this issue."));
					} 
					
					else if(Main.commands[loc][0].equalsIgnoreCase("l.info")) {
						e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), Main.getBotID() + " is made by Nes370. She is written in Java using BtoBastian's Javacord library.\nIf you wish to add her to a server, please contact " + Main.getDeveloperID() + "."));
						System.out.println("Bot information sent.");
					}
				//Else the command is not implemented	
				} else {
					//Send a message to the requester stating the command was not recognized
					e.getMessage().getChannel().sendMessage(sendmsg(e.getMessage().getAuthor().getId(), "Unknown command.\nUse `l.help` for a list of commands."));
					//Print a log to the console declaring the command error
					System.out.println("Command error announced.");
				}
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
					line = line.replace(line.substring(line.indexOf(discordId) - 1, line.indexOf(discordId) + line.substring(line.indexOf(discordId) - 1).indexOf("}")), "");
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
					return line.substring(
							line.indexOf(discordId) + line.substring(line.indexOf(discordId)).indexOf(",") + 1, 
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
