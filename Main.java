package com.github.nes370.lolydealer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;

public class Main {
	
	public static String[][] commands = {
			{"L.help", "Show this list of commands."},
			{"L.register", "Register an account to your Discord ID. `l.register [Account]`"},
			{"L.unregister", "Remove a linked account from your Discord ID. `l.unregister`"},
			{"L.value", "Evaluate an account. `l.value [Account] [Bonus]`"},
			{"L.cars", "Rank an account's cars. `l.cars [Account] [Cars]`"},
			{"L.info", "Learn about this bot."}
	};
	private static String token;
	public static String developerID, botID;
	public static void main(String[] args) {
		setToken();
		setDeveloperID();
		setBotID();
		DiscordApi api = new DiscordApiBuilder().setToken(getToken()).login().join();
		System.out.println("Logged in!");
		api.updateActivity("with Lolis", ActivityType.PLAYING);
		api.updateStatus(UserStatus.IDLE);
		api.addMessageCreateListener(new Commands());
	}
	
	public static String getToken() {return token;}
	public static void setToken() {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Token.txt"));
			token = in.readLine();
			in.close();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An error occured when attempting to read from Token.txt");
		}
		return;
	}
	
	public static String getDeveloperID() {return developerID;}
	public static void setDeveloperID() {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Developer.txt"));
			developerID = in.readLine();
			in.close();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An error occured when attempting to read from Developer.txt");
		}
		return;
	}

	public static String getBotID() {return botID;}
	public static void setBotID() {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("C:/Users/Rie_f/eclipse-workspace/lolydealer/src/main/resources/Bot.txt"));
			botID = in.readLine();
			in.close();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An error occured when attempting to read from Bot.txt");
		}
		return;
	}
}