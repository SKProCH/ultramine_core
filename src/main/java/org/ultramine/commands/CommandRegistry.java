package org.ultramine.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import org.ultramine.commands.syntax.ArgumentsPatternParser;
import org.ultramine.server.util.TranslitTable;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CommandRegistry
{
	private final Map<String, IExtendedCommand> commandMap;
	private final SortedSet<IExtendedCommand> registeredCommands;
	private final ArgumentsPatternParser argumentsPatternParser;

	public CommandRegistry()
	{
		commandMap = new HashMap<String, IExtendedCommand>();
		registeredCommands = new TreeSet<IExtendedCommand>();
		argumentsPatternParser = new ArgumentsPatternParser();
	}

	public IExtendedCommand registerCommand(IExtendedCommand command)
	{
		@SuppressWarnings("unchecked")
		List<String> aliases = command.getCommandAliases();
		commandMap.put(command.getCommandName(), command);
		commandMap.put(TranslitTable.translitENRU(command.getCommandName()), command);
		registeredCommands.add(command);

		if (aliases != null)
		{
			for (String alias : aliases)
			{
				commandMap.put(alias, command);
				commandMap.put(TranslitTable.translitENRU(alias), command);
			}
		}

		return command;
	}

	public IExtendedCommand registerVanillaCommand(ICommand command)
	{
		ModContainer active = Loader.instance().activeModContainer();
		return registerCommand(new VanillaCommandWrapper(command, active != null ? active.getModId().toLowerCase() : "vanilla"));
	}

	public void registerCommands(Class<?> cls)
	{
		List<HandlerBasedCommand.Builder> builders = new ArrayList<HandlerBasedCommand.Builder>();
		Map<String, Map<String, ICommandHandler>> actions = new HashMap<String, Map<String, ICommandHandler>>();

		for (Method method : cls.getMethods())
		{
			if (method.isAnnotationPresent(Command.class) && Modifier.isStatic(method.getModifiers()))
			{
				Command data = method.getAnnotation(Command.class);
				HandlerBasedCommand.Builder builder = new HandlerBasedCommand.Builder(data.name(), data.group(), new MethodBasedCommandHandler(method))
						.setAliases(data.aliases())
						.setPermissions(data.permissions())
						.setUsableFromServer(data.isUsableFromServer());

				for (String completion : data.syntax())
					builder.addArgumentsPattern(argumentsPatternParser.parse(completion));

				builders.add(builder);
			}

			if (method.isAnnotationPresent(Action.class) && Modifier.isStatic(method.getModifiers()))
			{
				Action data = method.getAnnotation(Action.class);

				if (!actions.containsKey(data.command()))
					actions.put(data.command(), new HashMap<String, ICommandHandler>());

				actions.get(data.command()).put(data.name(), new MethodBasedCommandHandler(method));
			}
		}

		for (HandlerBasedCommand.Builder builder : builders)
		{
			if (actions.containsKey(builder.getName()))
			{
				for (Map.Entry<String, ICommandHandler> action : actions.get(builder.getName()).entrySet())
					builder.addAction(action.getKey(), action.getValue());
			}

			registerCommand(builder.build());
		}
	}

	public IExtendedCommand get(String name)
	{
		return commandMap.get(name);
	}
	public String[] getPermissions(String name)
	{
		return commandMap.get(name).getPermissions();
	}

	public Map<String, IExtendedCommand> getCommandMap()
	{
		return commandMap;
	}

	public List<String> filterPossibleCommandsNames(ICommandSender sender, String filter)
	{
		List<String> result = new ArrayList<String>();

		for (Map.Entry<String, IExtendedCommand> entry : commandMap.entrySet())
		{
			if (CommandBase.doesStringStartWith(filter, entry.getKey()) && entry.getValue().canCommandSenderUseCommand(sender))
				result.add(entry.getKey());
		}

		return result;
	}

	public List<IExtendedCommand> getPossibleCommands(ICommandSender sender)
	{
		List<IExtendedCommand> result = new ArrayList<IExtendedCommand>();

		for (IExtendedCommand command : registeredCommands)
		{
			if (command.canCommandSenderUseCommand(sender))
				result.add(command);
		}

		return result;
	}

	public ArgumentsPatternParser getArgumentsParser()
	{
		return argumentsPatternParser;
	}
}
