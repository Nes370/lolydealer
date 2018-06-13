package com.github.nes370.lolydealer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Evaluate {

	public static String main(String arg) throws Exception {
		
		int bonus = 0;
		String url = "";
		if(arg.indexOf(" ") != -1) {
			url = "https://www.nitrotype.com/racer/" + arg.substring(0, arg.indexOf(" "));
			bonus = Integer.parseInt(arg.substring(arg.indexOf(" ") + 1));
		} else url = "https://www.nitrotype.com/racer/" + arg;
		
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
		
		int index, gold = 0;
		if(racerInfo.substring((index = racerInfo.indexOf("membership") + 13), index + racerInfo.substring(index).indexOf("\"")).equals("gold"))			
			gold = 1;
		
		String displayName = racerInfo.substring((index = racerInfo.indexOf("displayName") + 14), index + racerInfo.substring(index).indexOf("\""));
		if(displayName.equals("") || displayName.equals("ull,"))
			displayName = racerInfo.substring((index = racerInfo.indexOf("username") + 11), index + racerInfo.substring(index).indexOf("\""));
		for(int i = 0; i < displayName.length(); i++)
			if(displayName.charAt(i) == '\\' && displayName.charAt(i+1) == 'u')
				displayName = displayName.substring(0, i) + (char)(Integer.parseInt(displayName.substring(i + 2, i + 6), 16)) + displayName.substring(i + 6);
		
		int experience = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("experience") + 12), index + racerInfo.substring(index).indexOf(',')));
		
		int money = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("money") + 7), index + racerInfo.substring(index).indexOf(',')));
		
		int nitros = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("nitros") + 8), index + racerInfo.substring(index).indexOf(',')));
		
		int races = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("racesPlayed") + 13), index + racerInfo.substring(index).indexOf(',')));
		
		int session = Integer.parseInt(racerInfo.substring((index = racerInfo.indexOf("longestSession") + 16), index + racerInfo.substring(index).indexOf(',')));
		
		double age = ((System.currentTimeMillis() / 1000) - Long.parseLong(racerInfo.substring((index = racerInfo.indexOf("createdStamp") + 14), index + racerInfo.substring(index).indexOf(',')))) / 31536000.0;
		
		u = new URL("https://www.nitrotype.com/index/605/bootstrap.js");
		HttpURLConnection d = (HttpURLConnection) u.openConnection();
		d.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36");
		Scanner t = new Scanner(u.openStream());
		
		String carsInfo = "";
		while(t.hasNext())
			if((carsInfo = t.nextLine()).contains("CARS"))
				break;
		t.close();
		
		int size = Integer.parseInt(carsInfo.substring(carsInfo.lastIndexOf("id") + 4, carsInfo.lastIndexOf("}")));
		int[] carValues = new int[size];
		index = 0;
		for(int i = 0; i < size; i++) {	
			index += carsInfo.substring(index + 1).indexOf("{") + 1;
			carValues[Integer.parseInt(carsInfo.substring(index + 9, index + carsInfo.substring(index).indexOf(','))) - 1] 
					= Integer.parseInt(carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7, index + carsInfo.substring(index).indexOf("price") + 7 + carsInfo.substring(index + carsInfo.substring(index).indexOf("price") + 7).indexOf(",")));
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
			lastDate = 0;
			if(i == 70)	//2012 Xmaxx Event
				lastDate = 1386028800;
			else if(i == 83 || i == 84 || i == 86 || i == 87)	//2013 Summer Event
				lastDate = 1375833600;
			//2013 Halloween Event
			else if(i == 99 || i == 101)	//2013 Xmaxx Event
				lastDate = 1386028800;
			else if(i == 81 || i == 109)	//2014 Summer Event
				lastDate = 1408924800;
			else if(i == 97)	//2014 Halloween Event
				lastDate = 1414713600;
			else if(i == 98 || i == 112 || i == 113)	//2014 Winter Event
				lastDate = 1420070400;
			else if(i == 49 || i == 56)	//Popularity Contest ended by 2.0 update
				lastDate = 1430179200;
			else if(i == 82 || i == 108 || i == 114)	//2015 Summer Event
				lastDate = 1441065600;
			else if(i == 116)	//2015 Halloween Event
				lastDate = 1446336000;
			else if(i == 118 || i == 122)	//2015 Xmaxx Event
				lastDate = 1451779200;
			else if(i == 115 || i == 124 || i == 126 || i == 127)	//2016 Summer Event
				lastDate = 1471392000;
			//2016 Hallowampus Event
			else if(i == 69 || i == 102 || i == 112 || i == 119 || i == 121 || i == 131 || i == 132 || i == 135)	//2016 Xmaxx Event
				lastDate = 1483228800;
			else if(i == 80 || i == 125 || i == 137 || i == 138 || i == 139)	//2017 Summer Event
				lastDate = 1502496000;
			else if(i == 117 || i == 129 || i == 130 || i == 140)	//2017 Hallowampus Event
				lastDate = 1509494400;
			else if(i == 68 || i == 71 || i == 102 || i == 110 || i == 133 || i == 134 || i == 141 || i == 142 || i == 143 || i == 144 || i == 145)	//2017 Xmaxx Event
				lastDate = 1514851200;
			else if(i == 148 || i == 149 || i == 150 || i == 151)	//2018 Spring Fever Event
				lastDate = 1522540800;
			else if(i == 153 || i == 154)	//2018 PAC Event
				lastDate = 1526601600;
			else lastDate = System.currentTimeMillis() / 1000;
			if(cars[i] == 1) {	//Car is owned
				liquidCars += carValues[i] * 0.6;
				subjectiveCars += carValues[i] * 0.4 * Math.pow(2.0, (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0);
			} else if (cars[i] == 2 && 1.3219280949 < (System.currentTimeMillis() / 1000 - lastDate) / 31536000.0) {	//Car is sold, but obtainable
				subjectiveCars += carValues[i] * (0.4 * Math.pow(2.0, ((System.currentTimeMillis() / 1000) - lastDate) / 31536000.0) - 1);
			}
		}
		
		long liquid = money + nitros * 500 + liquidCars; 
		long subjective = (long)(gold * 10000000 + Math.pow(2.0, age) * 50000 + experience + races * 1000 + session * 2000 + subjectiveCars + bonus);
		if(bonus == 0)
			return "\n__**" + displayName + "**__: " + url 
				+ "\n__**Total Value**__: " + moneyToText(liquid + subjective)
				+ "\n	**Liquid**: " + fractionToText(liquid, liquid + subjective)
				+ "	**Subjective**: " + fractionToText(subjective, liquid + subjective)
				+ "\n__**Liquid**__: " + moneyToText(liquid)
				+ "\n	**Cash**: " + fractionToText(money, liquid)
				+ "	**Nitros**: " + fractionToText(nitros * 500, liquid)
				+ "\n	**Cars**: " + fractionToText(liquidCars, liquid)
				+ "\n__**Subjective**__: " + moneyToText(subjective)
				+ "\n	**Gold**: " + fractionToText(gold * 10000000, subjective)
				+ "	**Age**: " + fractionToText(Math.pow(2.0, age) * 50000, subjective)
				+ "\n	**Experience**: " + fractionToText(experience, subjective)
				+ "	**Races**: " + fractionToText(races * 1000, subjective)
				+ "\n	**Longest Session**: " + fractionToText(session * 2000, subjective)
				+ "	**Cars**: " + fractionToText(subjectiveCars, subjective);
		else return "\n__**" + displayName + "**__: " + url
				+ "\n__**Total Value**__: " + moneyToText(liquid + subjective)
				+ "\n	**Liquid**: " + fractionToText(liquid, liquid + subjective)
				+ "	**Subjective**: " + fractionToText(subjective, liquid + subjective)
				+ "\n__**Liquid**__: " + moneyToText(liquid)
				+ "\n	**Cash**: " + fractionToText(money, liquid)
				+ "	**Nitros**: " + fractionToText(nitros * 500, liquid)
				+ "\n	**Cars**: " + fractionToText(liquidCars, liquid)
				+ "\n__**Subjective**__: " + moneyToText(subjective)
				+ "\n	**Gold**: " + fractionToText(gold * 10000000, subjective)
				+ "	**Age**: " + fractionToText(Math.pow(2.0, age) * 50000, subjective)
				+ "\n	**Experience**: " + fractionToText(experience, subjective)
				+ "	**Races**: " + fractionToText(races * 1000, subjective)
				+ "\n	**Longest Session**: " + fractionToText(session * 2000, subjective)
				+ "	**Cars**: " + fractionToText(subjectiveCars, subjective)
				+ "\n	**Bonus**: " + fractionToText(bonus, subjective);
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
}
