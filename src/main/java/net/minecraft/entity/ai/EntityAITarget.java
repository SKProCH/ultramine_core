package net.minecraft.entity.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.StringUtils;

public abstract class EntityAITarget extends EntityAIBase
{
	protected EntityCreature taskOwner;
	protected boolean shouldCheckSight;
	private boolean nearbyOnly;
	private int targetSearchStatus;
	private int targetSearchDelay;
	private int field_75298_g;
	private static final String __OBFID = "CL_00001626";

	public EntityAITarget(EntityCreature par1EntityCreature, boolean par2)
	{
		this(par1EntityCreature, par2, false);
	}

	public EntityAITarget(EntityCreature par1EntityCreature, boolean par2, boolean par3)
	{
		this.taskOwner = par1EntityCreature;
		this.shouldCheckSight = par2;
		this.nearbyOnly = par3;
	}

	public boolean continueExecuting()
	{
		EntityLivingBase entitylivingbase = this.taskOwner.getAttackTarget();

		if (entitylivingbase == null)
		{
			return false;
		}
		else if (!entitylivingbase.isEntityAlive())
		{
			return false;
		}
		else
		{
			double d0 = this.getTargetDistance();

			if (this.taskOwner.getDistanceSqToEntity(entitylivingbase) > d0 * d0)
			{
				return false;
			}
			else
			{
				if (this.shouldCheckSight)
				{
					if (this.taskOwner.getEntitySenses().canSee(entitylivingbase))
					{
						this.field_75298_g = 0;
					}
					else if (++this.field_75298_g > 60)
					{
						return false;
					}
				}

				return !(entitylivingbase instanceof EntityPlayerMP) || !((EntityPlayerMP)entitylivingbase).theItemInWorldManager.isCreative();
			}
		}
	}

	protected double getTargetDistance()
	{
		IAttributeInstance iattributeinstance = this.taskOwner.getEntityAttribute(SharedMonsterAttributes.followRange);
		return iattributeinstance == null ? 16.0D : iattributeinstance.getAttributeValue();
	}

	public void startExecuting()
	{
		this.targetSearchStatus = 0;
		this.targetSearchDelay = 0;
		this.field_75298_g = 0;
	}

	public void resetTask()
	{
		this.taskOwner.setAttackTarget((EntityLivingBase)null);
	}

	protected boolean isSuitableTarget(EntityLivingBase par1EntityLivingBase, boolean par2)
	{
		if (par1EntityLivingBase == null)
		{
			return false;
		}
		else if (par1EntityLivingBase == this.taskOwner)
		{
			return false;
		}
		else if (!par1EntityLivingBase.isEntityAlive())
		{
			return false;
		}
		else if (!this.taskOwner.canAttackClass(par1EntityLivingBase.getClass()))
		{
			return false;
		}
		else
		{
			if (this.taskOwner instanceof IEntityOwnable && StringUtils.isNotEmpty(((IEntityOwnable)this.taskOwner).getOwnerName()))
			{
				if (par1EntityLivingBase instanceof IEntityOwnable && ((IEntityOwnable)this.taskOwner).getOwnerName().equals(((IEntityOwnable)par1EntityLivingBase).getOwnerName()))
				{
					return false;
				}

				if (par1EntityLivingBase == ((IEntityOwnable)this.taskOwner).getOwner())
				{
					return false;
				}
			}
			else if (par1EntityLivingBase instanceof EntityPlayer && !par2 && ((EntityPlayer)par1EntityLivingBase).capabilities.disableDamage)
			{
				return false;
			}

			if (!this.taskOwner.isWithinHomeDistance(MathHelper.floor_double(par1EntityLivingBase.posX), MathHelper.floor_double(par1EntityLivingBase.posY), MathHelper.floor_double(par1EntityLivingBase.posZ)))
			{
				return false;
			}
			else if (this.shouldCheckSight && !this.taskOwner.getEntitySenses().canSee(par1EntityLivingBase))
			{
				return false;
			}
			else
			{
				if (this.nearbyOnly)
				{
					if (--this.targetSearchDelay <= 0)
					{
						this.targetSearchStatus = 0;
					}

					if (this.targetSearchStatus == 0)
					{
						this.targetSearchStatus = this.canEasilyReach(par1EntityLivingBase) ? 1 : 2;
					}

					if (this.targetSearchStatus == 2)
					{
						return false;
					}
				}

				return true;
			}
		}
	}

	private boolean canEasilyReach(EntityLivingBase par1EntityLivingBase)
	{
		this.targetSearchDelay = 10 + this.taskOwner.getRNG().nextInt(5);
		PathEntity pathentity = this.taskOwner.getNavigator().getPathToEntityLiving(par1EntityLivingBase);

		if (pathentity == null)
		{
			return false;
		}
		else
		{
			PathPoint pathpoint = pathentity.getFinalPathPoint();

			if (pathpoint == null)
			{
				return false;
			}
			else
			{
				int i = pathpoint.xCoord - MathHelper.floor_double(par1EntityLivingBase.posX);
				int j = pathpoint.zCoord - MathHelper.floor_double(par1EntityLivingBase.posZ);
				return (double)(i * i + j * j) <= 2.25D;
			}
		}
	}
}