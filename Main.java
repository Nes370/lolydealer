package com.github.nes370.lolydealer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;

public class Main {
	
	public static String[][] commands = {
			{"L.help", "Show this list of commands."},
			{"L.value", "Evaluate an account.\n`l.value [username or @discordname] [bonus value]`"},
			{"L.cars", "Rank an account's cars.\n`l.cars [username or @discordname] [number of cars]`"},
			{"L.register", "Register an account to your Discord ID.\n`l.register [username]`"},
			{"L.unregister", "Remove a linked account from your Discord ID."},
			//L.soldCars [Account]
			//L.team [Account/Team]
			{"L.info", "Learn about this bot."}
	};
	private static int[] counter = new int[commands.length];
	private static String token, developerID, botID;
	private static long loginStamp;
	public static void main(String[] args) {
		setToken();
		setDeveloperID();
		setBotID();
		DiscordApi api = new DiscordApiBuilder().setToken(getToken()).login().join();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		//LocalDateTime now = LocalDateTime.now();
		LocalDateTime loginDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(loginStamp = System.currentTimeMillis()), ZoneId.systemDefault());
		System.out.println("Logged in at " + dtf.format(loginDateTime) + ".");
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

	public static long getLoginStamp() {
		return loginStamp;
	}

	public static void setLoginStamp(long loginStamp) {
		Main.loginStamp = loginStamp;
	}

	public static int[] getCounter() {
		return counter;
	}

	public static void addCounter(int index) {
		Main.counter[index] += 1;
	}
}