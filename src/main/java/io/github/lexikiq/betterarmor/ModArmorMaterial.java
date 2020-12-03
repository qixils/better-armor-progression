package io.github.lexikiq.betterarmor;

import io.github.lexikiq.betterarmor.utils.PlayerUtils;
import io.github.lexikiq.betterarmor.utils.Time;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Lazy;
import net.minecraft.world.World;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public enum ModArmorMaterial implements ArmorMaterial {
	STONE("stone", 4, new int[]{1, 1, 2, 1}, 9, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, 0.0D, 0, 0, Items.STONE, false, false),

	BLAZE("blaze", 15, new int[]{1, 4, 5, 2}, 12, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, 0.1D, 2, 1, Items.BLAZE_POWDER, false, false){
		private final double fireChance = 0.005; // .5%
		private final Random rand = new Random();

		private double getFireChance(EquipmentSlot slot) {return fireChance*(double)getIngredients(slot);}

		@Override
		public void armorTick(World world, Entity entity, int armorCount) {
			StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 8, 0, false, false);
			LivingEntity player = (LivingEntity) entity;
			player.addStatusEffect(effect);
		}

		@Override
		public void movementTick(World world, Entity entity) {
			world.addParticle(ParticleTypes.LARGE_SMOKE, entity.getParticleX(0.5D), entity.getRandomBodyY(), entity.getParticleZ(0.5D), 0.0D, 0.0D, 0.0D);
		}

		@Override
		public MutableText getTooltip(int n, EquipmentSlot slot, boolean isSetBonus, Object... args) {
			if (n == 2 && isSetBonus) { args=new Object[]{(int) ((rangedMultiplier-1)*100)}; }
			else if (n == 1 && !isSetBonus) { args=new Object[]{DF.format(100*getFireChance(slot))}; }
			return super.getTooltip(n, slot, isSetBonus, args);
		}

		@Override
		public void attackEntity(Entity entity, List<EquipmentSlot> slots) {
			if (entity.isOnFire()) {return;}
			double chance = slots.stream().map(this::getFireChance).mapToDouble(f -> f).sum();
			if (rand.nextDouble() <= chance) {
				entity.setOnFireFor(5);
			}
		}
	},

	// todo: crafting ingredient
	MARROW("marrow", 30, new int[]{3, 6, 7, 2}, 10, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 0.0F, 0.0F, 0.25D, 1, 0, Items.BONE, false, false),

	VOID("void", 30, new int[]{3, 6, 7, 2}, 10, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 0.5F, 0.0F, 0.0D, 2, 0, RegisterItems.VOID_FRAGMENT.getItem(), false, true){
		final double range = 1.0;

		@Override
		public void armorTick(World world, Entity entity, int count) {
			PlayerUtils.setRange((LivingEntity) entity, range);
		}

		@Override
		public void noArmorTick(World world, Entity entity) {
			PlayerUtils.setRange((LivingEntity) entity, null);
		}

		@Override
		public MutableText getTooltip(int n, EquipmentSlot slot, boolean isSetBonus, Object ... args) {
			if (n == 1 && isSetBonus) {
				args = new Object[]{range};
			}
			return super.getTooltip(n, slot, isSetBonus, args);
		}
	},

	OBSIDIAN("obsidian", 48, new int[]{4,9,10,5}, 9, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 3.0F, 0.175F, 0, 0, 1, Items.OBSIDIAN, true, false) {
		@Override
		public void armorTick(World world, Entity entity, int count) {
			StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.SLOWNESS, 8, count-1, false, false);
			LivingEntity player = (LivingEntity) entity;
			player.addStatusEffect(effect);
		}
	},

	// TODO: better repair ingredient than NETHER_STAR (create new wither drop item)
	WITHERING("withering", 45, new int[]{4, 8, 9, 5}, 18, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 4.0F, 0.25F, 0.5D, 2, 1, RegisterItems.NECRO_DUST.getItem(), false, false){
		private final double witherChance = 0.01; // 1%
		private final double witherSeconds = 5;
		private final int witherAmplifier = 1;
		private final Random rand = new Random();

		private double getWitherChance(EquipmentSlot slot) {return getWitherChance(getIngredients(slot));}
		private double getWitherChance(double multiplier) {return witherChance*multiplier;}

		@Override
		public MutableText getTooltip(int n, EquipmentSlot slot, boolean isSetBonus, Object... args) {
			if (n == 1 && isSetBonus) { args=new Object[]{DF.format(100*Arrays.stream(INGREDIENTS).mapToDouble(this::getWitherChance).sum()), witherSeconds-1, witherAmplifier+2}; }
			if (n == 2 && isSetBonus) { args=new Object[]{(int) ((rangedMultiplier-1)*100)}; }
			else if (n == 1 && !isSetBonus) { args=new Object[]{DF.format(100*getWitherChance(slot)), witherSeconds, witherAmplifier+1}; }
			return super.getTooltip(n, slot, isSetBonus, args);
		}

		@Override
		public void attackEntity(Entity entity, List<EquipmentSlot> slots) {
			if (!(entity instanceof LivingEntity)) {return;}

			double chance = slots.stream().map(this::getWitherChance).mapToDouble(f -> f).sum();
			boolean isFullSet = slots.size() == 4;
			int amplifier = witherAmplifier + (isFullSet ? 1 : 0);
			int duration = Time.SECOND.of(witherSeconds - (isFullSet ? 1 : 0));
			if (rand.nextDouble() <= chance) {
				StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.WITHER, duration, amplifier, false, false);
				((LivingEntity) entity).addStatusEffect(effect);
			}
		}
	}
	;

	protected static final int[] INGREDIENTS = new int[]{4, 7, 8, 5};
	protected static final int[] BASE_DURABILITY = new int[]{13, 15, 16, 11};
	private static final DecimalFormat DF = new DecimalFormat("0.0");
	protected final @Getter @Environment(EnvType.CLIENT) String name;
	protected final int durabilityMultiplier;
	protected final int[] protectionAmounts;
	protected final @Getter int enchantability;
	protected final @Getter SoundEvent equipSound;
	protected final @Getter float toughness;
	protected final @Getter float knockbackResistance;
	protected final @Getter double rangedMultiplier;
	protected final int setBonuses;
	protected final int pieceBonuses;
	protected final Lazy<Ingredient> repairIngredientSupplier;
	protected final boolean partialSet;
	protected final boolean noPearlDamage;

	ModArmorMaterial(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantability, SoundEvent equipSound, float toughness, float knockbackResistance, double rangedMultiplier, int bonuses, int tooltips, Supplier<Ingredient> supplier, boolean partialSet, boolean blocksPearlDamage)
	{
		this.name = name;
		this.durabilityMultiplier = durabilityMultiplier;
		this.protectionAmounts = protectionAmounts;
		this.enchantability = enchantability;
		this.equipSound = equipSound;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
		this.rangedMultiplier = 1.0D + rangedMultiplier;
		this.setBonuses = bonuses;
		this.pieceBonuses = tooltips;
		this.repairIngredientSupplier = new Lazy<>(supplier);
		this.partialSet = partialSet;
		this.noPearlDamage = blocksPearlDamage;
	}

	ModArmorMaterial(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantability, SoundEvent equipSound, float toughness, float knockbackResistance, double rangedMultiplier, int bonuses, int tooltips, Item repairItem, boolean partialSet, boolean blocksPearlDamage){
		this(name, durabilityMultiplier, protectionAmounts, enchantability, equipSound, toughness, knockbackResistance, rangedMultiplier, bonuses, tooltips, () -> Ingredient.ofItems(repairItem), partialSet, blocksPearlDamage);
	}

	public boolean allowsPartialSet() {
		return this.partialSet;
	}

	public boolean blocksPearlDamage() {
		return this.noPearlDamage;
	}

	public int getIngredients(EquipmentSlot slot) {
		return INGREDIENTS[slot.getEntitySlotId()];
	}

	public int getDurability(EquipmentSlot slot){
		return BASE_DURABILITY[slot.getEntitySlotId()] * this.durabilityMultiplier;
	}

	public int getProtectionAmount(EquipmentSlot slot){
		return this.protectionAmounts[slot.getEntitySlotId()];
	}

	public Ingredient getRepairIngredient(){
		return this.repairIngredientSupplier.get();
	}

	public void movementTick(World world, Entity entity) {}

	public void armorTick(World world, Entity entity, int count) {}

	public void armorTick(World world, Entity entity) {this.armorTick(world, entity, 4);}

	public void noArmorTick(World world, Entity entity) {}

	public void attackEntity(Entity entity, List<EquipmentSlot> armorCount) {}

	public MutableText getTooltip(int n, EquipmentSlot slot, boolean isSetBonus, Object ... args) {
		String key = isSetBonus ? "set" : "piece";
		return new TranslatableText("item."+BArmorMod.MOD_ID+"."+this.name.toLowerCase()+"."+key+n, args).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xb37fc9)));
	}

	public List<MutableText> getTooltips(EquipmentSlot slot) {
		List<MutableText> output = new ArrayList<>();
		if (setBonuses > 0) {
			output.add(new TranslatableText("barmorprog.set").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8e51a8)).withBold(true)));
			for (int i = 1; i <= setBonuses; i++) {
				output.add(getTooltip(i, slot, true));
			}
		}
		if (pieceBonuses > 0) {
			output.add(new TranslatableText("barmorprog.piece").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8e51a8)).withBold(true)));
			for (int i = 1; i <= pieceBonuses; i++) {
				output.add(getTooltip(i, slot, false));
			}
		}
		return output;
	}
}