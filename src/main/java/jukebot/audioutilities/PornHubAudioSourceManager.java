package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;

public class PornHubAudioSourceManager implements AudioSourceManager, HttpConfigurable {

    static final String CHARSET = "UTF-8";
    private static final String VIDEO_REGEX = "^https?://www.pornhub.com/view_video.php\\?viewkey=[a-zA-Z0-9]{10,15}$";
    private static final Pattern VIDEO_INFO_REGEX = Pattern.compile("var flashvars_\\d{8,9} = (\\{.+})");
    private final HttpInterfaceManager httpInterfaceManager;

    private static final Pattern videoUrlPattern = Pattern.compile(VIDEO_REGEX);

    private static final Pattern[] validVideoPatterns = new Pattern[] {
            Pattern.compile(VIDEO_REGEX) // might need this for later idk
    };

    public PornHubAudioSourceManager() {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "pornhub";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        if (!videoUrlPattern.matcher(reference.identifier).matches())
            return null;

        try {
            return loadItemOnce(reference);
        } catch (FriendlyException exception) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.getCause())) {
                return loadItemOnce(reference);
            } else {
                throw exception;
            }
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        // No custom values that need saving
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new PornHubAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        IOUtils.closeQuietly(httpInterfaceManager);
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

    private AudioItem loadItemOnce(AudioReference reference) {
        try (HttpInterface httpInterface = getHttpInterface()) {
            JsonBrowser info = getVideoInfo(httpInterface, reference.identifier);
            if (info == null) {
                return AudioReference.NO_TRACK;
            }

            String playbackURL = null;

            for (JsonBrowser format : info.get("mediaDefinitions").values()) {
                if (!format.get("videoUrl").text().isEmpty()) {
                    playbackURL = format.get("videoUrl").text();
                    break;
                }
            }

            if (playbackURL == null) {
                return AudioReference.NO_TRACK;
            }

            String videoTitle = info.get("video_title").text();
            int videoDuration = Integer.parseInt(info.get("video_duration").text()) * 1000; // PH returns seconds

            return buildTrackObject(reference.identifier, playbackURL, videoTitle, "Unknown Uploader", false, videoDuration);
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a PornHub track failed.", FAULT, e);
        }
    }

    public JsonBrowser getVideoInfo(HttpInterface httpInterface, String videoURL) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(videoURL))) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new IOException("Invalid status code for video page response: " + statusCode);
            }

            String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
            Matcher match = VIDEO_INFO_REGEX.matcher(html);

            if (match.find()) {
                return JsonBrowser.parse(match.group(1));
            } else {
                return null;
            }
        }
    }

    public PornHubAudioTrack buildTrackObject(String uri, String identifier, String title, String uploader, boolean isStream, long duration) {
        return new PornHubAudioTrack(new AudioTrackInfo(title, uploader, duration, identifier, isStream, uri), this);
    }

}
