import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import java.util.Scanner;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.entities.ChannelType;

public class XkcdBot extends ListenerAdapter{
	public static void main(String[] args){
		try{
			Logger.log("Logging in...");
			JDA jda = JDABuilder.createDefault(args[0])			// The token of the account that is logging in.
				.addEventListeners(new Listener())	// An instance of a class that will handle events.
	 			.build();

			jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.

			Logger.say("Login successful!");
		}catch(ArrayIndexOutOfBoundsException e){
			Logger.error("Please specify token.");
			Logger.say("USAGE: XkcdBot token [verbose|silent]");
			System.exit(1);
		}catch(LoginException e){
			Logger.error("Error logging in: \n");
			e.printStackTrace();
			System.exit(2);
		}catch(InterruptedException e){
			Logger.error("Login error: \n");
			e.printStackTrace();
			System.exit(3);
		}

		try{
			if(args[1].equals("verbose")){
				Logger.say("Running in verbose mode. All messages will be displayed.");
				Logger.verbose = true;
			}else if(args[1].equals("silent")){
				Logger.say("Running in silent mode. No messages will be displayed.");
				Logger.verbose = false;
			}
		}catch(ArrayIndexOutOfBoundsException e){
			Logger.say("Running in silent mode. Add 'verbose' to see all messages.");
		}
	}
}


class Listener extends ListenerAdapter{
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		JDA jda = event.getJDA();
		long responseNumber = event.getResponseNumber();

		User author = event.getAuthor();
		Message message = event.getMessage();
		MessageChannel channel = event.getChannel();
		String msg = message.getContentDisplay();

		if(author.isBot()){
			return;
		}

		if(msg.startsWith("!xkcd")){
			Logger.log(author.getName() + " from " + channel.getName() + " requested: " + msg);
			String url;
			try{
				url = "https://xkcd.com/" + Integer.valueOf(msg.replaceAll("!xkcd", "").strip()) ;
			}catch(NumberFormatException e){
				if(msg.equals("!xkcd")){
					url = "https://xkcd.com/";
				}else{
					channel.sendMessage("Invalid comic ID. Should be a number. :frowning:").queue();
					return;
				}
			}
			try{
				Comic c = new Comic(url);
				channel.sendMessage(c.toEmbedBuilder().build()).queue();
			}catch(IOException e){
				channel.sendMessage("Problem getting data from XKCD.").queue();
			}
		}else if(msg.startsWith("https://xkcd.com/")){
			Logger.log(author.getName() + " from " + channel.getName() + " requested: " + msg);
			try{
				Comic c = new Comic(msg);
				channel.sendMessage(c.toEmbedBuilder().build()).queue();
				if(channel.getType()==ChannelType.TEXT){
					message.delete().queue();
				}else{
					Logger.log("Will not attempt to delete original message from non-text channel");
				}
			}catch(IOException e){
				channel.sendMessage("Problem getting data from XKCD.");
			}catch(InsufficientPermissionException e){
				Logger.log("Original message not deleted due to lack of permissions.");
			}
		}else if(msg.equals("!ping")){
			channel.sendMessage("pong!").queue();
		}
	}

}


class Logger {
	static boolean verbose = false;

	protected static void log(String msg){
		if(verbose){
			System.out.println(msg);
		}
	}

	protected static void say(String msg){
		System.out.println(msg);
	}

	protected static void error(String msg){
		System.out.println("ERROR: " + msg);
	}

	protected static void warn(String msg){
		System.out.println("WARN: " + msg);
	}
}

class Comic{

	HashMap<String, String> values = new HashMap<>();

	public Comic(String url) throws IOException{
		try{
			String s = request(url+ "/info.0.json");
			this.values = new ObjectMapper().readValue(s, HashMap.class);
		}catch(JsonProcessingException e){
			Logger.warn("Error parsing data from XKCD!");
		}
		Logger.log("Serving comic " + String.valueOf(this.values.get("num")));
	}

	String request(String url) throws IOException, MalformedURLException{
		Scanner scanner = new Scanner(new URL(url).openStream());
		scanner.useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}


	public String toString(){
		return String.format("**%s**\n%s\n||%s||\n", this.values.get("title"), this.values.get("img"), this.values.get("alt"));
	}

	public EmbedBuilder toEmbedBuilder(){
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle(this.values.get("title"), this.getURL());
		embed.setImage(this.values.get("img"));
		embed.addField(new MessageEmbed.Field(this.getDate(), "||" + this.values.get("alt") + "||", true));
		return embed;
	}

	String getURL(){
		return "https://xkcd.com/" + String.valueOf(this.values.get("num"));
	}

	String getDate(){
		return this.values.get("day") + "." + this.values.get("month") + "." + this.values.get("year");
	}

}
