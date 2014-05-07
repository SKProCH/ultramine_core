package net.minecraft.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class WeightedRandom
{
	private static final String __OBFID = "CL_00001503";

	public static int getTotalWeight(Collection par0Collection)
	{
		int i = 0;
		WeightedRandom.Item item;

		for (Iterator iterator = par0Collection.iterator(); iterator.hasNext(); i += item.itemWeight)
		{
			item = (WeightedRandom.Item)iterator.next();
		}

		return i;
	}

	public static WeightedRandom.Item getRandomItem(Random par0Random, Collection par1Collection, int par2)
	{
		if (par2 <= 0)
		{
			throw new IllegalArgumentException();
		}
		return getItem(par1Collection, par0Random.nextInt(par2));
	}

	//Forge: Added to allow custom random implementations, Modder is responsible for making sure the 
	//'weight' is under the totalWeight of the items.
	public static WeightedRandom.Item getItem(Collection par1Collection, int weight)
	{
		{
			int j = weight;
			Iterator iterator = par1Collection.iterator();
			WeightedRandom.Item item;

			do
			{
				if (!iterator.hasNext())
				{
					return null;
				}

				item = (WeightedRandom.Item)iterator.next();
				j -= item.itemWeight;
			}
			while (j >= 0);

			return item;
		}
	}

	public static WeightedRandom.Item getRandomItem(Random par0Random, Collection par1Collection)
	{
		return getRandomItem(par0Random, par1Collection, getTotalWeight(par1Collection));
	}

	public static int getTotalWeight(WeightedRandom.Item[] par0ArrayOfWeightedRandomItem)
	{
		int i = 0;
		WeightedRandom.Item[] aitem = par0ArrayOfWeightedRandomItem;
		int j = par0ArrayOfWeightedRandomItem.length;

		for (int k = 0; k < j; ++k)
		{
			WeightedRandom.Item item = aitem[k];
			i += item.itemWeight;
		}

		return i;
	}

	public static WeightedRandom.Item getRandomItem(Random par0Random, WeightedRandom.Item[] par1ArrayOfWeightedRandomItem, int par2)
	{
		if (par2 <= 0)
		{
			throw new IllegalArgumentException();
		}
		return getItem(par1ArrayOfWeightedRandomItem, par0Random.nextInt(par2));
	}

	//Forge: Added to allow custom random implementations, Modder is responsible for making sure the 
	//'weight' is under the totalWeight of the items.
	public static WeightedRandom.Item getItem(WeightedRandom.Item[] par1ArrayOfWeightedRandomItem, int weight)
	{
		{
			int j = weight;
			WeightedRandom.Item[] aitem = par1ArrayOfWeightedRandomItem;
			int k = par1ArrayOfWeightedRandomItem.length;

			for (int l = 0; l < k; ++l)
			{
				WeightedRandom.Item item = aitem[l];
				j -= item.itemWeight;

				if (j < 0)
				{
					return item;
				}
			}

			return null;
		}
	}

	public static WeightedRandom.Item getRandomItem(Random par0Random, WeightedRandom.Item[] par1ArrayOfWeightedRandomItem)
	{
		return getRandomItem(par0Random, par1ArrayOfWeightedRandomItem, getTotalWeight(par1ArrayOfWeightedRandomItem));
	}

	public static class Item
		{
			public int itemWeight;
			private static final String __OBFID = "CL_00001504";

			public Item(int par1)
			{
				this.itemWeight = par1;
			}
		}
}