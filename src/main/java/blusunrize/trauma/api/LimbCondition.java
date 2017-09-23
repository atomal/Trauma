/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */
package blusunrize.trauma.api;

import blusunrize.trauma.api.effects.IEffectAttribute;
import blusunrize.trauma.api.effects.ITraumaEffect;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;

/**
 * A class representing the condition of a limb<br>
 * Stores limb, state, recovery timer and all applied TraumaEffects
 *
 * @author BluSunrize
 * @since 20.09.2017
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LimbCondition
{
	private final EnumLimb limb;
	private EnumTraumaState state = EnumTraumaState.NONE;
	private long recoveryTimer;
	private HashMap<String, ITraumaEffect> effects = new HashMap<>();

	public LimbCondition(EnumLimb limb)
	{
		this.limb = limb;
	}

	public EnumLimb getLimb()
	{
		return limb;
	}

	public EnumTraumaState getState()
	{
		return state;
	}

	public void setState(EnumTraumaState state)
	{
		this.state = state;
	}

	public long getRecoveryTimer()
	{
		return recoveryTimer;
	}

	public void setRecoveryTimer(long recoveryTimer)
	{
		this.recoveryTimer = recoveryTimer;
	}

	public HashMap<String, ITraumaEffect> getEffects()
	{
		return effects;
	}

	public void addEffect(ITraumaEffect effect)
	{
		this.effects.put(effect.getIndentifier(), effect);
	}

	public boolean hasEffect(String ident)
	{
		return this.effects.containsKey(ident);
	}

	public void removeEffect(String ident)
	{
		this.effects.remove(ident);
	}

	public void clearEffects(EntityPlayer player)
	{
		Multimap<String, AttributeModifier> attributeMap = HashMultimap.create();
		for(ITraumaEffect effect : getEffects().values())
			if(effect instanceof IEffectAttribute)
				((IEffectAttribute)effect).gatherModifiers(player, this, attributeMap);
		player.getAttributeMap().removeAttributeModifiers(attributeMap);
		this.effects.clear();
	}

	/**
	 * @return true, if the condition was cured
	 */
	public boolean tick(EntityPlayer player)
	{
		if(this.recoveryTimer>0 && --this.recoveryTimer<=0)
		{
			cure(player);
			return true;
		}
		return false;
	}

	/**
	 * Sets the state, recovery timer and all associated effects
	 * @param player the Player
	 * @param state given TraumaState
	 */
	public void assumeState(EntityPlayer player, EnumTraumaState state)
	{
		this.cure(player); //Cure first to clear everything
		this.setState(state);
		this.setRecoveryTimer(TraumaApiLib.getRecoveryTime(limb, state));
		for(ITraumaEffect effect : TraumaApiLib.getRegisteredEffects(getLimb(), state))
			this.addEffect(effect);
	}

	/**
	 * Cures this condition, resetting state, timer and all effects
	 */
	public void cure(EntityPlayer player)
	{
		this.setState(EnumTraumaState.NONE);
		this.recoveryTimer = 0;
		this.clearEffects(player);
	}

	public NBTTagCompound writeToNBT(@Nullable NBTTagCompound nbt)
	{
		if(nbt==null)
			nbt = new NBTTagCompound();
		nbt.setInteger("limb", limb.ordinal());
		nbt.setInteger("state", state.ordinal());
		nbt.setLong("recoveryTimer", recoveryTimer);
		return nbt;
	}

	public static LimbCondition readFromNBT(NBTTagCompound nbt)
	{
		EnumLimb limb = EnumLimb.values()[nbt.getInteger("limb")];
		LimbCondition limbCondition = new LimbCondition(limb);
		limbCondition.setState(EnumTraumaState.values()[nbt.getInteger("state")]);
		limbCondition.setRecoveryTimer(nbt.getLong("recoveryTimer"));
		for(ITraumaEffect effect : TraumaApiLib.getRegisteredEffects(limbCondition.getLimb(), limbCondition.getState()))
			limbCondition.addEffect(effect);
		return limbCondition;
	}
}
