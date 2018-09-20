package com.github.nes370.lolydealer;

import java.awt.Color;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class RankCars {
	
	public static EmbedBuilder main(String arg, int greg) throws Exception {
		
		int displayCars = 10;
		String url = "";
		int index = 0;
		//removes leading spaces
		while(arg.substring(index).startsWith(" ")) {index++;} arg = arg.substring(index);
		//confirms 3rd argument
		if(arg.indexOf(" ") != -1) {
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
				displayCars = (int)(Double.parseDouble(arg) + 0.5);
			else
				displayCars = Integer.parseInt(arg);
		} else {
			if(arg.startsWith("https://www.nitrotype.com/racer/"))
				url = arg;
			else if(arg.startsWith("www.nitrotype.com/racer/"))
				url = "https://" + arg;
			else url = "https://www.nitrotype.com/racer/" + arg;
		}
		
		if(displayCars > 25)
			displayCars = 25;
		
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
		
		boolean gold = false;
		if(racerInfo.substring((index = racerInfo.indexOf("membership") + 13), index + racerInfo.substring(index).indexOf("\"")).equals("gold"))			
			gold = true;
		
		index = 0;
		String displayName = racerInfo.substring((index = racerInfo.indexOf("displayName") + 14), index + racerInfo.substring(index).indexOf("\""));
		if(displayName.equals("") || displayName.equals("ull,"))
			displayName = racerInfo.substring((index = racerInfo.indexOf("username") + 11), index + racerInfo.substring(index).indexOf("\""));
		for(int i = 0; i < displayName.length() - 1; i++) {
			if(displayName.charAt(i) == '\\')
				if(displayName.charAt(i + 1) == '\\') {
					if(i + 2 < displayName.length())
						displayName = displayName.substring(0, i + 1) + displayName.substring(i + 3);
					else displayName = displayName.substring(0, i + 1);
				}
				else if(displayName.charAt(i + 1) == 'u')
					displayName = displayName.substring(0, i) + (char)(Integer.parseInt(displayName.substring(i + 2, i + 6), 16)) + displayName.substring(i + 6);
		}
		
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
		
		String[][] showcase = new String[displayCars][6];	
		//showcase[x][0] = "carId", showcase[x][1] = "carName", showcase [x][2] = "carLiquid", showcase [x][3] = "carSubjective", showcase [x][4] = "carTotal"
		long lastDate = 0;
		int tempLiq, tempSub, tempTot;
		for(int i = 0; i < cars.length; i++) {
			lastDate = 0;
			tempLiq = 0;
			tempSub = 0;
			tempTot = 0;
			lastDate = lastObtainableDate(i + 1);
			if(greg == 0) {
				if(cars[i] == 1) {	//Car is owned
					tempLiq += carValues[i] * 0.6;
					tempSub += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
					tempTot = tempLiq + tempSub;
					for(int j = 0; j < showcase.length; j++)
						if(showcase[j][4] == null || Integer.parseInt(showcase[j][4]) < tempTot ) {
							for(int k = showcase.length - 1; k > j; k--)
								for(int l = 0; l < showcase[k].length; l++)
									showcase[k][l] = showcase[k - 1][l];
							showcase[j][0] = "" + (i + 1);
							showcase[j][1] = carsInfo.substring((index = (index = carsInfo.indexOf("carID\":" + showcase[j][0])) + carsInfo.substring(index).indexOf("name") + 7), 
									index + carsInfo.substring(index).indexOf("\""));
							for(int m = 0; m < showcase[j][1].length(); m++)
								if(showcase[j][1].charAt(m) == '\\' && showcase[j][1].charAt(m + 1) == 'u')
									showcase[j][1] = showcase[j][1].substring(0, m) + (char)(Integer.parseInt(showcase[j][1].substring(m + 2, m + 6), 16)) + showcase[j][1].substring(m + 6);
							showcase[j][2] = "" + tempLiq;
							showcase[j][3] = "" + tempSub;
							showcase[j][4] = "" + tempTot;
							showcase[j][5] = "" + carValues[i];
							break;
						}					
				}
			} else if(greg == 1){
				if(cars[i] == 2) {	//Car is owned
					tempLiq += carValues[i] * 0.6;
					tempSub += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
					tempTot = tempLiq + tempSub;
					for(int j = 0; j < showcase.length; j++)
						if(showcase[j][4] == null || Integer.parseInt(showcase[j][4]) < tempTot ) {
							for(int k = showcase.length - 1; k > j; k--)
								for(int l = 0; l < showcase[k].length; l++)
									showcase[k][l] = showcase[k - 1][l];
							showcase[j][0] = "" + (i + 1);
							showcase[j][1] = carsInfo.substring((index = (index = carsInfo.indexOf("carID\":" + showcase[j][0])) + carsInfo.substring(index).indexOf("name") + 7), 
									index + carsInfo.substring(index).indexOf("\""));
							for(int m = 0; m < showcase[j][1].length(); m++)
								if(showcase[j][1].charAt(m) == '\\' && showcase[j][1].charAt(m + 1) == 'u')
									showcase[j][1] = showcase[j][1].substring(0, m) + (char)(Integer.parseInt(showcase[j][1].substring(m + 2, m + 6), 16)) + showcase[j][1].substring(m + 6);
							showcase[j][2] = "" + tempLiq;
							showcase[j][3] = "" + tempSub;	
							showcase[j][4] = "" + tempTot;
							showcase[j][5] = "" + carValues[i];
							break;
						}
				}
			}
		}
		String team = racerInfo.substring(index = racerInfo.indexOf("tag") + 5, index + racerInfo.substring(index).indexOf(","));
		if(team.contains("\""))
			displayName = "[" + team.substring(1, team.length() - 1) + "]" + displayName;
		EmbedBuilder embed = new EmbedBuilder()
				.setAuthor(displayName, url, "")
				.setColor(Color.PINK);
		if(greg == 0)
			embed.setTitle("__**Cars Owned by Rank**__");
		else if(greg == 1)
			embed.setTitle("__**Cars Sold by Rank**__");
		if(gold)
			embed.setColor(new Color(0xFFD700));
		
		for(int i = 0; i < showcase.length; i++)
			if(greg == 0 && showcase[i][1] != null)
				embed.addInlineField(i + 1 + ". " + showcase[i][1], "**Value** " + Evaluate.moneyToText(Integer.parseInt(showcase[i][4])) 
						+ "\n**Sell Price** " + Evaluate.moneyToText(Integer.parseInt(showcase[i][2]))
						+ "\n[🔗NT Wiki](https://nitro-type.wikia.com/wiki/" + spacesToUnderscores(showcase[i][1]) + ")");
			else if(greg == 1 && showcase[i][1] != null)
				embed.addInlineField(i + 1 + ". " + showcase[i][1] , "**Value** " + Evaluate.moneyToText(Integer.parseInt(showcase[i][4]))
						+ "\n**Buy Price** " + Evaluate.moneyToText(Integer.parseInt(showcase[i][5]))
						+ "\n[🔗NT Wiki](https://nitro-type.wikia.com/wiki/" + spacesToUnderscores(showcase[i][1]) + ")");
		return embed;

	}

	static long lastObtainableDate(int id) {
		if(id == 71)	//2012 Xmaxx Event
			return 1386028800;
		else if(id == 84 || id == 85 || id == 87 || id == 88)	//2013 Summer Event
			return 1375833600;
		//2013 Halloween Event
		else if(id == 100 || id == 102)	//2013 Xmaxx Event
			return 1386028800;
		else if(id == 82 || id == 110)	//2014 Summer Event
			return 1408924800;
		else if(id == 98)	//2014 Halloween Event
			return 1414713600;
		else if(id == 99 || id == 113 || id == 114)	//2014 Winter Event
			return 1420070400;
		else if(id == 50 || id == 57)	//Popularity Contest ended by 2.0 update
			return 1430179200;
		else if(id == 109)	//2015 Summer Event
			return 1441065600;
		else if(id == 117)	//2015 Halloween Event
			return 1446336000;
		else if(id == 119 || id == 123)	//2015 Xmaxx Event
			return 1451779200;
		else if(id == 116 || id == 125 || id == 127)	//2016 Summer Event
			return 1471392000;
		//2016 Hallowampus Event
		else if(id == 70 || id == 103 || id == 113 || id == 120 || id == 122 || id == 132 || id == 133 || id == 136)	//2016 Xmaxx Event
			return 1483228800;
		else if(id == 126 || id == 138 || id == 139 || id == 140)	//2017 Summer Event
			return 1502496000;
		else if(id == 118 || id == 130 || id == 131 || id == 141)	//2017 Hallowampus Event
			return 1509494400;
		else if(id == 69 || id == 72 || id == 103 || id == 111 || id == 134 || id == 135 || id == 142 || id == 143 || id == 144 || id == 145 || id == 146)	//2017 Xmaxx Event
			return 1514851200;
		else if(id == 149 || id == 150 || id == 151 || id == 152)	//2018 Spring Fever Event
			return 1522540800;
		else if(id == 154 || id == 155)	//2018 PAC Event
			return 1526601600;
		else if(id == 81 || id == 83 || id == 115 || id == 128 || id == 156 || id == 157 || id == 158 || id == 159)	//2018 Surf n' Turf Event
			return 1532044800;
		else return System.currentTimeMillis() / 1000;
	}

	private static String spacesToUnderscores(String string) {
		String str = "";
		for(int i = 0; i < string.length(); i++)
			if(string.substring(i).startsWith(" "))
				str += "_";
			else
				str += string.substring(i, i + 1);				
		return str;
	}

}