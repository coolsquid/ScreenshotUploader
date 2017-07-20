package coolsquid.screenshotuploader;

import java.io.InputStreamReader;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import coolsquid.screenshotuploader.Host.Result;
import coolsquid.screenshotuploader.org.apache.http.entity.mime.MultipartEntityBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.google.gson.Gson;

@Mod(modid = ScreenshotUploader.MODID, name = ScreenshotUploader.NAME, version = ScreenshotUploader.VERSION, dependencies = ScreenshotUploader.DEPENDENCIES, updateJSON = ScreenshotUploader.UPDATE_JSON, clientSideOnly = true)
public class ScreenshotUploader {

	public static final String MODID = "screenshotuploader";
	public static final String NAME = "ScreenshotUploader";
	public static final String VERSION = "1.0.0";
	public static final String DEPENDENCIES = "required-after:forge@[14.21.1.2387,)";
	public static final String UPDATE_JSON = "https://coolsquid.me/api/version/screenshotuploader.json";

	public static final KeyBinding KEY = new KeyBinding("ScreenshotUploader", Keyboard.KEY_U, "key.categories.misc");
	public static final Logger LOGGER = LogManager.getFormatterLogger(NAME);

	@Mod.EventHandler
	public void onInit(FMLInitializationEvent event) {
		ClientRegistry.registerKeyBinding(KEY);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent event) {
		if (MODID.equals(event.getModID())) {
			ConfigManager.sync(MODID, Type.INSTANCE);
		}
	}

	@SubscribeEvent
	public void onScreenshot(ScreenshotEvent event) {
		event.setResultMessage(new TextComponentTranslation("screenshot.success",
				new TextComponentString(event.getScreenshotFile().getName()).setStyle(new Style().setClickEvent(
						new ClickEvent(ClickEvent.Action.OPEN_FILE, event.getScreenshotFile().getAbsolutePath())))));
		event.setCanceled(true);
		boolean upload = KEY.isKeyDown();
		new Thread(() -> {
			try {
				ImageIO.write(event.getImage(), "png", event.getScreenshotFile());
			} catch (Exception e) {
				LogManager.getLogger(ScreenShotHelper.class).warn("Could not save screenshot", e);
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentTranslation("screenshot.failure", e.getMessage()));
			}
			if (upload) {
				Minecraft.getMinecraft().player
						.sendMessage(new TextComponentString("Attempting to upload screenshot..."));
				try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
					HttpPost request = new HttpPost(ModConfig.host.url);
					for (int i = 0; i < ModConfig.host.headers.length; i += 2) {
						request.setHeader(ModConfig.host.headers[i], ModConfig.host.headers[i + 1]);
					}
					MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
					ModConfig.host.addParameters(event, entityBuilder);
					request.setEntity(entityBuilder.build());
					HttpResponse response = client.execute(request);
					if (response.getStatusLine().getStatusCode() != 200) {
						ScreenshotUploader.LOGGER.error("Response code: %s. Response message: %s.",
								response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
						Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Could not upload screenshot"));
						return;
					}
					Result result = ModConfig.host.parse(
							new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()), Map.class));
					if (result == null) {
						LOGGER.error("Could not upload screenshot %s", event.getScreenshotFile().getName());
						Minecraft.getMinecraft().player
								.sendMessage(new TextComponentString("Could not upload screenshot"));
					} else {
						ScreenshotUploader.LOGGER.info("Uploaded screenshot to %s. Deletion url: %s.", result.url,
								result.deletionUrl);
						Minecraft
								.getMinecraft().player
										.sendMessage(
												new TextComponentString("The screenshot was successfully uploaded. ")
														.appendSibling(new TextComponentString("[URL]")
																.setStyle(new Style()
																		.setClickEvent(new ClickEvent(Action.OPEN_URL,
																				result.url))
																		.setColor(TextFormatting.BLUE)))
														.appendText(" ").appendSibling(
																new TextComponentString("[Delete]").setStyle(new Style()
																		.setClickEvent(new ClickEvent(Action.OPEN_URL,
																				result.deletionUrl))
																		.setColor(TextFormatting.BLUE))));
					}
				} catch (Exception e) {
					ScreenshotUploader.LOGGER.catching(e);
				}
			}
		}, NAME).start();
	}
}
