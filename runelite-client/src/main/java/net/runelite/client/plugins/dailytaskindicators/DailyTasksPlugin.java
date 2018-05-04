/*
 * Copyright (c) 2018, Infinitay <https://github.com/Infinitay>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.runelite.client.plugins.dailytaskindicators;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColor;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Daily Task Indicator",
	enabledByDefault = false
)
@Slf4j
public class DailyTasksPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DailyTasksConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private boolean hasSentHerbMsg, hasSentStavesMsg, hasSentEssenceMsg;

	@Provides
	DailyTasksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DailyTasksConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hasSentHerbMsg = hasSentStavesMsg = hasSentEssenceMsg = false;
		cacheColors();
	}

	@Override
	protected void shutDown() throws Exception
	{
		hasSentHerbMsg = hasSentStavesMsg = hasSentEssenceMsg = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("dailytaskindicators"))
		{
			switch (event.getKey())
			{
				case "showHerbBoxes":
					hasSentHerbMsg = false;
					break;
				case "showStaves":
					hasSentStavesMsg = false;
					break;
				case "showEssence":
					hasSentEssenceMsg = false;
					break;
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == WidgetInfo.CHATBOX.getGroupId())
		{
			if (config.showHerbBoxes() && !hasSentHerbMsg && numberOfHerbBoxesReady() > 0)
			{
				sendChatMessage("You have " + numberOfHerbBoxesReady() + " herb boxes waiting to be collected at Nightmare Zone.");
				hasSentHerbMsg = true;
			}
			if (config.showStaves() && !hasSentStavesMsg && numberOfStavesReady() > 0)
			{
				sendChatMessage("You have " + numberOfStavesReady() + " staves waiting to be collected from Zaff.");
				hasSentStavesMsg = true;
			}
			if (config.showEssence() && !hasSentEssenceMsg && numberOfEssenceReady() > 0)
			{
				sendChatMessage("You have " + numberOfEssenceReady() + " pure essence waiting to be collected from Wizard Cromperty.");
				hasSentEssenceMsg = true;
			}
		}
	}

	private int numberOfHerbBoxesReady()
	{
		int numberClaimed = client.getVar(Varbits.DAILY_HERB_BOX);
		return 15 - numberClaimed;
	}

	private int numberOfStavesReady()
	{
		if (client.getVar(Varbits.DAILY_STAVES) == 1)
		{
			return 0;
		}
		if (client.getVar(Varbits.DIARY_VARROCK_ELITE) == 1)
		{
			return 120;
		}
		else if (client.getVar(Varbits.DIARY_VARROCK_HARD) == 1)
		{
			return 60;
		}
		else if (client.getVar(Varbits.DIARY_VARROCK_MEDIUM) == 1)
		{
			return 30;
		}
		else if (client.getVar(Varbits.DIARY_VARROCK_EASY) == 1)
		{
			return 15;
		}
		else
		{
			return 0;
		}
	}

	private int numberOfEssenceReady()
	{
		if (client.getVar(Varbits.DAILY_ESSENCE) == 1)
		{
			return 0;
		}
		if (client.getVar(Varbits.DIARY_ARDOUGNE_ELITE) == 1)
		{
			return 250;
		}
		else if (client.getVar(Varbits.DIARY_ARDOUGNE_HARD) == 1)
		{
			return 150;
		}
		else if (client.getVar(Varbits.DIARY_ARDOUGNE_MEDIUM) == 1)
		{
			return 100;
		}
		else
		{
			return 0;
		}
	}

	private void cacheColors()
	{
		chatMessageManager.cacheColor(new ChatColor(ChatColorType.HIGHLIGHT, Color.RED, false), ChatMessageType.GAME).refreshAll();
	}

	private void sendChatMessage(String chatMessage)
	{
		final String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(chatMessage)
			.build();

		chatMessageManager.queue(
			QueuedMessage.builder()
				.type(ChatMessageType.GAME)
				.runeLiteFormattedMessage(message)
				.build());
	}
}
