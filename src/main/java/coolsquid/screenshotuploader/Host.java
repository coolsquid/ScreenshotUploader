package coolsquid.screenshotuploader;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import net.minecraftforge.client.event.ScreenshotEvent;

import coolsquid.screenshotuploader.org.apache.http.entity.mime.MultipartEntityBuilder;

public enum Host {

	IMGUR("https://api.imgur.com/3/image/", "authorization", "Client-ID 8020927d0fc458f") {

		@Override
		public void addParameters(ScreenshotEvent event, MultipartEntityBuilder builder) {
			builder.addTextBody("title", ModConfig.title);
			builder.addTextBody("description", ModConfig.description);
			builder.addTextBody("type", "file");
			builder.addBinaryBody("image", event.getScreenshotFile());
		}

		@Override
		public Result parse(Map<String, Object> response) {
			if (response == null || !(boolean) response.get("success")) {
				ScreenshotUploader.LOGGER.error(response);
				return null;
			}
			Map<String, Object> map = (Map<String, Object>) response.get("data");
			String url = ((String) map.get("link")).replace('\\', '/').replace("http", "https");
			String deletionUrl = "https://imgur.com/delete/" + map.get("deletehash");
			return new Result(url, deletionUrl);
		}
	},
	LUTIM("https://lut.im/") {

		@Override
		public void addParameters(ScreenshotEvent event, MultipartEntityBuilder builder) {
			builder.addTextBody("name", "screenshot.png");
			builder.addTextBody("delete-day", Integer.toString(ModConfig.retentionTime));
			builder.addTextBody("format", "json");
			builder.addBinaryBody("file", event.getScreenshotFile());
		}

		@Override
		public Result parse(Map<String, Object> response) {
			if (response == null || !(boolean) response.get("success")) {
				ScreenshotUploader.LOGGER.error(response);
				return null;
			}
			Map<String, Object> map = (Map<String, Object>) response.get("msg");
			String url = "https://lut.im/" + map.get("short");
			String deletionUrl = "https://lut.im/d/" + map.get("real_short") + "/" + map.get("token");
			return new Result(url, deletionUrl);
		}
	};

	public abstract void addParameters(ScreenshotEvent event, MultipartEntityBuilder builder);

	public abstract Result parse(Map<String, Object> response);

	public final String url;
	public final String[] headers;

	private Host(String url, String... headers) {
		this.url = url;
		this.headers = headers;
	}

	public static class Result {

		public final String url, deletionUrl;

		public Result(String url, String deletionUrl) {
			this.url = url;
			this.deletionUrl = deletionUrl;
		}
	}

	static final String BOUNDARY = UUID.randomUUID().toString();
	static final byte[] BOUNDARY_BYTES = ("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8);
}