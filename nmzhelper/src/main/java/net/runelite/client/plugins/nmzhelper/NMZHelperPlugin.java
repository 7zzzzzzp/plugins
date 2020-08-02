package net.runelite.client.plugins.nmzhelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.util.Text;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.nmzhelper.Tasks.AbsorptionTask;
import net.runelite.client.plugins.nmzhelper.Tasks.AcceptDreamTask;
import net.runelite.client.plugins.nmzhelper.Tasks.ContinueDialogTask;
import net.runelite.client.plugins.nmzhelper.Tasks.DominicDialogue1Task;
import net.runelite.client.plugins.nmzhelper.Tasks.DominicDialogue2Task;
import net.runelite.client.plugins.nmzhelper.Tasks.DominicDreamTask;
import net.runelite.client.plugins.nmzhelper.Tasks.DrinkPotionTask;
import net.runelite.client.plugins.nmzhelper.Tasks.OpenAbsorptionsBarrelTask;
import net.runelite.client.plugins.nmzhelper.Tasks.OpenOverloadsBarrel;
import net.runelite.client.plugins.nmzhelper.Tasks.OverloadTask;
import net.runelite.client.plugins.nmzhelper.Tasks.RockCakeTask;
import net.runelite.client.plugins.nmzhelper.Tasks.WithdrawAbsorptionTask;
import net.runelite.client.plugins.nmzhelper.Tasks.WithdrawOverloadTask;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "NMZ Helper",
	description = "An automation utility for NMZ",
	tags = {"combat", "potion", "overload", "absorption", "nmz", "nightmare", "zone", "helper"},
	enabledByDefault = false,
	type = PluginType.MINIGAME
)
public class NMZHelperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private NMZHelperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NMZHelperOverlay overlay;

	boolean pluginStarted;

	@Provides
	NMZHelperConfig provideConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(NMZHelperConfig.class);
	}

	public String status = "initializing...";
	private TaskSet tasks = new TaskSet();

	public static int rockCakeDelay = 0;

	@Override
	protected void startUp() throws Exception
	{
		pluginStarted = false;
		overlayManager.add(overlay);
		status = "initializing...";
		tasks.clear();
		tasks.addAll(
			new OverloadTask(client, config),
			new AbsorptionTask(client, config),
			new RockCakeTask(client, config),
			new OpenAbsorptionsBarrelTask(client, config),
			new OpenOverloadsBarrel(client, config),
			new WithdrawAbsorptionTask(client, config),
			new WithdrawOverloadTask(client, config),
			new DominicDreamTask(client, config),
			new DominicDialogue1Task(client, config),
			new DominicDialogue2Task(client, config),
			new ContinueDialogTask(client, config),
			new DrinkPotionTask(client, config),
			new AcceptDreamTask(client, config));
	}

	@Override
	protected void shutDown() throws Exception
	{
		pluginStarted = false;
		overlayManager.remove(overlay);
		tasks.clear();
	}

	@Subscribe
	public void onConfigButtonClicked(ConfigButtonClicked event)
	{
		if (!event.getGroup().equals("nmzhelper"))
		{
			return;
		}

		if (event.getKey().equals("startButton"))
		{
			pluginStarted = true;
		}
		else if (event.getKey().equals("stopButton"))
		{
			pluginStarted = false;
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!pluginStarted)
		{
			return;
		}

		String msg = Text.removeTags(event.getMessage()); //remove color

		switch (event.getType())
		{
			case SPAM:
				if (msg.contains("You drink some of your overload potion."))
				{
					rockCakeDelay = 12;
				}
				break;
			case GAMEMESSAGE:
				if (msg.contains("This barrel is empty."))
				{
					pluginStarted = false;
				}
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!pluginStarted)
		{
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (client.getVarbitValue(3948) < 26)
		{
			pluginStarted = false;
			return;
		}

		Task task = tasks.getValidTask();
		if (task != null)
		{
			status = task.getTaskDescription();
			task.onGameTick(event);
		}
		else
		{
			System.out.println("ERROR: (NMZHelper) Proper task not found...");
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!pluginStarted)
		{
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Task task = tasks.getValidTask();
		if (task != null)
		{
			status = task.getTaskDescription();
			task.onMenuOptionClicked(event);
		}
	}
}
