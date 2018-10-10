WoWChat -- README
=================

WoWChat is a Discord integration chat bot for old versions of World of Warcraft. Features:
* Clientless (Does not need the WoW Client to be open to run)
* Seamless Chat integration of channels between WoW and Discord
  * Guild chat, Officer chat, Local chat, Emotes, Custom Channels.
  * In-game links (items, spells, ...) are displayed as links to classicdb or twinstar, depending on the expansion
  * Configurable message format
* Custom commands
  * Check who is online in your guild
  * Query other players in the world
* Runs as a Java program, and therefore works on Windows, Mac, and Linux.

Currently supported expansions are Vanilla, The Burning Crusade, and Wrath of the Lich King.

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
   * Under token click Copy. This is the value WoW Chat will use to login to Discord.
2. Configure WoW Chat by opening `wowchat.conf` in a text editor.
   * You can also create your own file, using the supplied `wowchat.conf` as a template.
   * In section **discord**:
     * Paste the above copied Bot token after "token="
   * In section **wow**:
     * **version**: put either 1.12.1, 2.4.3, or 3.3.5 based on the server's expansion.
     * **realmlist**: this is server's realmlist, same as in your realmlist.wtf file.
     Example values are logon.lightshope.org or wow.gamer-district.org
     * **realm**: This is the realm name the Bot will connect to.
     It is the Text shown on top of character list window. Put ONLY the name, do NOT put the realm type like PVP or PVE.
     In the following example, the **realm** value is The Construct
     * ![realm-construct](https://raw.githubusercontent.com/fjaros/wowchat/master/images/example3.png)
     * **account**: The bot's WoW game account.
     * **password**: The bot's WoW game account password.
     * **character**: Your character's name as would be shown in the character list.
   * In section **guild**:
     * This section sets up guild notifications on Discord.
     * For each notification, **online**, **offline**, **joined**, **left**, specify:
       * **enabled**: **0** to not display in Discord, **1** to display in Discord
       * **format**: How to display the message.
   * In section **chat**:
     * This section sets up the channel relays between Discord and WoW. You can have an unlimited number of channel relays.
     * **direction**: How do you want to relay each channel, put either
     **wow_to_discord**, **discord_to_wow**, or **both**.
     * **wow** section:
       * In type put one of, **Say**, **Guild**, **Officer**, **Emote**, **Yell**, **System**, **Channel**. This is the type of chat the Bot will read for this section.
         * If you put **type=Channel**, you also must provide a **channel=name of channel** value.
       * In format put how you want to display the message, supported replacable values are **%user**, **%message**, and **%channel** if above type is **Channel**.
     * **discord** section:
       * **channel**: The discord channel where to display the message.
       * **format**: Same options as in **wow** section above.
3. Invite your bot to Discord
   * Go back to https://discordapp.com/developers/applications/ and click your new Bot application.
   * In browser enter: https://discordapp.com/oauth2/authorize?client_id=**CLIENT_ID**&scope=bot
     * Replace **CLIENT_ID** with the value from Discord applications page.
   * Setup the bot with the correct Discord roles/permissions to enter your desired channels.

## Run
1. Download the latest ready-made binary from github releases: https://github.com/fjaros/wowchat/releases
   * **Make sure you have a Java Runtime Environment (JRE) 1.8 or higher installed on your system!**
   * **On Windows**: Edit wowchat.conf as above and run `run.bat`
   * **On Mac/Linux**: Edit wowchat.conf as above and run `run.sh`

OR to compile yourself:
1. WoW Chat is written in Scala and compiles to a Java executable using [maven](https://maven.apache.org).
2. It uses Java JDK 1.8 and Scala 2.12.6.
3. Run `mvn clean package` which will produce a file in the target folder called `wowchat.zip`
4. unzip `wowchat-1.1.0.zip`, edit the configuration file and run `java -jar wowchat.jar <config file>`
   * If no config file is supplied, the bot will try to use `wowchat.conf`