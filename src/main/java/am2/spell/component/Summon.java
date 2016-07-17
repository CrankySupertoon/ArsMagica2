package am2.spell.component;

import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import am2.api.affinity.Affinity;
import am2.defs.ItemDefs;
import am2.extensions.EntityExtension;
import am2.items.ItemOre;
import am2.power.PowerTypes;
import am2.spell.IComponent;
import am2.spell.SpellModifiers;
import am2.utils.EntityUtils;
import am2.utils.SpellUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class Summon implements IComponent{
	

	public EntityLiving summonCreature(ItemStack stack, EntityLivingBase caster, EntityLivingBase target, World world, double x, double y, double z){
		Class<? extends Entity> clazz = getSummonType(stack);
		EntityLiving entity = null;
		try{
			entity = (EntityLiving)clazz.getConstructor(World.class).newInstance(world);
		}catch (Throwable t){
			t.printStackTrace();
			return null;
		}

		if (entity == null){
			return null;
		}
		if (entity instanceof EntitySkeleton){
			((EntitySkeleton)entity).setSkeletonType(0);
			((EntitySkeleton)entity).setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BOW));
		}else if (entity instanceof EntityHorse && caster instanceof EntityPlayer){
			((EntityHorse)entity).setTamedBy(((EntityPlayer)caster));
		}
		entity.setPosition(x, y, z);
		world.spawnEntityInWorld(entity);
		if (caster instanceof EntityPlayer){
			EntityUtils.makeSummon_PlayerFaction((EntityCreature)entity, (EntityPlayer)caster, false);
		}else{
			EntityUtils.makeSummon_MonsterFaction((EntityCreature)entity, false);
		}
		EntityUtils.setOwner(entity, caster);

		int duration = SpellUtils.getModifiedInt_Mul(4800, stack, caster, target, world, SpellModifiers.DURATION);

		EntityUtils.setSummonDuration(entity, duration);

		SpellUtils.applyStageToEntity(stack, caster, world, entity, false);

		return entity;
	}

	@Override
	public Object[] getRecipe(){
		//Chimerite, purified vinteum, blue orchid, monster focus, any filled crystal phylactery, 1500 dark power
		return new Object[]{
				new ItemStack(ItemDefs.itemOre, 1, ItemOre.META_CHIMERITE),
				new ItemStack(ItemDefs.itemOre, 1, ItemOre.META_PURIFIED_VINTEUM),
				//TODO BlocksCommonProxy.cerublossom,
				ItemDefs.mobFocus,
				//TODO new ItemStack(ItemDefs.crystalPhylactery, 1, ItemDefs.crystalPhylactery.META_FULL),
				"E:" + PowerTypes.DARK.ID(), 1500
		};
	}

	public void setSummonType(ItemStack stack, ItemStack phylacteryStack){
//		if (phylacteryStack.getItemDamage() == ItemDefs.crystalPhylactery.META_FULL && phylacteryStack.getItem() instanceof ItemCrystalPhylactery){
//			if (!stack.hasTagCompound())
//				stack.setTagCompound(new NBTTagCompound());
//
//			setSummonType(stack, ItemDefs.crystalPhylactery.getSpawnClass(phylacteryStack));
//		}
	}

	public Class<? extends Entity> getSummonType(ItemStack stack){
		String s = SpellUtils.getSpellMetadata(stack, "SummonType");
		if (s == null || s == "")
			s = "Skeleton"; //default!  default!  default!
		Class<? extends Entity> clazz = (Class<? extends Entity>)EntityList.NAME_TO_CLASS.get(s);
		return clazz;
	}

	public void setSummonType(ItemStack stack, String s){
		Class<? extends Entity> clazz = (Class<? extends Entity>)EntityList.NAME_TO_CLASS.get(s);
		setSummonType(stack, clazz);
	}

	public void setSummonType(ItemStack stack, Class<? extends Entity> clazz){
		if (!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());

		clazz = checkForSpecialSpawns(stack, clazz);

		String s = (String)EntityList.CLASS_TO_NAME.get(clazz);
		if (s == null)
			s = "";

		SpellUtils.setSpellMetadata(stack, "SpawnClassName", s);
		SpellUtils.setSpellMetadata(stack, "SummonType", s);
	}

	private Class<? extends Entity> checkForSpecialSpawns(ItemStack stack, Class<? extends Entity> clazz){
//		if (clazz == EntityChicken.class){
//			if (SpellUtils.modifierIsPresent(SpellModifiers.DAMAGE, stack) && SpellUtils.componentIsPresent(stack, Haste.class)){
//				return EntityBattleChicken.class;
//			}
//		}else if (clazz == EntityCow.class){
//			if (SpellUtils.modifierIsPresent(SpellModifiers.DAMAGE, stack) && SpellUtils.componentIsPresent(stack, AstralDistortion.class)){
//				return EntityHellCow.class;
//			}
//		}
		return clazz;
	}

	@Override
	public boolean applyEffectBlock(ItemStack stack, World world, BlockPos blockPos, EnumFacing blockFace, double impactX, double impactY, double impactZ, EntityLivingBase caster){
		if (!world.isRemote){
			if (EntityExtension.For(caster).getCanHaveMoreSummons()){
				if (summonCreature(stack, caster, caster, world, impactX, impactY, impactZ) == null){
					return false;
				}
			}else{
				if (caster instanceof EntityPlayer){
					((EntityPlayer)caster).addChatMessage(new TextComponentString(I18n.translateToLocal("am2.tooltip.noMoreSummons")));
				}
			}
		}

		return true;
	}

	@Override
	public boolean applyEffectEntity(ItemStack stack, World world, EntityLivingBase caster, Entity target){

		if (target instanceof EntityLivingBase && EntityUtils.isSummon((EntityLivingBase)target))
			return false;

		if (!world.isRemote){
			if (EntityExtension.For(caster).getCanHaveMoreSummons()){
				if (summonCreature(stack, caster, caster, world, target.posX, target.posY, target.posZ) == null){
					return false;
				}
			}else{
				if (caster instanceof EntityPlayer){
					((EntityPlayer)caster).addChatComponentMessage(new TextComponentString(I18n.translateToLocal("am2.tooltip.noMoreSummons")));
				}
			}
		}

		return true;
	}

	@Override
	public float manaCost(EntityLivingBase caster){
		return 400;
	}

	@Override
	public ItemStack[] reagents(EntityLivingBase caster){
		return null;
	}

	@Override
	public void spawnParticles(World world, double x, double y, double z, EntityLivingBase caster, Entity target, Random rand, int colorModifier){

	}

	@Override
	public Set<Affinity> getAffinity(){
		return Sets.newHashSet(Affinity.ENDER, Affinity.LIFE);
	}

	@Override
	public float getAffinityShift(Affinity affinity){
		return 0.01f;
	}

	@Override
	public void encodeBasicData(NBTTagCompound tag, Object[] recipe) {}
}