package curses.mixin;

import curses.CursedEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityStepSoundMixin {
	@Inject(method = "playStepSound", at = @At("TAIL"))
	private void curses$handleCursedBootSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
		CursedEffects.onStep((Entity) (Object) this, state);
	}
}
