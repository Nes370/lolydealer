package com.github.nes370.lolydealer;

import java.awt.Color;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class Evaluate {

	public static EmbedBuilder main(String arg) throws Exception {
		
		long bonus = 0;
		String url = "";
		int index = 0;
		//removes leading spaces
		while(arg.substring(index).startsWith(" ")) {index++;} arg = arg.substring(index);
		//confirms 3rd argument
		if(arg.indexOf(" ") != -1) {
			//2nd argument is a url, use it for the url
			if(arg.startsWith("https://www.nitrotype.com/racer/"))
				url = arg.substring(0, arg.indexOf(" "));
			else if(arg.startsWith("www.nitrotype.com/racer/"))
				url = "https://" + arg.substring(0, arg.indexOf(" "));
			else 
				url = "https://www.nitrotype.com/racer/" + arg.substring(0, arg.indexOf(" "));
			
			index = 0;
			arg = arg.substring(arg.indexOf(" "));
			//remove leading spaces
			while(arg.substring(index).startsWith(" ")) {index++;} arg = arg.substring(index);
			if(arg.contains("."))
				bonus = (long)(Double.parseDouble(arg) + 0.5);
			else bonus = Long.parseLong(arg);
		} else {
			if(arg.startsWith("https://www.nitrotype.com/racer/"))
				url = arg;
			else if(arg.startsWith("www.nitrotype.com/racer/"))
				url = "https://" + arg;
			else url = "https://www.nitrotype.com/racer/" + arg;
		}
		
		System.setProperty("http.agent", "Chrome");
		URL u = new URL(url);
		HttpURLConnection c = (HttpURLConnection) u.openConnection();
		c.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner s = new Scanner(u.openStream());
		
		String racerInfo = "";
		while(s.hasNext())
			if((racerInfo = s.nextLine()).contains("RACER_INFO"))
				break;
		s.close();
		
		int gold = 0;
		if(racerInfo.substring((index = racerInfo.indexOf("membership") + 13), index + racerInfo.substring(index).indexOf("\"")).equals("gold"))			
			gold = 1;
		
		String team = racerInfo.substring(index = racerInfo.indexOf("tag") + 5, index + racerInfo.substring(index).indexOf(","));
		
		String displayName = racerInfo.substring((index = racerInfo.indexOf("displayName") + 14), index + racerInfo.substring(index).indexOf(",") - 1);
		if(displayName.equals("") || displayName.equals("ul"))
			displayName = racerInfo.substring((index = racerInfo.indexOf("username") + 11), index + racerInfo.substring(index).indexOf("\""));
		for(int i = 0; i < displayName.length() - 1; i++) {
			//System.out.println(displayName);
			if(displayName.charAt(i) == '\\')
				if(displayName.charAt(i + 1) == '\\') {
					if(i + 2 < displayName.length())
						displayName = displayName.substring(0, i + 1) + displayName.substring(i + 3);
					else displayName = displayName.substring(0, i + 1);
				} 
				else if(displayName.charAt(i + 1) == '/')
					displayName = displayName.substring(0, i) + displayName.substring(i + 1);
				else if(displayName.charAt(i + 1) == 'u')
					displayName = displayName.substring(0, i) + (char)(Integer.parseInt(displayName.substring(i + 2, i + 6), 16)) + displayName.substring(i + 6);
		} //System.out.println(displayName);
		int experience = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("experience") + 12), index + racerInfo.substring(index).indexOf(',')));
		
		int money = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("money") + 7), index + racerInfo.substring(index).indexOf(',')));
		
		int nitros = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("nitros") + 8), index + racerInfo.substring(index).indexOf(',')));
		
		int races = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("racesPlayed") + 13), index + racerInfo.substring(index).indexOf(',')));
		int placed1 = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("placed1") + 9), index + racerInfo.substring(index).indexOf(',')));
		int placed2 = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("placed2") + 9), index + racerInfo.substring(index).indexOf(',')));
		int placed3 = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("placed3") + 9), index + racerInfo.substring(index).indexOf(',')));
		
		int session = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("longestSession") + 16), index + racerInfo.substring(index).indexOf(',')));
		
		double age = ((System.currentTimeMillis() / 1000) - Long.parseLong(racerInfo.substring((index = racerInfo.indexOf("createdStamp") + 14), 
				index + racerInfo.substring(index).indexOf(',')))) / 31536000.0;
		
		long highwpm = (long)(50000 * Math.pow(2, (Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("highestSpeed") + 14), index + racerInfo.substring(index).indexOf(','))) - 40) / 20.56)) - 12981;
		
		u = new URL("https://www.nitrotype.com/index/605/bootstrap.js");
		HttpURLConnection d = (HttpURLConnection) u.openConnection();
		d.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner t = new Scanner(u.openStream());
		
		String carsInfo = "";
		while(t.hasNext())
			if((carsInfo = t.nextLine()).contains("CARS"))
				break;
		t.close();
		carsInfo = carsInfo.substring(index = carsInfo.indexOf("data.push(['CARS'") + 13, index + carsInfo.substring(index).indexOf("]]);"));
		
		int size = Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")));
		int[] carValues = new int[size];
		index = 0;
		for(int i = 0; i < size; i++) {	
			index += carsInfo.substring(index + 1).indexOf("{") + 1;
			carValues[Integer.parseInt(carsInfo.substring(index + 9, index + carsInfo.substring(index).indexOf(','))) - 1] 
					= Integer.parseInt(carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7, 
							index + carsInfo.substring(index).indexOf("price") + 7 + carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7).indexOf(",")));
		}
		
		byte[] cars = new byte[Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")))];
		index = racerInfo.indexOf("cars") + 8;
		while(racerInfo.substring(index - 1).indexOf('[') < racerInfo.substring(index).indexOf("profileView")) {
			if(racerInfo.substring(index + racerInfo.substring(index).indexOf('\"') + 1, index + racerInfo.substring(index).indexOf('\"') + 6).equals("owned"))
				cars[Integer.parseInt(racerInfo.substring(index, index + racerInfo.substring(index).indexOf(','))) - 1] = 1;
			else if(racerInfo.substring(index + racerInfo.substring(index).indexOf('\"') + 1, index + racerInfo.substring(index).indexOf('\"') + 5).equals("sold"))
				cars[Integer.parseInt(racerInfo.substring(index, index + racerInfo.substring(index).indexOf(','))) - 1] = 2;
			index += racerInfo.substring(index).indexOf('[') + 1;
		}
		
		int liquidCars = 0, subjectiveCars = 0;
		long lastDate = 0;
		for(int i = 0; i < cars.length; i++) {
			lastDate = RankCars.lastObtainableDate(i + 1);
			if(cars[i] == 1) {	//Car is owned
				liquidCars += carValues[i] * 0.6;
				subjectiveCars += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
			} else if (cars[i] == 2 && 1.3219280949 < (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0) {	//Car is sold, but obtainable
				subjectiveCars += carValues[i] * (0.4 * Math.pow(2.0, ((System.currentTimeMillis() / 1000) - lastDate) / 31536000.0) - 1);
			}
		}
		
		long liquid = money + nitros * 500 + liquidCars; 
		long subjective = (long)(gold * 10000000 + Math.pow(2.0, age) * 50000 + experience + races * 750 + placed1 * 750 + placed2 * 500 + placed3 * 250 + session * 2000 + subjectiveCars + highwpm);
		
		if(team.contains("\""))
			displayName = "[" + team.substring(1, team.length() - 1) + "]" + displayName;
		
		EmbedBuilder embed = new EmbedBuilder().setTitle("__**Account Value**__:").setDescription("**" + moneyToText(liquid + subjective) + "**")
				.setAuthor(displayName, url, "")
				.setColor(Color.PINK)
				.addInlineField("Liquid", "**" + fractionToText(liquid, liquid + subjective) + "**")
				.addInlineField("Subjective", "**" +fractionToText(subjective, liquid + subjective) + "**")
				.addField("__**Liquid Value**__:", "**" + moneyToText(liquid) + "**")
				.addInlineField("Cash", "**" + fractionToText(money, liquid) + "**" + " (" + moneyToText(money) + ")")
				.addInlineField("Nitros", "**" + fractionToText(nitros * 500, liquid) + "**" + " (" + moneyToText(nitros * 500) + ")")
				.addInlineField("Cars", "**" + fractionToText(liquidCars, liquid) + "**" + " (" + moneyToText(liquidCars) + ")")
				.addField("__**Subjective Value**__:", "**" + moneyToText(subjective) + "**");
		if(gold != 0)
			embed.addInlineField("Gold", "**" + fractionToText(gold * 10000000, subjective) + "**" + " (" + moneyToText(gold * 10000000) + ")").setColor(new Color(0xFFD700));
		embed.addInlineField("Age", "**" + fractionToText(Math.pow(2.0, age) * 50000, subjective) + "**" + " (" + moneyToText((long)(Math.pow(2.0, age) * 50000)) + ")")
				.addInlineField("Experience", "**" + fractionToText(experience, subjective) + "**" + " (" + moneyToText(experience) + ")")
				.addInlineField("Races", "**" + fractionToText(races * 750 + placed1 * 750 + placed2 * 500 + placed3 * 250, subjective) + "**" + " (" + moneyToText(races * 750 + placed1 * 750 + placed2 * 500 + placed3 * 250) + ")")
				.addInlineField("Highest Speed", "**" + fractionToText(highwpm, subjective) + "**" + " (" + moneyToText(highwpm) + ")")
				.addInlineField("Longest Session", "**" + fractionToText(session * 2000, subjective) + "**" + " (" + moneyToText(session * 2000) + ")")		
				.addInlineField("Cars", "**" + fractionToText(subjectiveCars, subjective) + "**" + " (" + moneyToText(subjectiveCars) + ")");
		if(bonus !=0)
			embed.addInlineField("Bonus", "**" +fractionToText(bonus, subjective) + "**" + " (" + moneyToText(bonus) + ")");
		return embed;
	}

	public static EmbedBuilder main(String arg1, String arg2) throws Exception {
		
		String url1 = "", url2 = "";
		int index = 0;
		//removes leading spaces
		while(arg1.substring(index).startsWith(" ")) {index++;} arg1 = arg1.substring(index);
		//setup url from arg1 and arg2
		if(arg1.startsWith("https://www.nitrotype.com/racer/"))
			url1 = arg1;
		else if(arg1.startsWith("www.nitrotype.com/racer/"))
			url1 = "https://" + arg1;
		else url1 = "https://www.nitrotype.com/racer/" + arg1;
		
		if(arg2.startsWith("https://www.nitrotype.com/racer/"))
			url2 = arg2;
		else if(arg2.startsWith("www.nitrotype.com/racer/"))
			url2 = "https://" + arg2;
		else url2 = "https://www.nitrotype.com/racer/" + arg2;

		System.setProperty("http.agent", "Chrome");
		URL u = new URL(url1);
		HttpURLConnection c = (HttpURLConnection) u.openConnection();
		c.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner s = new Scanner(u.openStream());
		
		String racerInfo1 = "";
		while(s.hasNext())
			if((racerInfo1 = s.nextLine()).contains("RACER_INFO"))
				break;
		s.close();
		
		u = new URL(url2);
		HttpURLConnection c2 = (HttpURLConnection) u.openConnection();
		c2.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner s2 = new Scanner(u.openStream());
		
		String racerInfo2 = "";
		while(s2.hasNext())
			if((racerInfo2 = s2.nextLine()).contains("RACER_INFO"))
				break;
		s2.close();
		
		String team1 = racerInfo1.substring(index = racerInfo1.indexOf("tag") + 5, index + racerInfo1.substring(index).indexOf(","));
		String team2 = racerInfo2.substring(index = racerInfo2.indexOf("tag") + 5, index + racerInfo2.substring(index).indexOf(","));
		
		String displayName1 = racerInfo1.substring((index = racerInfo1.indexOf("displayName") + 14), index + racerInfo1.substring(index).indexOf(",") - 1);
		if(displayName1.equals("") || displayName1.equals("ul"))
			displayName1 = racerInfo1.substring((index = racerInfo1.indexOf("username") + 11), index + racerInfo1.substring(index).indexOf("\""));
		for(int i = 0; i < displayName1.length() - 1; i++) {
			if(displayName1.charAt(i) == '\\') {
				if(displayName1.charAt(i + 1) == '\\') {
					if(i + 2 < displayName1.length())
						displayName1 = displayName1.substring(0, i + 1) + displayName1.substring(i + 3);
					else displayName1 = displayName1.substring(0, i + 1);
				}
				else if(displayName1.charAt(i + 1) == 'u')
					displayName1 = displayName1.substring(0, i) + (char)(Integer.parseInt(displayName1.substring(i + 2, i + 6), 16)) + displayName1.substring(i + 6);
			}
			else if((displayName1.charAt(i) == '_') && (i == 0 || displayName1.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName1.length())
					displayName1 = "\\_" + displayName1.substring(i + 1);
				else if(i + 2 < displayName1.length())
					displayName1 = displayName1.substring(0, i) + "\\_" + displayName1.substring(i + 1);
				else displayName1 = displayName1.substring(0, i) + "\\_";
			}
			else if((displayName1.charAt(i) == '*') && (i == 0 || displayName1.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName1.length())
					displayName1 = "\\*" + displayName1.substring(i + 1);
				else if(i + 2 < displayName1.length())
					displayName1 = displayName1.substring(0, i) + "\\*" + displayName1.substring(i + 1);
				else displayName1 = displayName1.substring(0, i) + "\\*";
			}
			else if((displayName1.charAt(i) == '~') && (i == 0 || displayName1.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName1.length())
					displayName1 = "\\~" + displayName1.substring(i + 1);
				else if(i + 2 < displayName1.length())
					displayName1 = displayName1.substring(0, i) + "\\~" + displayName1.substring(i + 1);
				else displayName1 = displayName1.substring(0, i) + "\\~";
			}
			else if((displayName1.charAt(i) == '`') && (i == 0 || displayName1.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName1.length())
					displayName1 = "\\`" + displayName1.substring(i + 1);
				else if(i + 2 < displayName1.length())
					displayName1 = displayName1.substring(0, i) + "\\`" + displayName1.substring(i + 1);
				else displayName1 = displayName1.substring(0, i) + "\\`";
			}
		}
		
		String displayName2 = racerInfo2.substring((index = racerInfo2.indexOf("displayName") + 14), index + racerInfo2.substring(index).indexOf(",") - 1);
		if(displayName2.equals("") || displayName2.equals("ul"))
			displayName2 = racerInfo2.substring((index = racerInfo2.indexOf("username") + 11), index + racerInfo2.substring(index).indexOf("\""));
		for(int i = 0; i < displayName2.length() - 1; i++) {
			if(displayName2.charAt(i) == '\\') {
				if(displayName2.charAt(i + 1) == '\\') {
					if(i + 2 < displayName2.length())
						displayName2 = displayName2.substring(0, i + 1) + displayName2.substring(i + 3);
					else displayName2 = displayName2.substring(0, i + 1);
				}
				else if(displayName2.charAt(i + 1) == 'u')
					displayName2 = displayName2.substring(0, i) + (char)(Integer.parseInt(displayName2.substring(i + 2, i + 6), 16)) + displayName2.substring(i + 6);
			}
			else if((displayName2.charAt(i) == '_') && (i == 0 || displayName2.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName2.length())
					displayName2 = "\\_" + displayName2.substring(i + 1);
				else if(i + 2 < displayName2.length())
					displayName2 = displayName2.substring(0, i) + "\\_" + displayName2.substring(i + 1);
				else displayName2 = displayName2.substring(0, i) + "\\_";
			}
			else if((displayName2.charAt(i) == '*') && (i == 0 || displayName2.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName2.length())
					displayName2 = "\\*" + displayName2.substring(i + 1);
				else if(i + 2 < displayName2.length())
					displayName2 = displayName2.substring(0, i) + "\\*" + displayName2.substring(i + 1);
				else displayName2 = displayName2.substring(0, i) + "\\*";
			}
			else if((displayName2.charAt(i) == '~') && (i == 0 || displayName2.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName2.length())
					displayName2 = "\\~" + displayName2.substring(i + 1);
				else if(i + 2 < displayName2.length())
					displayName2 = displayName2.substring(0, i) + "\\~" + displayName2.substring(i + 1);
				else displayName2 = displayName2.substring(0, i) + "\\~";
			}
			else if((displayName2.charAt(i) == '`') && (i == 0 || displayName2.charAt(i - 1) != '\\')) {
				if(i == 0 && i + 2 < displayName2.length())
					displayName2 = "\\`" + displayName2.substring(i + 1);
				else if(i + 2 < displayName2.length())
					displayName2 = displayName2.substring(0, i) + "\\`" + displayName2.substring(i + 1);
				else displayName2 = displayName2.substring(0, i) + "\\`";
			}
		}
		
		long age1 = Long.parseLong(racerInfo1.substring((index = racerInfo1.indexOf("createdStamp") + 14), index + racerInfo1.substring(index).indexOf(',')));
		long age2 = Long.parseLong(racerInfo2.substring((index = racerInfo2.indexOf("createdStamp") + 14), index + racerInfo2.substring(index).indexOf(',')));
		
		int level1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("level") + 7), index + racerInfo1.substring(index).indexOf(',')));
		int experience1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("experience") + 12), index + racerInfo1.substring(index).indexOf(',')));
		int level2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("level") + 7), index + racerInfo2.substring(index).indexOf(',')));
		int experience2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("experience") + 12), index + racerInfo2.substring(index).indexOf(',')));
				
		int avgSpd1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("avgSpeed") + 10), index + racerInfo1.substring(index).indexOf(',')));
		int highSpd1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("highestSpeed") + 14), index + racerInfo1.substring(index).indexOf(',')));
		int avgSpd2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("avgSpeed") + 10), index + racerInfo2.substring(index).indexOf(',')));
		int highSpd2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("highestSpeed") + 14), index + racerInfo2.substring(index).indexOf(',')));
		
		//TODO accuracy1
		int typed1 = 0, typos1 = 0; 
		index = racerInfo1.indexOf("raceLogs");
		if(racerInfo1.substring(index).contains("typed"))
			for(int end = index + racerInfo1.substring(index).indexOf("}]"); racerInfo1.substring(index, end).contains("typed");) {
				typed1 += Integer.parseInt(racerInfo1.substring(index += racerInfo1.substring(index).indexOf("typed") + 7, index + racerInfo1.substring(index).indexOf(',')));
				if(racerInfo1.substring(index + racerInfo1.substring(index).indexOf("errs") + 6,
						index + racerInfo1.substring(index).indexOf("errs") + 6 + racerInfo1.substring(index + racerInfo1.substring(index).indexOf("errs") + 6).indexOf('}')).equals("null"));
				else typos1 += Integer.parseInt(racerInfo1.substring(index += racerInfo1.substring(index).indexOf("errs") + 6, index + racerInfo1.substring(index).indexOf('}')));
		}
		double accuracy1 = 0.0;
		if(typed1 - typos1 != 0)
			accuracy1 = (int)((typed1 - typos1) * 10000.0) / typed1 / 100.0;
		
		int typed2 = 0, typos2 = 0; 
		index = racerInfo2.indexOf("raceLogs");
		if(racerInfo2.substring(index).contains("typed"))
			for(int end = index + racerInfo2.substring(index).indexOf("}]"); racerInfo2.substring(index, end).contains("typed");) {
				typed2 += Integer.parseInt(racerInfo2.substring(index += racerInfo2.substring(index).indexOf("typed") + 7, index + racerInfo2.substring(index).indexOf(',')));
				//TODO stop the index from incrementing if the if statement is false
				if(racerInfo2.substring(index + racerInfo2.substring(index).indexOf("errs") + 6,
						index + racerInfo2.substring(index).indexOf("errs") + 6 + racerInfo2.substring(index + racerInfo2.substring(index).indexOf("errs") + 6).indexOf('}')).equals("null"));
				else typos2 += Integer.parseInt(racerInfo2.substring(index += racerInfo2.substring(index).indexOf("errs") + 6, index + racerInfo2.substring(index).indexOf('}')));
			}
		double accuracy2 = 0.0; 
		if(typed2 - typos2 != 0)
			accuracy2 = (int)((typed2 - typos2) * 10000.0) / typed2 / 100.0;
		
		int races1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("racesPlayed") + 13), index + racerInfo1.substring(index).indexOf(',')));
		int placed11 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("placed1") + 9), index + racerInfo1.substring(index).indexOf(',')));
		int placed21 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("placed2") + 9), index + racerInfo1.substring(index).indexOf(',')));
		int placed31 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("placed3") + 9), index + racerInfo1.substring(index).indexOf(',')));
		int session1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("longestSession") + 16), index + racerInfo1.substring(index).indexOf(',')));
		
		int races2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("racesPlayed") + 13), index + racerInfo2.substring(index).indexOf(',')));
		int placed12 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("placed1") + 9), index + racerInfo2.substring(index).indexOf(',')));
		int placed22 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("placed2") + 9), index + racerInfo2.substring(index).indexOf(',')));
		int placed32 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("placed3") + 9), index + racerInfo2.substring(index).indexOf(',')));
		int session2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("longestSession") + 16), index + racerInfo2.substring(index).indexOf(',')));
		
		int money1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("money") + 7), index + racerInfo1.substring(index).indexOf(',')));
		int money2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("money") + 7), index + racerInfo2.substring(index).indexOf(',')));
		
		int achPts1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("achievementPoints") + 19), index + racerInfo1.substring(index).indexOf(',')));
		int achPts2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("achievementPoints") + 19), index + racerInfo2.substring(index).indexOf(',')));
		
		int nitros1 = Integer.parseInt(racerInfo1.substring((index = racerInfo1.indexOf("nitros") + 8), index + racerInfo1.substring(index).indexOf(',')));		
		int gold1 = 0;
		if(racerInfo1.substring((index = racerInfo1.indexOf("membership") + 13), index + racerInfo1.substring(index).indexOf("\"")).equals("gold"))			
			gold1 = 1;
		int nitros2 = Integer.parseInt(racerInfo2.substring((index = racerInfo2.indexOf("nitros") + 8), index + racerInfo2.substring(index).indexOf(',')));		
		int gold2 = 0;
		if(racerInfo2.substring((index = racerInfo2.indexOf("membership") + 13), index + racerInfo2.substring(index).indexOf("\"")).equals("gold"))			
			gold2 = 1;
		
		u = new URL("https://www.nitrotype.com/index/605/bootstrap.js");
		HttpURLConnection c3 = (HttpURLConnection) u.openConnection();
		c3.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner s3 = new Scanner(u.openStream());
		
		String carsInfo = "";
		while(s3.hasNext())
			if((carsInfo = s3.nextLine()).contains("CARS"))
				break;
		s3.close();
		carsInfo = carsInfo.substring(index = carsInfo.indexOf("data.push(['CARS'") + 13, index + carsInfo.substring(index).indexOf("]]);"));
		
		int size = Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")));
		int[] carValues = new int[size];
		index = 0;
		for(int i = 0; i < size; i++) {	
			index += carsInfo.substring(index + 1).indexOf("{") + 1;
			carValues[Integer.parseInt(carsInfo.substring(index + 9, index + carsInfo.substring(index).indexOf(','))) - 1] 
					= Integer.parseInt(carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7, 
							index + carsInfo.substring(index).indexOf("price") + 7 + carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7).indexOf(",")));
		}
		
		byte[] cars1 = new byte[Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")))];
		index = racerInfo1.indexOf("cars") + 8;
		while(racerInfo1.substring(index - 1).indexOf('[') < racerInfo1.substring(index).indexOf("profileView")) {
			if(racerInfo1.substring(index + racerInfo1.substring(index).indexOf('\"') + 1, index + racerInfo1.substring(index).indexOf('\"') + 6).equals("owned"))
				cars1[Integer.parseInt(racerInfo1.substring(index, index + racerInfo1.substring(index).indexOf(','))) - 1] = 1;
			else if(racerInfo1.substring(index + racerInfo1.substring(index).indexOf('\"') + 1, index + racerInfo1.substring(index).indexOf('\"') + 5).equals("sold"))
				cars1[Integer.parseInt(racerInfo1.substring(index, index + racerInfo1.substring(index).indexOf(','))) - 1] = 2;
			index += racerInfo1.substring(index).indexOf('[') + 1;
		}
		byte[] cars2 = new byte[Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")))];
		index = racerInfo2.indexOf("cars") + 8;
		while(racerInfo2.substring(index - 1).indexOf('[') < racerInfo2.substring(index).indexOf("profileView")) {
			if(racerInfo2.substring(index + racerInfo2.substring(index).indexOf('\"') + 1, index + racerInfo2.substring(index).indexOf('\"') + 6).equals("owned"))
				cars2[Integer.parseInt(racerInfo2.substring(index, index + racerInfo2.substring(index).indexOf(','))) - 1] = 1;
			else if(racerInfo2.substring(index + racerInfo2.substring(index).indexOf('\"') + 1, index + racerInfo2.substring(index).indexOf('\"') + 5).equals("sold"))
				cars2[Integer.parseInt(racerInfo2.substring(index, index + racerInfo2.substring(index).indexOf(','))) - 1] = 2;
			index += racerInfo2.substring(index).indexOf('[') + 1;
		}
		
		int liquidCars1 = 0, subjectiveCars1 = 0, numCars1 = 0;
		long lastDate = 0;
		for(int i = 0; i < cars1.length; i++) {
			lastDate = RankCars.lastObtainableDate(i + 1);
			if(cars1[i] == 1) {	//Car is owned
				liquidCars1 += carValues[i] * 0.6;
				subjectiveCars1 += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
				numCars1++;
			} else if (cars1[i] == 2 && 1.3219280949 < (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0) {	//Car is sold, but obtainable
				subjectiveCars1 += carValues[i] * (0.4 * Math.pow(2.0, ((System.currentTimeMillis() / 1000) - lastDate) / 31536000.0) - 1);
			}
		}
		int liquidCars2 = 0, subjectiveCars2 = 0, numCars2 = 0;
		for(int i = 0; i < cars2.length; i++) {
			lastDate = RankCars.lastObtainableDate(i + 1);
			if(cars2[i] == 1) {	//Car is owned
				liquidCars2 += carValues[i] * 0.6;
				subjectiveCars2 += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
				numCars2++;
			} else if (cars2[i] == 2 && 1.3219280949 < (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0) {	//Car is sold, but obtainable
				subjectiveCars2 += carValues[i] * (0.4 * Math.pow(2.0, ((System.currentTimeMillis() / 1000) - lastDate) / 31536000.0) - 1);
			}
		}
		
		if(team1.contains("\""))
			displayName1 = "[" + team1.substring(1, team1.length() - 1) + "]" + displayName1;
		if(team2.contains("\""))
			displayName2 = "[" + team2.substring(1, team2.length() - 1) + "]" + displayName2;
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy MMMM d");
		//LocalDateTime now = LocalDateTime.now();
		LocalDateTime dateStamp1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(age1 * 1000), ZoneId.systemDefault());
		LocalDateTime dateStamp2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(age2 * 1000), ZoneId.systemDefault());
		
		long timeDiff = Math.abs(age1 - age2);
		long years = timeDiff / 31557600, days = timeDiff % 31557600 / 86400;
		String yearStr, dayStr;
		if(years == 0)
			yearStr = "";
		else if(years == 1)
			yearStr = "1 year ";
		else yearStr = years + " years ";
		if(days == 0)
			dayStr = "";
		else if(days == 1)
			dayStr = "1 day ";
		else dayStr = days + " days ";
				
		long liquid1 = money1 + nitros1 * 500 + liquidCars1;
		long subjective1 = (long)(gold1 * 10000000 + Math.pow(2.0, ((System.currentTimeMillis() / 1000) - age1) / 31536000.0) * 50000 + experience1 + races1 * 750 + placed11 * 750 + placed21 * 500 + placed31 * 250 + session1 * 2000 + subjectiveCars1);
		long liquid2 = money2 + nitros2 * 500 + liquidCars2; 
		long subjective2 = (long)(gold2 * 10000000 + Math.pow(2.0, ((System.currentTimeMillis() / 1000) - age2) / 31536000.0) * 50000 + experience2 + races2 * 750 + placed12 * 750 + placed22 * 500 + placed32 * 250 + session2 * 2000 + subjectiveCars2);
		
		// TODO Auto-generated method stub
		return new EmbedBuilder().setTitle("Comparison")
				//.setDescription("[" + displayName1 + "](" + url1 + ") and [" + displayName2 + "](" + url2 + ")")
				.setColor(Color.PINK)
				.addInlineField(displayName1,
						"**Date Created**: " + dtf.format(dateStamp1) 
						+ "\n**Level**: " + numberToText(level1)
						+ "\n**Experience**: " + numberToText(experience1)
						+ "\n**Average Speed**: " + avgSpd1 + " wpm"
						+ "\n**Highest Speed**: " + highSpd1 + " wpm"
						+ "\n**Accuracy**: " + accuracy1 + "%"
						+ "\n**Total Races**: " + numberToText(races1) + plural(" race", races1)//race1Str
						+ "\n**Longest Session**: " + numberToText(session1) + plural(" race", session1)
						+ "\n**Money**: " + moneyToText(money1)
						+ "\n**Account Value**: " + moneyToText(liquid1 + subjective1)
						+ "\n**Number of Cars**: " + numCars1 + plural(" car", numCars1)//car1Str
						+ "\n**Achievement Points**: " + numberToText(achPts1)
						+ "\n[🔗Profile](" + url1 + ")")
				.addInlineField(displayName2,
						"**Date Created**: " + dtf.format(dateStamp2) 
						+ "\n**Level**: " + numberToText(level2)
						+ "\n**Experience**: " + numberToText(experience2)
						+ "\n**Average Speed**: " + avgSpd2 + " wpm"
						+ "\n**Highest Speed**: " + highSpd2 + " wpm"
						+ "\n**Accuracy**: " + accuracy2 + "%"
						+ "\n**Total Races**: " + numberToText(races2) + plural(" race", races2)
						+ "\n**Longest Session**: " + numberToText(session2) + plural(" race", session2)
						+ "\n**Money**: " + moneyToText(money2)
						+ "\n**Account Value**: " + moneyToText(liquid2 + subjective2)
						+ "\n**Number of Cars**: " + numCars2 + plural(" car", numCars2)
						+ "\n**Achievement Points**: " + numberToText(achPts2)
						+ "\n[🔗Profile](" + url2 + ")")
				.addField("Difference", "```diff"
						+ "\n" + positiveOrNegative(age2, age1) + yearStr + dayStr//Age
						+ "\n" + positiveOrNegative(level1, level2) + numberToText(Math.abs(level1 - level2)) + plural(" level", level1 - level2)//Level
						+ "\n" + positiveOrNegative(experience1, experience2) + numberToText(Math.abs(experience1 - experience2)) + plural(" experience point", experience1 - experience2)//Experience
						+ "\n" + positiveOrNegative(avgSpd1, avgSpd2) + numberToText(Math.abs(avgSpd1 - avgSpd2)) + " average wpm"//Avg Speed
						+ "\n" + positiveOrNegative(highSpd1, highSpd2) + numberToText(Math.abs(highSpd1 - highSpd2)) + " highest wpm"//Highest Speed
						+ "\n" + positiveOrNegative(accuracy1, accuracy2) + Math.abs((int)(100 * (accuracy1 - accuracy2)) / 100.0) + "% accuracy"//Accuracy
						+ "\n" + positiveOrNegative(races1, races2) + numberToText(Math.abs(races1 - races2)) + plural(" total race", races1 - races2)//Total Races
						+ "\n" + positiveOrNegative(session1, session2) + numberToText(Math.abs(session1 - session2)) + plural(" session race", session1 - session2)//Longest Session
						+ "\n" + positiveOrNegative(money1, money2) + moneyToText(Math.abs(money1 - money2)) + " money"//Money
						+ "\n" + positiveOrNegative(liquid1 + subjective1, liquid2 + subjective2) + moneyToText(Math.abs(liquid1 + subjective1 - (liquid2 + subjective2))) + " account value"//Account Value
						+ "\n" + positiveOrNegative(numCars1, numCars2) + numberToText(Math.abs(numCars1 - numCars2)) + plural(" car", numCars1 - numCars2)//Number of Cars
						+ "\n" + positiveOrNegative(achPts1, achPts2) + numberToText(Math.abs(achPts1 - achPts2)) + plural(" achievement point", achPts1 - achPts2)//Achievement points
						+ "```")
		;				
			
				//"Test " + arg1 + " " + arg2);
	}
	
	

	private static String plural(String unit, int value) {
		if(Math.abs(value) != 1)
			unit += 's';
		return unit;
	}

	public static String fractionToText(long nominator, long denominator) {
		return (long)(10000.0 * nominator / denominator) / 100.0 + "%";
	}
	
	public static String fractionToText(double nominator, long denominator) {
		return (long)(10000.0 * nominator / denominator) / 100.0 + "%";
	}

	public static String moneyToText(long amount) {
		String money = "" + amount;
		String text = "";
		for(int i = 0; i < money.length(); i++) {
			text = money.substring(money.length() - i - 1, money.length() - i) + text;
			if(i != money.length() - 1 && (i + 1) % 3 == 0)
				text = ',' + text;
		}
		return "$" + text;
	}
	public static String numberToText(long amount) {
		String money = "" + amount;
		String text = "";
		for(int i = 0; i < money.length(); i++) {
			text = money.substring(money.length() - i - 1, money.length() - i) + text;
			if(i != money.length() - 1 && (i + 1) % 3 == 0)
				text = ',' + text;
		}
		return text;
	}
	public static String positiveOrNegative(long one, long two) {
		if(one > two)
			return "+";
		else if(one < two)
			return "-";
		else return "";
	}
	private static String positiveOrNegative(int one, int two) {
		if(one > two)
			return "+";
		else if(one < two)
			return "-";
		else return "";
	}
	private static String positiveOrNegative(double one, double two) {
		if(one > two)
			return "+";
		else if(one < two)
			return "-";
		else return "";
	}
}
