package org.ultramine.server.internal;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import com.koloboke.collect.map.ShortObjMap;
import com.koloboke.collect.map.hash.HashShortObjMaps;
import org.ultramine.server.util.CachedEntry;

import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Java 8 features like lambdas must not be used in net.minecraft classes, so all lambdas used there, located here*/
public class LambdaHolder
{
	public static final Predicate<?> ENTITY_REMOVAL_PREDICATE = o -> ((Entity)o).removeThisTick;
	public static final Predicate<?> TILE_ENTITY_REMOVAL_PREDICATE = o -> ((TileEntity)o).removeThisTick;

	public static <T> Supplier<TreeSet<T>> newTreeSet()
	{
		return TreeSet::new;
	}

	public static <T> Supplier<ShortObjMap<T>> newShortObjMap()
	{
		return HashShortObjMaps::newMutableMap;
	}

	@SuppressWarnings({"Guava"})
	public static <T> com.google.common.base.Function<CachedEntry<T>, T> cachedEntryGetValueGuavaFunc()
	{
		return CachedEntry::getValueAndUpdateTime;
	}
}
