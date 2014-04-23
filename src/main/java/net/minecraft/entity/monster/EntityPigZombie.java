package net.minecraft.entity.monster;

import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

public class EntityPigZombie extends EntityZombie
{
	private static final UUID field_110189_bq = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
	private static final AttributeModifier field_110190_br = (new AttributeModifier(field_110189_bq, "Attacking speed boost", 0.45D, 0)).setSaved(false);
	private int angerLevel;
	private int randomSoundDelay;
	private Entity field_110191_bu;
	private static final String __OBFID = "CL_00001693";

	public EntityPigZombie(World par1World)
	{
		super(par1World);
		this.isImmuneToFire = true;
	}

	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(field_110186_bp).setBaseValue(0.0D);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(5.0D);
	}

	protected boolean isAIEnabled()
	{
		return false;
	}

	public void onUpdate()
	{
		if (this.field_110191_bu != this.entityToAttack && !this.worldObj.isRemote)
		{
			IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
			iattributeinstance.removeModifier(field_110190_br);

			if (this.entityToAttack != null)
			{
				iattributeinstance.applyModifier(field_110190_br);
			}
		}

		this.field_110191_bu = this.entityToAttack;

		if (this.randomSoundDelay > 0 && --this.randomSoundDelay == 0)
		{
			this.playSound("mob.zombiepig.zpigangry", this.getSoundVolume() * 2.0F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) * 1.8F);
		}

		super.onUpdate();
	}

	public boolean getCanSpawnHere()
	{
		return this.worldObj.difficultySetting != EnumDifficulty.PEACEFUL && this.worldObj.checkNoEntityCollision(this.boundingBox) && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty() && !this.worldObj.isAnyLiquid(this.boundingBox);
	}

	public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeEntityToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setShort("Anger", (short)this.angerLevel);
	}

	public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readEntityFromNBT(par1NBTTagCompound);
		this.angerLevel = par1NBTTagCompound.getShort("Anger");
	}

	protected Entity findPlayerToAttack()
	{
		return this.angerLevel == 0 ? null : super.findPlayerToAttack();
	}

	public boolean attackEntityFrom(DamageSource par1DamageSource, float par2)
	{
		if (this.isEntityInvulnerable())
		{
			return false;
		}
		else
		{
			Entity entity = par1DamageSource.getEntity();

			if (entity instanceof EntityPlayer)
			{
				List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.expand(32.0D, 32.0D, 32.0D));

				for (int i = 0; i < list.size(); ++i)
				{
					Entity entity1 = (Entity)list.get(i);

					if (entity1 instanceof EntityPigZombie)
					{
						EntityPigZombie entitypigzombie = (EntityPigZombie)entity1;
						entitypigzombie.becomeAngryAt(entity);
					}
				}

				this.becomeAngryAt(entity);
			}

			return super.attackEntityFrom(par1DamageSource, par2);
		}
	}

	private void becomeAngryAt(Entity par1Entity)
	{
		this.entityToAttack = par1Entity;
		this.angerLevel = 400 + this.rand.nextInt(400);
		this.randomSoundDelay = this.rand.nextInt(40);
	}

	protected String getLivingSound()
	{
		return "mob.zombiepig.zpig";
	}

	protected String getHurtSound()
	{
		return "mob.zombiepig.zpighurt";
	}

	protected String getDeathSound()
	{
		return "mob.zombiepig.zpigdeath";
	}

	protected void dropFewItems(boolean par1, int par2)
	{
		int j = this.rand.nextInt(2 + par2);
		int k;

		for (k = 0; k < j; ++k)
		{
			this.dropItem(Items.rotten_flesh, 1);
		}

		j = this.rand.nextInt(2 + par2);

		for (k = 0; k < j; ++k)
		{
			this.dropItem(Items.gold_nugget, 1);
		}
	}

	public boolean interact(EntityPlayer par1EntityPlayer)
	{
		return false;
	}

	protected void dropRareDrop(int par1)
	{
		this.dropItem(Items.gold_ingot, 1);
	}

	protected void addRandomArmor()
	{
		this.setCurrentItemOrArmor(0, new ItemStack(Items.golden_sword));
	}

	public IEntityLivingData onSpawnWithEgg(IEntityLivingData par1EntityLivingData)
	{
		super.onSpawnWithEgg(par1EntityLivingData);
		this.setVillager(false);
		return par1EntityLivingData;
	}
}