package insane96mcp.progressivebosses.modules.wither.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.utils.RandomHelper;
import insane96mcp.progressivebosses.ai.wither.WitherChargeAttackGoal;
import insane96mcp.progressivebosses.ai.wither.WitherDoNothingGoal;
import insane96mcp.progressivebosses.ai.wither.WitherRangedAttackGoal;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.RangedAttackGoal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

@Label(name = "Attack", description = "Makes the Wither smarter (will no longer try to stand on the player's head ...), attack faster and hit harder")
public class AttackFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Boolean> applyToVanillaWitherConfig;
	private final ForgeConfigSpec.ConfigValue<Double> chargeAttackChanceConfig;
	private final ForgeConfigSpec.ConfigValue<Double> maxChargeAttackChanceConfig;
	private final ForgeConfigSpec.ConfigValue<Double> barrageAttackChanceConfig;
	private final ForgeConfigSpec.ConfigValue<Double> maxBarrageAttackChanceConfig;
	private final ForgeConfigSpec.ConfigValue<Double> skullVelocityMultiplierConfig;
	private final ForgeConfigSpec.ConfigValue<Double> increasedAttackDamageConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> attackIntervalConfig;
	private final ForgeConfigSpec.ConfigValue<Boolean> increaseAttackSpeedWhenNearConfig;

	public boolean applyToVanillaWither = true;
	public double chargeAttackChance = 0.0015d;
	public double maxChargeAttackChance = 0.04d;
	public double barrageAttackChance = 0.002d;
	public double maxBarrageAttackChance = 0.05d;
	//Skulls
	public double skullVelocityMultiplier = 2.5d;
	public double increasedAttackDamage = 0.02d;
	//Attack Speed
	public int attackInterval = 40;
	public boolean increaseAttackSpeedWhenNear = true;

	public AttackFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		applyToVanillaWitherConfig = Config.builder
				.comment("If the AI changes should be applied to the first wither spawned too.")
				.define("Apply to Vanilla Wither", applyToVanillaWither);
		chargeAttackChanceConfig = Config.builder
				.comment("Chance (per difficulty) every time the Wither takes damage to start a charce attack. The chance is doubled when the Wither is below half health.")
				.defineInRange("Charge Attack Chance", chargeAttackChance, 0d, 1d);
		maxChargeAttackChanceConfig = Config.builder
				.comment("Max Chance for the charge attack. The max chance is doubled when the Wither is below half health.")
				.defineInRange("Charge Attack Chance", maxChargeAttackChance, 0d, 1d);
		barrageAttackChanceConfig = Config.builder
				.comment("Chance (per difficulty) every time the Wither takes damage to start a barrage attack. The chance is doubled when the Wither is below half health")
				.defineInRange("Barrage Attack Chance", barrageAttackChance, 0d, 1d);
		maxBarrageAttackChanceConfig = Config.builder
				.comment("Max Chance for the barrage attack. The max chance is doubled when the Wither is below half health")
				.defineInRange("Max Barrage Attack Chance", maxBarrageAttackChance, 0d, 1d);
		//Skulls
		Config.builder.comment("Wither Skull Changes").push("Skulls");
		skullVelocityMultiplierConfig = Config.builder
				.comment("Wither Skull Projectiles speed will be multiplied by this value.")
				.defineInRange("Skull Velocity Multiplier", skullVelocityMultiplier, 0d, Double.MAX_VALUE);
		increasedAttackDamageConfig = Config.builder
				.comment("Percentage bonus damage dealt by Wither skulls.")
				.defineInRange("Increased Attack Damage", increasedAttackDamage, 0d, Double.MAX_VALUE);
		Config.builder.pop();
		//Attack Speed
		Config.builder.comment("Attack Speed Changes").push("Attack Speed");
		attackIntervalConfig = Config.builder
				.comment("Every how many ticks (20 ticks = 1 seconds) the middle head will fire a projectile to the target.")
				.defineInRange("Attack Interval", attackInterval, 0, Integer.MAX_VALUE);
		increaseAttackSpeedWhenNearConfig = Config.builder
				.comment("The middle head will attack faster (up to 40% of the attack speed) the nearer the target is to the Wither.")
				.define("Increase Attack Speed when Near", increaseAttackSpeedWhenNear);
		Config.builder.pop();

		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.applyToVanillaWither = this.applyToVanillaWitherConfig.get();
		this.chargeAttackChance = this.chargeAttackChanceConfig.get();
		this.maxChargeAttackChance = this.maxChargeAttackChanceConfig.get();
		this.barrageAttackChance = this.barrageAttackChanceConfig.get();
		this.maxBarrageAttackChance = this.maxBarrageAttackChanceConfig.get();
		//Skulls
		this.skullVelocityMultiplier = this.skullVelocityMultiplierConfig.get();
		this.increasedAttackDamage = this.increasedAttackDamageConfig.get();
		//Attack Speed
		this.attackInterval = this.attackIntervalConfig.get();
		this.increaseAttackSpeedWhenNear = this.increaseAttackSpeedWhenNearConfig.get();
	}

	@SubscribeEvent
	public void onSpawn(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof WitherSkullEntity && this.isEnabled() && this.skullVelocityMultiplier > 0d){
			WitherSkullEntity witherSkullEntity = (WitherSkullEntity) event.getEntity();
			witherSkullEntity.accelerationX *= this.skullVelocityMultiplier;
			witherSkullEntity.accelerationY *= this.skullVelocityMultiplier;
			witherSkullEntity.accelerationZ *= this.skullVelocityMultiplier;
			return;
		}

		if (event.getWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();
		CompoundNBT compoundNBT = wither.getPersistentData();
		if ((!compoundNBT.contains(Strings.Tags.DIFFICULTY) || compoundNBT.getFloat(Strings.Tags.DIFFICULTY) == 0f) && !this.applyToVanillaWither)
			return;

		setWitherAI(wither);
	}

	@SubscribeEvent
	public void onUpdate(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntity().getEntityWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (this.chargeAttackChance == 0d || this.maxChargeAttackChance == 0d)
			return;

		if (!event.getEntity().isAlive())
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();
		CompoundNBT witherTags = wither.getPersistentData();
		if (witherTags.contains(Strings.Tags.CHARGE_ATTACK)){
			if (wither.ticksExisted % 10 == 0 && wither.getInvulTime() > 0) {
				wither.setHealth(wither.getHealth() - 10f + (wither.getMaxHealth() * 0.01f));
			}
			return;
		}
	}

	@SubscribeEvent
	public void onPlayerDamage(LivingHurtEvent event) {
		if (event.getEntity().getEntityWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (this.increasedAttackDamage == 0d /*&& this.chanceForWither3 == 0d*/)
			return;

		if (!(event.getSource().getImmediateSource() instanceof WitherSkullEntity) || !(event.getSource().getTrueSource() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getSource().getTrueSource();
		CompoundNBT compoundNBT = wither.getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		if (this.increasedAttackDamage > 0d)
			event.setAmount(event.getAmount() * (float)(1d + (this.increasedAttackDamage * difficulty)));
	}

	@SubscribeEvent
	public void onWitherDamageBarrage(LivingHurtEvent event) {
		if (event.getEntity().getEntityWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (this.barrageAttackChance == 0d || this.maxBarrageAttackChance == 0d)
			return;

		if (!event.getEntity().isAlive())
			return;

		if (!(event.getEntityLiving() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntityLiving();
		CompoundNBT witherTags = wither.getPersistentData();
		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);

		double chance = Math.min(this.barrageAttackChance * difficulty, this.maxBarrageAttackChance);
		if (wither.isCharged())
			chance *= 2d;
		if (RandomHelper.getDouble(wither.getRNG(), 0d, 1d) < chance) {
			int barrage = witherTags.getInt(Strings.Tags.BARRAGE_ATTACK);
			int duration = 50;
			if (wither.isCharged())
				duration *= 2;
			witherTags.putInt(Strings.Tags.BARRAGE_ATTACK, barrage + duration);
		}

	}

	@SubscribeEvent
	public void onWitherDamageCharge(LivingHurtEvent event) {
		if (event.getEntity().getEntityWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (this.chargeAttackChance == 0d || this.maxChargeAttackChance == 0d)
			return;

		if (!event.getEntity().isAlive())
			return;

		if (!(event.getEntityLiving() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntityLiving();
		CompoundNBT witherTags = wither.getPersistentData();
		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);

		double chance = Math.min(this.chargeAttackChance * difficulty, this.maxChargeAttackChance);
		if (wither.isCharged())
			chance *= 2d;
		if (RandomHelper.getDouble(wither.getRNG(), 0d, 1d) < chance) {
			wither.setInvulTime(70);
			witherTags.putBoolean(Strings.Tags.CHARGE_ATTACK, true);
		}

	}

	public void setWitherAI(WitherEntity wither) {
		ArrayList<Goal> toRemove = new ArrayList<>();
		wither.goalSelector.goals.forEach(goal -> {
			if (goal.getGoal() instanceof RangedAttackGoal)
				toRemove.add(goal.getGoal());
			if (goal.getGoal() instanceof WitherEntity.DoNothingGoal)
				toRemove.add(goal.getGoal());
		});

		toRemove.forEach(wither.goalSelector::removeGoal);

		wither.goalSelector.addGoal(0, new WitherDoNothingGoal(wither));
		wither.goalSelector.addGoal(2, new WitherRangedAttackGoal(wither,  this.attackInterval, 24.0f, this.increaseAttackSpeedWhenNear));
		wither.goalSelector.addGoal(2, new WitherChargeAttackGoal(wither));

		//Fixes https://bugs.mojang.com/browse/MC-29274
		wither.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(wither, PlayerEntity.class, 0, false, false, null));
	}
}
