WoWChat -- README
=================

WoWChat is a Discord integration chat bot for old versions of World of Warcraft.

**It does NOT support WoW Classic or Retail servers.**

New Requirement: Due to Discord changes, you must check PRESENCE INTENT, SERVER MEMBERS INTENT, and MESSAGE CONTENT INTENT under Privileged Gateway Intents in Discord developer portal.

Currently supported versions are:
  * Vanilla
  * The Burning Crusade
  * Wrath of the Lich King
  * Cataclysm (4.3.4 build 15595)
  * Mists of Pandaria (5.4.8 build 18414)

Some servers have their own modifications which deviate from the original client implementation. For those, consider these dedicated forks which add the custom features of those servers:
  * Ascension: https://github.com/Szyler/AscensionChat
  * Turtle WoW: https://github.com/Zebouski/WoWChat-Turtle

Features:
* Clientless (Does not need the WoW Client to be open to run)
* Seamless Chat integration of channels between WoW and Discord
  * Guild chat, Officer chat, Local chat, Emotes, Custom Channels.
  * In-game links (items, spells, ...) are displayed as links to classicdb or twinstar, depending on the expansion
  * Configurable message format
* Smart Tagging
  * Tag players on Discord from WoW using @and_part_or_all_of_their_name.
  * You can also tag @here and @everyone and "@Even Roles With Spaces" (include quotes around them).
* Custom commands
  * Check who is online in your guild
  * Query other players in the world
* Runs as a Java program, and therefore works on Windows, Mac, and Linux.

## How it works
The bot uses Discord's API to login to your Discord server. It then uses supplied information
to login as a WoW character onto your chosen server. Once it logs in to WoW as a character,
and sees the configured channels. It will relay messages in Discord chat and WoW chat respectively.

##### DO NOT, under any circumstances, use this bot on an account with existing characters!
Even though this bot does not do anything malicious, some servers may not like a bot connecting, and GMs may ban the account!

