package me.devoxin.jukebot.audio.sources.deezer;

import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import me.devoxin.jukebot.audio.HighQualityAudioTrack;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.BiFunction;

public class DeezerAudioTrack extends DelegatedAudioTrack implements HighQualityAudioTrack {
    private final String isrc;
    private final String artworkURL;
    private final DeezerAudioSourceManager sourceManager;
    private boolean allowHighQuality = false;
    private final CookieStore cookieStore;

    private SourceWithFormat preparedSource = null;

    public DeezerAudioTrack(final AudioTrackInfo trackInfo, final String isrc, final String artworkURL,
                            final DeezerAudioSourceManager sourceManager) {
        super(trackInfo);
        this.isrc = isrc;
        this.artworkURL = artworkURL;
        this.sourceManager = sourceManager;
        this.cookieStore = new BasicCookieStore();
    }

    public String getISRC() {
        return this.isrc;
    }

    public String getArtworkURL() {
        return this.artworkURL;
    }

    @Override
    public void setAllowHighQuality(boolean allowHighQuality) {
        this.allowHighQuality = allowHighQuality;
    }

    private JsonBrowser getJsonResponse(HttpUriRequest request, boolean includeArl) {
        try (HttpInterface httpInterface = this.sourceManager.getHttpInterface()) {
            httpInterface.getContext().setRequestConfig(RequestConfig.custom().setCookieSpec("standard").build());
            httpInterface.getContext().setCookieStore(cookieStore);

            if (includeArl && this.sourceManager.getArl() != null) {
                request.setHeader("Cookie", "arl=" + this.sourceManager.getArl());
            }

            try (CloseableHttpResponse response = httpInterface.execute(request)) {
                return JsonBrowser.parse(response.getEntity().getContent());
            }
        } catch (IOException e) {
            throw ExceptionTools.toRuntimeException(e);
        }
    }

    private JsonBrowser generateLicenceToken(boolean useArl) {
        final HttpGet request = new HttpGet("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&input=3&api_version=1.0&api_token=");

        // session ID is not needed with ARL and vice-versa.
        if (!useArl || this.sourceManager.getArl() == null) {
            request.setHeader("Cookie", "sid=" + this.getSessionId());
        }

        return this.getJsonResponse(request, useArl);
    }

    private String getSessionId() {
        final HttpPost getSessionID = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=deezer.ping&input=3&api_version=1.0&api_token=");
        final JsonBrowser sessionIdJson = this.getJsonResponse(getSessionID, false);

        if (sessionIdJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get session ID");
        }

        return sessionIdJson.get("results").get("SESSION").text();
    }

    private SourceWithFormat getSource(boolean requestPremiumFormats, boolean isRetry) throws URISyntaxException {
        cookieStore.clear();
        JsonBrowser userTokenJson = this.generateLicenceToken(requestPremiumFormats);

        if (userTokenJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new RuntimeException("Failed to get user token");
        }

        final String userLicenseToken = userTokenJson.get("results").get("USER").get("OPTIONS").get("license_token").text();
        final String apiToken = userTokenJson.get("results").get("checkForm").text();

        final HttpPost getTrackToken = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=song.getData&input=3&api_version=1.0&api_token=" + apiToken);
        getTrackToken.setEntity(new StringEntity("{\"sng_id\":\"" + this.trackInfo.identifier + "\"}", ContentType.APPLICATION_JSON));

        final JsonBrowser trackTokenJson = this.getJsonResponse(getTrackToken, requestPremiumFormats);

        if (trackTokenJson.get("error").get("VALID_TOKEN_REQUIRED").text() != null && !isRetry) {
            // "error":{"VALID_TOKEN_REQUIRED":"Invalid CSRF token"}
            // seems to indicate an invalid API token?
            return this.getSource(requestPremiumFormats, true);
        }

        if (trackTokenJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get track token");
        }

        final String trackToken = trackTokenJson.get("results").get("TRACK_TOKEN").text();

        final HttpPost getMediaURL = new HttpPost(DeezerAudioSourceManager.MEDIA_BASE + "/get_url");
        final JsonStringWriter formatWriter = JsonWriter.string().array();

        for (TrackFormat format : TrackFormat.values()) {
            if (requestPremiumFormats || !format.isPremiumFormat) {
                // @formatter:off
                formatWriter
                    .object()
                        .value("cipher", "BF_CBC_STRIPE")
                        .value("format", format.name())
                    .end();
                // @formatter:on
            }
        }

        final String requestedFormats = formatWriter.end().done();
        getMediaURL.setEntity(new StringEntity("{\"license_token\":\"" + userLicenseToken + "\",\"media\":[{\"type\":\"FULL\",\"formats\":" + requestedFormats + "}],\"track_tokens\": [\"" + trackToken + "\"]}", ContentType.APPLICATION_JSON));

        final JsonBrowser mediaUrlJson = this.getJsonResponse(getMediaURL, requestPremiumFormats);
        JsonBrowser error = mediaUrlJson.get("data").index(0).get("errors").index(0);
        long errorCode = error.get("code").asLong(0);

        if (errorCode != 0) {
            // 2000 = error decoding track token
            // 2002 = track token has no sufficient rights on requested media

            // maybe because deezer didn't respond with a 'premium' licence token.
            // seems to happen sporadically, and not sure why, when other requests succeed.
            if (errorCode == 2000 && !isRetry) {
                return this.getSource(requestPremiumFormats, true);
            }

            if (requestPremiumFormats) {
                return this.getSource(false, false);
            }

            throw new FriendlyException("Failed to get media URL", Severity.COMMON, new RuntimeException("Code " + errorCode + " (message: " + error.get("message").safeText() + ")"));
        }

        return SourceWithFormat.fromResponse(mediaUrlJson.get("data").index(0).get("media").index(0), trackTokenJson);
    }

