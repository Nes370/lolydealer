package com.github.nes370.lolydealer;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NewsUpdate extends TimerTask {

	int count = 0;
	long blogID;
	long blogCommentID;
	JSONObject news;
	ServerTextChannel feed;
	
	public NewsUpdate (ServerTextChannel feed) {
		//read blogID and blogCommentID from file
		news = (JSONObject) Main.readJSON(Main.getResourcePath() + "news.json");
		blogID = (long) news.get("blogID");
		blogCommentID = (long) news.get("blogCommentID");
		this.feed = feed;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		System.out.print("News checked. ");
		boolean newPost = false;
	//	if(count++ % 6 == 0) {
			JSONArray posts = null;
			try {
				posts = getPosts();
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int i = 0; i < posts.size(); i++) {
				JSONObject post = (JSONObject) posts.get(i);
				if((long) post.get("blogID") > blogID) {
					blogID = (long) post.get("blogID");
					newPost = true;
				}
			}
	//	}
		
		//fetch news/blogID
		JSONObject postComments = null;
		try {
			postComments = getPostComments();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(newPost) {
			//create blog post embed
			EmbedBuilder postEmbed = new EmbedBuilder()
					.setAuthor((String) postComments.get("title"), "https://www.nitrotype.com/news/read/" + blogID + "/" + (String) postComments.get("slug"), "")
					.setDescription("Post by " + (String) postComments.get("writtenBy"))
					.setImage("https://www.nitrotype.com/uploads/newsimage/" + blogID + "/.png")
					.setFooter("News Post " + blogID)
					.setTimestamp(Instant.ofEpochMilli((long) postComments.get("createdStamp") * 1000))
					.setColor(Color.decode("#2f93db"));
			//2f93db
			try {
				feed.sendMessage(postEmbed).get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//JSONArray comments = (JSONArray) postComments.get("comments");
		long newestID = blogCommentID;
/*		for(int i = 0; i < comments.size(); i++) {
			JSONObject comment = (JSONObject) comments.get(i);
			long currentID = (long) comment.get("blogCommentID");
			if(currentID > blogCommentID) {
				if(currentID > newestID)
					newestID = currentID;
				String tag = (String) comment.get("tag");
				String username = (String) comment.get("username");
				String displayName = (String) comment.get("displayName");
				String title = (String) comment.get("title");
				String country = (String) comment.get("country");
				String gender = (String) comment.get("gender");
				long level = (long) comment.get("level");
				long avgSpeed = (long) comment.get("avgSpeed");
				long carHueAngle = (long) comment.get("carHueAngle");
				long carID = (long) comment.get("carID");
				boolean gold = ((String) comment.get("membership")).equals("gold");
				boolean admin = 1 == (long) comment.get("adminComment");
				boolean moderator = 1 == (long) comment.get("moderatorComment");
				String tagColor = (String) comment.get("tagColor");
				String text = (String) comment.get("comment");
				
				String author = "";
				String description = "";
				String thumbnail = "https://assets.nitrotype.com/cars/";
				if(tag != null && !tag.equals("")) {
					author += "[" + tag + "] ";
				}
				if(displayName != null && !displayName.equals("")) {
					author += displayName;
				} else author += username;
				if(title != null && !title.equals(""))
					author += "\n\"" + title + "\"";
				if(admin) {
					description += "**Administrator**\n";
				} else if(moderator) {
					description += "**Moderator**\n";
				}
				if(country != null && !country.equals("")) {
					description += ":flag_" + country.toLowerCase() + ": ";
				}
				if(gender != null && !gender.equals("")) {
					if(gender.equals("male"))
						description += "<:male:587018976635125771> ";
					else description += "<:female:587019025310154762> ";
				}
				if(carHueAngle != 0) {
					thumbnail += "painted/" + carID + "_small_1_" + carHueAngle;
				} else {
					thumbnail += carID + "_small_1";
				}
				description += "Level **" + level + "** Avg Speed **" + avgSpeed + "** wpm";
				text = convertMarkup(text);
				
				System.out.println(thumbnail);
				EmbedBuilder commentEmbed = new EmbedBuilder()
						.setAuthor(author, "https://www.nitrotype.com/racer/" + username, "")
						.setThumbnail(thumbnail)
						.setDescription(description)
						.setFooter((String) postComments.get("title"))
						.setTimestamp(Instant.ofEpochMilli((long) comment.get("createdStamp") * 1000));
				if(text.length() < 1024) {
					commentEmbed.addField("Comment " + (long) comment.get("blogCommentID"), text);
				} else {
					commentEmbed.addField("Comment " + (long) comment.get("blogCommentID"), text.substring(0, 1024));
					text = text.substring(1024);
					A: while(text.length() > 0) {
						if(text.length() > 1024) {
							commentEmbed.addField("᠎", text.substring(0, 1024)); //special space character in quotes
							text = text.substring(1024);
						} else {
							commentEmbed.addField("᠎", text);
							break A;
						}
					}
				}
				if(tagColor != null && !tagColor.equals("")) {
					commentEmbed.setColor(Color.decode("#" + tagColor));
				} else if(gold) {
					commentEmbed.setColor(new Color(0xFFD700));
				} else commentEmbed.setColor(Color.PINK);
				try {
					feed.sendMessage(commentEmbed).get();
				} catch (InterruptedException | ExecutionException e) {
					System.out.print(e.getStackTrace());
					e.printStackTrace();
				}
			}
		} */
		//for each comment, if greater than recorded
			//create a comment embed
		
		//write blogID and largest blogCommentID to news.json
		blogCommentID = newestID;
		news.put("blogID", blogID);
		news.put("blogCommentID", blogCommentID);
		FileWriter file;
		try {
			file = new FileWriter(Main.getResourcePath() + "news.json");
			file.write(news.toJSONString());
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*	private String convertMarkup(String text) {
		// TODO Auto-generated method stub
		text = text.replaceAll("\\*", "\\\\*")
				.replaceAll("_", "\\_")
				.replaceAll("~", "\\~")
				.replaceAll("`", "\\`")
				.replaceAll("\\|","\\\\|");
		String[] words = text.split("\\s+");
		String[] spaces = text.split("\\S+");
		String embedText = "";
		for(int i = 0; i < words.length || i < spaces.length; i++) {
			if(spaces.length > i)
				embedText += spaces[i];
			if(words.length > i) {
				if(words[i].startsWith("@")) {
					words[i] = "**" + words[i] + "**";
				} else if (words[i].contains("@")) {
					words[i] = words[i].substring(0, words[i].indexOf('@')) + "**" + words[i].substring(words[i].indexOf('@')) + "**";
				}
				embedText += words[i];
			}
		}
		return embedText;
	} */

	public JSONArray getPosts() throws IOException, ParseException {
		System.setProperty("http.agent", "Chrome");
		URL u = new URL("https://www.nitrotype.com/api/news");
		u.openConnection();
		Scanner s = new Scanner(u.openStream());
		String newsInfo = "";
		newsInfo = s.nextLine();
		s.close();
		return (JSONArray) ((JSONObject) new JSONParser().parse(newsInfo)).get("data");
	}
	
	public JSONObject getPostComments() throws IOException, ParseException {
		System.setProperty("http.agent", "Chrome");
		URL u = new URL("https://www.nitrotype.com/api/news/" + blogID);
		u.openConnection();
		Scanner s = new Scanner(u.openStream());
		String newsInfo = "";
		newsInfo = s.nextLine();
		s.close();
		return (JSONObject) ((JSONObject) new JSONParser().parse(newsInfo)).get("data");
	}
	
}