##### Watching GD's WotLK chat:
![gd-echoes](https://raw.githubusercontent.com/fjaros/wowchat/master/images/example1.png)

##### Talking in Guild Chat:
![guild-chat-construct](https://raw.githubusercontent.com/fjaros/wowchat/master/images/example2.png)

## Setup & Prerequisites

1. First you will want to create a Discord Bot on your discord account:
   * Go to https://discordapp.com/developers/applications/
   * Sign into your Discord account if necessary and click "Create an application"
   * Change the application name to something meaningful like "WoW Chat"
   * On the left click the Bot tab
   * Add a Bot
   * Uncheck Public Bot option
   * **Check PRESENCE INTENT and SERVER MEMBERS INTENT under "Privileged Gateway Intents"** This is important! Without it, your bot will not work!
   * Under token click Copy. This is the value WoW Chat will use to login to Discord.
2. Configure WoW Chat by opening `wowchat.conf` in a text editor.
   * You can also create your own file, using the supplied `wowchat.conf` as a template.
   * In section **discord**:
     * **token**: Paste the above copied Bot token, or set the DISCORD_TOKEN environment variable.
     * **enable_dot_commands**: If set to 1, it will not format outgoing messages starting with ".", enabling you to send things like ".s in" to the server directly. If set to 0, it will format these messages like regular messages.
     * **dot_commands_whitelist**: If empty, it will allow or disallow dot commands based on **enable_dot_commands** setting. If any command is listed here, the bot will ONLY allow those specific comamnds to be sent in game.
     * **enable_commands_channels**: A list of channels for which to allow commands. If not specified or empty, the bot will allow commands from all channels.
   * In section **wow**:
     * **platform**: Leave as **Mac** unless your target server has Warden (anticheat) disabled AND it is blocking/has disabled Mac logins. In this case put **Windows**.
     * **locale**: Optionally specify a locale if you want to join locale-specific global channels. **enUS** is the default locale.
     * **enable_server_motd**: **0** to ignore sending server's MotD. **1** to send server's MotD as a SYSTEM message.
     * **version**: put either 1.12.1, 2.4.3, 3.3.5, 4.3.4, or 5.4.8 based on the server's expansion.
     * **build**: you can include a build=<build number> setting in the config, if you are using a custom build version on your server. Optionally you can also use **realm_build** and **game_build** options if the number used is different for each server. See https://github.com/fjaros/wowchat/issues/90
     * **realmlist**: this is server's realmlist, same as in your realmlist.wtf file.
     Example values are logon.lightshope.org or wow.gamer-district.org
     * **realm**: This is the realm name the Bot will connect to.
     It is the Text shown on top of character list window. Put ONLY the name, do NOT put the realm type like PVP or PVE.
     In the following example, the **realm** value is The Construct
     * ![realm-construct](https://raw.githubusercontent.com/fjaros/wowchat/master/images/example3.png)
     * **account**: The bot's WoW game account, or set the WOW_ACCOUNT environment variable.
     * **password**: The bot's WoW game account password, or set the WOW_PASSWORD environment variable.
     * **character**: Your character's name as would be shown in the character list, or set the WOW_CHARACTER environment variable.
   * In section **guild**:
     * This section sets up guild notifications on Discord.
     * For each notification, **online**, **offline**, **joined**, **left**, **motd**, **achievement** specify:
       * **enabled**: **0** to not display in Discord, **1** to display in Discord
       * **format**: How to display the message.
       * **channel**: Optional channel **name** OR **ID** where to display message instead of the default guild chat channel.
   * In section **chat**:
     * This section sets up the channel relays between Discord and WoW. You can have an unlimited number of channel relays.
     * **direction**: How do you want to relay each channel, put either
     **wow_to_discord**, **discord_to_wow**, or **both**.
     * **wow** section:
       * In type put one of, **Say**, **Guild**, **Officer**, **Emote**, **Yell**, **System**, **Whisper**, **Channel**. This is the type of chat the Bot will read for this section.
         * If you put **type=Channel**, you also must provide a **channel=name of channel** value.
       * In format put how you want to display the message, supported replacable values are **%time**, **%user**, **%message**, and **%channel** if above type is **Channel**.
       * **filters**: See filters section. If a channel configuration has this section, it will override the global filters and use these instead for this channel.
         * If this is in the **wow** section, it will filter Discord->WoW messages.
       * Optionally in **id**, specify the channel ID if your server has a non-standard global channel.
     * **discord** section:
       * **channel**: The discord channel **name** OR **ID** where to display the message. **It is advised to use channel ID here instead of name, so the bot does not stop working when the channel name is changed.**
         * To see channels' IDs, you must enable Developer mode in Discord under User Settings -> Appearance -> Advanced.
       * **format**: Same options as in **wow** section above.
       * **filters**: See filters section. If a channel configuration has this section, it will override the global filters and use these instead for this channel.
         * If this is in the **discord** section, it will filter WoW->Discord messages.
   * In section **filters**:
     * This section specifies filters for chat messages to be ignored by the bot. It works for both directions, Discord to WoW and WoW to Discord. It can be overriden in each specific channel configuration as stated above.
     * **enabled**: **0** to globally disable all filters, **1** to enable them.
     * **patterns**: List of Java Regex match patterns. If the incoming messages matches any one of the patterns and filters are enabled, it will be ignored.
       * When ignored, the message will not be relayed; however it will be logged into the bot's command line output prepended with the word FILTERED.

3. Invite your bot to Discord
   * Go back to https://discordapp.com/developers/applications/ and click your new Bot application.
   * In browser enter: https://discordapp.com/oauth2/authorize?client_id=CLIENT_ID&scope=bot
     * Replace **CLIENT_ID** with the value from Discord applications page.
   * Setup the bot with the correct Discord roles/permissions to enter your desired channels.

## Run
1. Download the latest ready-made binary from github releases: https://github.com/fjaros/wowchat/releases
   * **Make sure you have a Java Runtime Environment (JRE) 1.8 or higher installed on your system!**
   * **On Windows**: Edit wowchat.conf as above and run `run.bat`
   * **On Mac/Linux**: Edit wowchat.conf as above and run `run.sh`

OR to compile yourself:
1. WoW Chat is written in Scala and compiles to a Java executable using [maven](https://maven.apache.org).
2. It uses Java JDK 1.8 and Scala 2.12.12.
3. Run `mvn clean package` which will produce a file in the target folder called `wowchat-1.3.8.zip`
4. unzip `wowchat-1.3.8.zip`, edit the configuration file and run `java -jar wowchat.jar <config file>`
   * If no config file is supplied, the bot will try to use `wowchat.conf`
