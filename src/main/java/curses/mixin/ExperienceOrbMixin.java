package curses.mixin;

import curses.CursedEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {
	@Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
	private void curses$corruptMending(Player player, int amount, CallbackInfoReturnable<Integer> cir) {
		int remaining = CursedEffects.maybeCorruptMending(player, amount);

		if (remaining >= 0) {
			cir.setReturnValue(remaining);
		}
	}
}
