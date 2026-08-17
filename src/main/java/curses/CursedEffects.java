package curses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class CursedEffects {
	private static final float BUTTERFINGERS_CHANCE = 0.12F;
	private static final float REVERSE_KNOCKBACK_CHANCE = 0.35F;
	private static final float PACIFISTS_RAGE_CHANCE = 0.15F;
	private static final float GRAVITY_TAX_CHANCE = 0.20F;
	private static final float UNSTABLE_MENDING_CHANCE = 0.25F;
	private static final float GOAT_HORN_VOLUME = 12.0F;
	private static final ResourceLocation GOAT_HORN_SOUND = ResourceLocation.withDefaultNamespace("item.goat_horn.sound.0");
	private static final ResourceLocation HEAL_SOUND = ResourceLocation.withDefaultNamespace("entity.experience_orb.pickup");
	private static final Map<UUID, Long> CANCELLED_KNOCKBACK = new HashMap<>();

	private CursedEffects() {
	}

	public static void register() {
		AttackEntityCallback.EVENT.register(CursedEffects::onAttackEntity);
		ServerTickEvents.START_SERVER_TICK.register(server -> cleanupExpiredKnockbackCancellations(server.getTickCount()));
	}

	public static void onStep(Entity entity, BlockState state) {
		if (!(entity instanceof Player player) || player.level().isClientSide()) {
			return;
		}

		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

		if (boots.isEmpty()) {
			return;
		}

		if (getEnchantmentLevel(player.level().registryAccess(), boots, CursedEnchantments.LOUD_STEPS) > 0) {
			playSound(player.level(), player.position(), GOAT_HORN_SOUND, GOAT_HORN_VOLUME, 0.6F + (player.getRandom().nextFloat() * 0.25F));
		}

		if (getEnchantmentLevel(player.level().registryAccess(), boots, CursedEnchantments.GRAVITY_TAX) > 0 && player.getRandom().nextFloat() < GRAVITY_TAX_CHANCE) {
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 0, false, false, true));
			Vec3 slowedMovement = player.getDeltaMovement().multiply(0.35D, 1.0D, 0.35D);
			player.setDeltaMovement(slowedMovement.x, Math.min(slowedMovement.y, 0.0D), slowedMovement.z);
		}
	}

	public static boolean consumeKnockbackCancellation(LivingEntity entity) {
		cleanupExpiredKnockbackCancellations(entity.level().getGameTime());
		Long expiresAt = CANCELLED_KNOCKBACK.get(entity.getUUID());

		if (expiresAt == null) {
			return false;
		}

		if (expiresAt < entity.level().getGameTime()) {
			CANCELLED_KNOCKBACK.remove(entity.getUUID());
			return false;
		}

		CANCELLED_KNOCKBACK.remove(entity.getUUID());
		return true;
	}

	public static int maybeCorruptMending(Player player, int amount) {
		if (!(player.level() instanceof ServerLevel serverLevel) || amount <= 0 || player.getRandom().nextFloat() >= UNSTABLE_MENDING_CHANCE) {
			return -1;
		}

		List<ItemStack> candidates = new ArrayList<>();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);

			if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged() && getEnchantmentLevel(serverLevel.registryAccess(), stack, CursedEnchantments.UNSTABLE_MENDING) > 0) {
				candidates.add(stack);
			}
		}

		if (candidates.isEmpty()) {
			return -1;
		}

		ItemStack chosen = candidates.get(player.getRandom().nextInt(candidates.size()));
		int repairPerExperience = Math.max(1, EnchantmentHelper.getRepairWithExperience(serverLevel, chosen, 1));
		int damageRoom = Math.max(0, (chosen.getMaxDamage() - 1) - chosen.getDamageValue());

		if (damageRoom <= 0) {
			return -1;
		}

		int appliedDamage = Math.min(damageRoom, Math.min(chosen.getDamageValue(), amount * repairPerExperience));

		if (appliedDamage <= 0) {
			return -1;
		}

		chosen.setDamageValue(chosen.getDamageValue() + appliedDamage);
		return amount - Mth.ceil((float) appliedDamage / (float) repairPerExperience);
	}

	private static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (level.isClientSide() || !(entity instanceof LivingEntity target)) {
			return InteractionResult.PASS;
		}

		cleanupExpiredKnockbackCancellations(level.getGameTime());

		ItemStack weapon = player.getItemInHand(hand);

		if (weapon.isEmpty()) {
			return InteractionResult.PASS;
		}

		RegistryAccess registryAccess = level.registryAccess();
		boolean pacifistTriggered = false;

		if (getEnchantmentLevel(registryAccess, weapon, CursedEnchantments.BUTTERFINGERS) > 0 && player.getRandom().nextFloat() < BUTTERFINGERS_CHANCE) {
			ItemStack droppedWeapon = weapon.copy();
			player.setItemInHand(hand, ItemStack.EMPTY);
			player.drop(droppedWeapon, true);
			weapon = player.getItemInHand(hand);
		}

		if (weapon.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (getEnchantmentLevel(registryAccess, weapon, CursedEnchantments.REVERSE_KNOCKBACK) > 0 && player.getRandom().nextFloat() < REVERSE_KNOCKBACK_CHANCE) {
			CANCELLED_KNOCKBACK.put(target.getUUID(), level.getGameTime() + 1L);

			Vec3 reverseDirection = player.position().subtract(target.position());

			if (reverseDirection.horizontalDistanceSqr() < 1.0E-4D) {
				reverseDirection = new Vec3(0.0D, 0.0D, 1.0D);
			}

			reverseDirection = reverseDirection.normalize().scale(0.9D);
			player.push(reverseDirection.x, 0.2D, reverseDirection.z);
		}

		if (getEnchantmentLevel(registryAccess, weapon, CursedEnchantments.PACIFISTS_RAGE) > 0 && player.getRandom().nextFloat() < PACIFISTS_RAGE_CHANCE) {
			target.heal(4.0F);
			playSound(level, target.position(), HEAL_SOUND, 1.0F, 0.6F);
			pacifistTriggered = true;
		}

		return pacifistTriggered ? InteractionResult.FAIL : InteractionResult.PASS;
	}

	private static int getEnchantmentLevel(RegistryAccess registryAccess, ItemStack stack, ResourceKey<Enchantment> key) {
		if (stack.isEmpty()) {
			return 0;
		}

		Holder<Enchantment> enchantment = CursedEnchantments.holder(registryAccess, key);
		return EnchantmentHelper.getLevel(enchantment, stack);
	}

	private static void playSound(Level level, Vec3 position, ResourceLocation soundId, float volume, float pitch) {
		SoundEvent soundEvent = level.registryAccess()
			.lookupOrThrow(Registries.SOUND_EVENT)
			.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT, soundId))
			.value();

		level.playSound(null, position.x, position.y, position.z, soundEvent, SoundSource.PLAYERS, volume, pitch);
	}

	private static void cleanupExpiredKnockbackCancellations(long gameTime) {
		CANCELLED_KNOCKBACK.entrySet().removeIf(entry -> entry.getValue() < gameTime);
	}
}
