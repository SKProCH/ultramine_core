package net.minecraft.entity.passive;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIControlledByPlayer;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.AchievementList;
import net.minecraft.world.World;

public class EntityPig extends EntityAnimal
{
	private final EntityAIControlledByPlayer aiControlledByPlayer;
	private static final String __OBFID = "CL_00001647";

	public EntityPig(World par1World)
	{
		super(par1World);
		this.setSize(0.9F, 0.9F);
		this.getNavigator().setAvoidsWater(true);
		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIPanic(this, 1.25D));
		this.tasks.addTask(2, this.aiControlledByPlayer = new EntityAIControlledByPlayer(this, 0.3F));
		this.tasks.addTask(3, new EntityAIMate(this, 1.0D));
		this.tasks.addTask(4, new EntityAITempt(this, 1.2D, Items.carrot_on_a_stick, false));
		this.tasks.addTask(4, new EntityAITempt(this, 1.2D, Items.carrot, false));
		this.tasks.addTask(5, new EntityAIFollowParent(this, 1.1D));
		this.tasks.addTask(6, new EntityAIWander(this, 1.0D));
		this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(8, new EntityAILookIdle(this));
	}

	public boolean isAIEnabled()
	{
		return true;
	}

	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(10.0D);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
	}

	protected void updateAITasks()
	{
		super.updateAITasks();
	}

	public boolean canBeSteered()
	{
		ItemStack itemstack = ((EntityPlayer)this.riddenByEntity).getHeldItem();
		return itemstack != null && itemstack.getItem() == Items.carrot_on_a_stick;
	}

	protected void entityInit()
	{
		super.entityInit();
		this.dataWatcher.addObject(16, Byte.valueOf((byte)0));
	}

	public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeEntityToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setBoolean("Saddle", this.getSaddled());
	}

	public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readEntityFromNBT(par1NBTTagCompound);
		this.setSaddled(par1NBTTagCompound.getBoolean("Saddle"));
	}

	protected String getLivingSound()
	{
		return "mob.pig.say";
	}

	protected String getHurtSound()
	{
		return "mob.pig.say";
	}

	protected String getDeathSound()
	{
		return "mob.pig.death";
	}

	protected void func_145780_a(int p_145780_1_, int p_145780_2_, int p_145780_3_, Block p_145780_4_)
	{
		this.playSound("mob.pig.step", 0.15F, 1.0F);
	}

	public boolean interact(EntityPlayer par1EntityPlayer)
	{
		if (super.interact(par1EntityPlayer))
		{
			return true;
		}
		else if (this.getSaddled() && !this.worldObj.isRemote && (this.riddenByEntity == null || this.riddenByEntity == par1EntityPlayer))
		{
			par1EntityPlayer.mountEntity(this);
			return true;
		}
		else
		{
			return false;
		}
	}

	protected Item getDropItem()
	{
		return this.isBurning() ? Items.cooked_porkchop : Items.porkchop;
	}

	protected void dropFewItems(boolean par1, int par2)
	{
		int j = this.rand.nextInt(3) + 1 + this.rand.nextInt(1 + par2);

		for (int k = 0; k < j; ++k)
		{
			if (this.isBurning())
			{
				this.dropItem(Items.cooked_porkchop, 1);
			}
			else
			{
				this.dropItem(Items.porkchop, 1);
			}
		}

		if (this.getSaddled())
		{
			this.dropItem(Items.saddle, 1);
		}
	}

	public boolean getSaddled()
	{
		return (this.dataWatcher.getWatchableObjectByte(16) & 1) != 0;
	}

	public void setSaddled(boolean par1)
	{
		if (par1)
		{
			this.dataWatcher.updateObject(16, Byte.valueOf((byte)1));
		}
		else
		{
			this.dataWatcher.updateObject(16, Byte.valueOf((byte)0));
		}
	}

	public void onStruckByLightning(EntityLightningBolt par1EntityLightningBolt)
	{
		if (!this.worldObj.isRemote)
		{
			EntityPigZombie entitypigzombie = new EntityPigZombie(this.worldObj);
			entitypigzombie.setCurrentItemOrArmor(0, new ItemStack(Items.golden_sword));
			entitypigzombie.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			this.worldObj.spawnEntityInWorld(entitypigzombie);
			this.setDead();
		}
	}

	protected void fall(float par1)
	{
		super.fall(par1);

		if (par1 > 5.0F && this.riddenByEntity instanceof EntityPlayer)
		{
			((EntityPlayer)this.riddenByEntity).triggerAchievement(AchievementList.flyPig);
		}
	}

	public EntityPig createChild(EntityAgeable par1EntityAgeable)
	{
		return new EntityPig(this.worldObj);
	}

	public boolean isBreedingItem(ItemStack par1ItemStack)
	{
		return par1ItemStack != null && par1ItemStack.getItem() == Items.carrot;
	}

	public EntityAIControlledByPlayer getAIControlledByPlayer()
	{
		return this.aiControlledByPlayer;
	}
}