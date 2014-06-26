package net.minecraft.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public enum EnumChatFormatting
{
	BLACK('0'),
	DARK_BLUE('1'),
	DARK_GREEN('2'),
	DARK_AQUA('3'),
	DARK_RED('4'),
	DARK_PURPLE('5'),
	GOLD('6'),
	GRAY('7'),
	DARK_GRAY('8'),
	BLUE('9'),
	GREEN('a'),
	AQUA('b'),
	RED('c'),
	LIGHT_PURPLE('d'),
	YELLOW('e'),
	WHITE('f'),
	OBFUSCATED('k', true),
	BOLD('l', true),
	STRIKETHROUGH('m', true),
	UNDERLINE('n', true),
	ITALIC('o', true),
	RESET('r');
	private static final Map formattingCodeMapping = new HashMap();
	private static final Map nameMapping = new HashMap();
	private static final Pattern formattingCodePattern = Pattern.compile("(?i)" + String.valueOf('\u00a7') + "[0-9A-FK-OR]");
	private final char formattingCode;
	private final boolean fancyStyling;
	private final String controlString;

	private static final String __OBFID = "CL_00000342";

	private EnumChatFormatting(char par3)
	{
		this(par3, false);
	}

	private EnumChatFormatting(char par3, boolean par4)
	{
		this.formattingCode = par3;
		this.fancyStyling = par4;
		this.controlString = "\u00a7" + par3;
	}

	public char getFormattingCode()
	{
		return this.formattingCode;
	}

	public boolean isFancyStyling()
	{
		return this.fancyStyling;
	}

	public boolean isColor()
	{
		return !this.fancyStyling && this != RESET;
	}

	public String getFriendlyName()
	{
		return this.name().toLowerCase();
	}

	public String toString()
	{
		return this.controlString;
	}

	@SideOnly(Side.CLIENT)
	public static String getTextWithoutFormattingCodes(String par0Str)
	{
		return par0Str == null ? null : formattingCodePattern.matcher(par0Str).replaceAll("");
	}

	public static EnumChatFormatting getValueByName(String par0Str)
	{
		return par0Str == null ? null : (EnumChatFormatting)nameMapping.get(par0Str.toLowerCase());
	}

	public static Collection getValidValues(boolean par0, boolean par1)
	{
		ArrayList arraylist = new ArrayList();
		EnumChatFormatting[] aenumchatformatting = values();
		int i = aenumchatformatting.length;

		for (int j = 0; j < i; ++j)
		{
			EnumChatFormatting enumchatformatting = aenumchatformatting[j];

			if ((!enumchatformatting.isColor() || par0) && (!enumchatformatting.isFancyStyling() || par1))
			{
				arraylist.add(enumchatformatting.getFriendlyName());
			}
		}

		return arraylist;
	}

	static
	{
		EnumChatFormatting[] var0 = values();
		int var1 = var0.length;

		for (int var2 = 0; var2 < var1; ++var2)
		{
			EnumChatFormatting var3 = var0[var2];
			formattingCodeMapping.put(Character.valueOf(var3.getFormattingCode()), var3);
			nameMapping.put(var3.getFriendlyName(), var3);
		}
	}
	
	public static EnumChatFormatting getByColorCode(char code)
	{
		return (EnumChatFormatting)formattingCodeMapping.get(code);
	}
}