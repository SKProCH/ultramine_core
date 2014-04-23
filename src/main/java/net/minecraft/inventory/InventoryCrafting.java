package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class InventoryCrafting implements IInventory
{
	private ItemStack[] stackList;
	private int inventoryWidth;
	private Container eventHandler;
	private static final String __OBFID = "CL_00001743";

	public InventoryCrafting(Container par1Container, int par2, int par3)
	{
		int k = par2 * par3;
		this.stackList = new ItemStack[k];
		this.eventHandler = par1Container;
		this.inventoryWidth = par2;
	}

	public int getSizeInventory()
	{
		return this.stackList.length;
	}

	public ItemStack getStackInSlot(int par1)
	{
		return par1 >= this.getSizeInventory() ? null : this.stackList[par1];
	}

	public ItemStack getStackInRowAndColumn(int par1, int par2)
	{
		if (par1 >= 0 && par1 < this.inventoryWidth)
		{
			int k = par1 + par2 * this.inventoryWidth;
			return this.getStackInSlot(k);
		}
		else
		{
			return null;
		}
	}

	public String getInventoryName()
	{
		return "container.crafting";
	}

	public boolean hasCustomInventoryName()
	{
		return false;
	}

	public ItemStack getStackInSlotOnClosing(int par1)
	{
		if (this.stackList[par1] != null)
		{
			ItemStack itemstack = this.stackList[par1];
			this.stackList[par1] = null;
			return itemstack;
		}
		else
		{
			return null;
		}
	}

	public ItemStack decrStackSize(int par1, int par2)
	{
		if (this.stackList[par1] != null)
		{
			ItemStack itemstack;

			if (this.stackList[par1].stackSize <= par2)
			{
				itemstack = this.stackList[par1];
				this.stackList[par1] = null;
				this.eventHandler.onCraftMatrixChanged(this);
				return itemstack;
			}
			else
			{
				itemstack = this.stackList[par1].splitStack(par2);

				if (this.stackList[par1].stackSize == 0)
				{
					this.stackList[par1] = null;
				}

				this.eventHandler.onCraftMatrixChanged(this);
				return itemstack;
			}
		}
		else
		{
			return null;
		}
	}

	public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
	{
		this.stackList[par1] = par2ItemStack;
		this.eventHandler.onCraftMatrixChanged(this);
	}

	public int getInventoryStackLimit()
	{
		return 64;
	}

	public void markDirty() {}

	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
	{
		return true;
	}

	public void openInventory() {}

	public void closeInventory() {}

	public boolean isItemValidForSlot(int par1, ItemStack par2ItemStack)
	{
		return true;
	}
}