package curses.mixin;

import curses.CursedEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityKnockbackMixin {
	@Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
	private void curses$cancelReverseKnockback(double strength, double x, double z, CallbackInfo ci) {
		if (CursedEffects.consumeKnockbackCancellation((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