    private byte[] getTrackDecryptionKey() throws NoSuchAlgorithmException {
        final char[] md5 = Hex.encodeHex(MessageDigest.getInstance("MD5").digest(this.trackInfo.identifier.getBytes()), true);
        final byte[] master_key = this.sourceManager.getMasterDecryptionKey().getBytes();

        final byte[] key = new byte[16];

        for (int i = 0; i < 16; i++) {
            key[i] = (byte) (md5[i] ^ md5[i + 16] ^ master_key[i]);
        }

        return key;
    }

    public SourceWithFormat prepareSource() throws URISyntaxException {
        synchronized (this) {
            if (preparedSource == null) {
                return (preparedSource = this.getSource(allowHighQuality && this.sourceManager.getArl() != null, false));
            }
        }

        return preparedSource;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        SourceWithFormat source = preparedSource != null ? preparedSource : this.prepareSource();

        try (final HttpInterface httpInterface = this.sourceManager.getHttpInterface()) {
            try (final DeezerPersistentHttpStream stream = new DeezerPersistentHttpStream(httpInterface, source.url, source.contentLength, this.getTrackDecryptionKey())) {
                processDelegate(source.getTrackFactory().apply(this.trackInfo, stream), executor);
            }
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new DeezerAudioTrack(this.trackInfo, this.isrc, this.artworkURL, this.sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }

    public static class SourceWithFormat {
        public final URI url;
        public final TrackFormat format;
        public final long contentLength;

        private SourceWithFormat(String url, TrackFormat format, long contentLength) throws URISyntaxException {
            this.url = new URI(url);
            this.format = format;
            this.contentLength = contentLength;
        }

        private BiFunction<AudioTrackInfo, PersistentHttpStream, InternalAudioTrack> getTrackFactory() {
            return this.format.trackFactory;
        }

        private static SourceWithFormat fromResponse(JsonBrowser media, JsonBrowser trackJson) throws URISyntaxException {
            String format = media.get("format").text();
            TrackFormat trackFormat = TrackFormat.from(format);

            if (media.isNull() || trackFormat == null) {
                throw new RuntimeException("Could not find media URL");
            }

            JsonBrowser sources = media.get("sources");
            String url = sources.index(0).get("url").text();
            long contentLength = trackJson.get("results").get("FILESIZE_" + trackFormat.name()).asLong(Units.CONTENT_LENGTH_UNKNOWN);
            return new SourceWithFormat(url, trackFormat, contentLength);
        }
    }

    // N.B. Deezer will return formats based on the order they're given, with heavier
    // weighting based on distance from index [0].
    // That is to say, if you request [FLAC, MP3_320, MP3_256], Deezer will try and return
    // FLAC first if it exists, otherwise it falls back to 320 and then 256.
    // If any formats are added in future that supersede FLAC, they must be added above FLAC,
    // and vice-versa! AAC_64 can probably be bumped up here as it's in theory better quality than its
    // MP3 counterpart.
    public enum TrackFormat {
        FLAC(true, FlacAudioTrack::new),
        MP3_320(true, Mp3AudioTrack::new),
        MP3_256(true, Mp3AudioTrack::new),
        MP3_128(false, Mp3AudioTrack::new),
        MP3_64(false, Mp3AudioTrack::new),
        AAC_64(true, MpegAudioTrack::new); // not sure if this one is so better to be safe.

        private final boolean isPremiumFormat;
        private final BiFunction<AudioTrackInfo, PersistentHttpStream, InternalAudioTrack> trackFactory;

        TrackFormat(boolean isPremiumFormat,
                    BiFunction<AudioTrackInfo, PersistentHttpStream, InternalAudioTrack> trackFactory) {
            this.isPremiumFormat = isPremiumFormat;
            this.trackFactory = trackFactory;
        }

        static TrackFormat from(String format) {
            return Arrays.stream(TrackFormat.values())
                .filter(it -> it.name().equals(format))
                .findFirst()
                .orElse(null);
        }
    }
}
