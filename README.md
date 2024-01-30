# JukeBot

JukeBot is a music bot for Discord, supporting a wide variety of audio sources,
with the goal of delivering high audio quality with an expansive feature set.

If your intention is to self-host, you're in luck! JukeBot is designed to consume as few resources as possible.
Keep reading to see how to set it up.

## Self-Hosting
First thing's first, you're going to need a server with [Java 13](https://adoptopenjdk.net/releases.html?variant=openjdk13&jvmVariant=hotspot) installed.
You can use any version of Java, providing it's version 11 or newer. Ideally, you'll want to install the `JRE`, as it's significantly lighter than the `JDK`.
The `JRE` is used for running applications, whereas the `JDK` is for building them.

Once you've installed Java, head over to the [releases](/../../releases) tab and download the newest release.
This ensures you'll have all the latest features, bug fixes and optimisations. From there, you'll want to grab the `JukeBot.jar` asset.

Now with that out of the way, go to the [Discord Developer portal](https://discord.com/developers/applications) and create a new application.
You can name it whatever you like, and give it any icon of your choosing. Once that's done, click on the `Bot` tab, and click `Reset Token` to generate a token for it.
Make a note of your token, and more importantly, **do not share it with anyone.**
A token allows you to authenticate the bot with Discord, but malicious actors could hijack it to cause mayhem, and could land you in trouble.

Next, you'll need to create a `config.properties` file in the same directory as your downloaded `JukeBot.jar`.
Your config will need to look something like this:
```properties
token=<YOUR DISCORD APPLICATION TOKEN>
prefix=!
```

This is the bare minimum you need for the bot to work. You can find more configurable fields in the [example config](config.properties.example).
You can change prefix to whatever you'd like, this is simply the string you'll need to prefix your messages with to use the bot's commands, for example `!play` or `$play`.
You can put pretty much anything there, as long as it's not empty. It should go without saying, make sure to replace the `<YOUR DISCORD APPLICATION TOKEN>` placeholder with
the token you copied from the Discord Developer Portal. This includes replacing the `<>`.

Once you've done all that, save your new `config.properties` file. Open a new terminal window/command prompt/whatever else you use, make sure you've `cd`'d into the directory
containing your `JukeBot.jar` and `config.properties` files. Now, you can actually run the bot.

Enter the command `java -jar JukeBot.jar` and watch as the bot comes to life! Invite the bot to your server, and then enjoy the fruits of your labour! You can tweak the JVM
with arguments for things like the garbage collector, which can sometimes make the bot perform better under heavy loads, however, unless you're running a public instance,
this is not something you're likely to need to do. Additionally, if you're not accustomed to tweaking the JVM, you can actually make it perform worse, so this is only recommended
for experienced users.

That's all there is to it, however if you find yourself stuck at any of these steps, you can [join the Discord server](https://discord.gg/xvtH2Yn) for help.