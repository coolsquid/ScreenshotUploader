package coolsquid.screenshotuploader;

import net.minecraftforge.common.config.Config;

@Config.LangKey("screenshotuploader.config.general")
@Config(modid = ScreenshotUploader.MODID, name = ScreenshotUploader.NAME)
public class ModConfig {

	@Config.Comment("Which image host to use.")
	public static Host host = Host.IMGUR;

	@Config.Comment("The title to use for the image post. Imgur only.")
	public static String title = "Minecraft screenshot";

	@Config.Comment("The description to use for the image post. Imgur only.")
	public static String description = "Minecraft screenshot";

	@Config.Comment("The number of days to keep the screenshot stored on the server. Set to 0 to keep it forever. Lutim only.")
	@Config.RangeInt(min = 0, max = 365)
	public static int retentionTime = 0;
}