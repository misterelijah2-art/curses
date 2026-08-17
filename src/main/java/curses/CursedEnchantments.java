package curses;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class CursedEnchantments {
	public static final ResourceKey<Enchantment> BUTTERFINGERS = key("butterfingers");
	public static final ResourceKey<Enchantment> REVERSE_KNOCKBACK = key("reverse_knockback");
	public static final ResourceKey<Enchantment> PACIFISTS_RAGE = key("pacifists_rage");
	public static final ResourceKey<Enchantment> LOUD_STEPS = key("loud_steps");
	public static final ResourceKey<Enchantment> GRAVITY_TAX = key("gravity_tax");
	public static final ResourceKey<Enchantment> UNSTABLE_MENDING = key("unstable_mending");

	private CursedEnchantments() {
	}

	public static Holder<Enchantment> holder(RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
		return registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Curses.MOD_ID, path));
	}
}
