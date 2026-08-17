package curses;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Curses implements ModInitializer {
	public static final String MOD_ID = "curses";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CursedEffects.register();
		LOGGER.info("Loaded cursed enchantments for Fabric 1.21.11");
	}
}
