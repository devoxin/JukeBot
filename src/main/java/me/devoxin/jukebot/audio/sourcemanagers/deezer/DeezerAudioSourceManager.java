package me.devoxin.jukebot.audio.sourcemanagers.deezer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeezerAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final Logger log = LoggerFactory.getLogger(DeezerAudioSourceManager.class);

    public static final Pattern URL_PATTERN = Pattern.compile("(https?://)?(www\\.)?deezer\\.com/(?<countrycode>[a-zA-Z]{2}/)?(?<type>track|album|playlist|artist)/(?<identifier>[0-9]+)");
    public static final String SEARCH_PREFIX = "dzsearch:";
    public static final String TRACK_SEARCH_PREFIX = "dztrack:";
    public static final String ISRC_PREFIX = "dzisrc:";
    public static final String SHARE_URL = "https://deezer.page.link/";
    public static final String PUBLIC_API_BASE = "https://api.deezer.com/2.0";
    public static final String PRIVATE_API_BASE = "https://www.deezer.com/ajax/gw-light.php";
    public static final String MEDIA_BASE = "https://media.deezer.com/v1";

    private final String masterDecryptionKey;

    private final HttpInterfaceManager httpInterfaceManager;

    public DeezerAudioSourceManager(final String masterDecryptionKey) {
        if (masterDecryptionKey == null || masterDecryptionKey.isEmpty()) {
            throw new IllegalArgumentException("Deezer master key must be set");
        }

        this.masterDecryptionKey = masterDecryptionKey;
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "deezer";
    }

    @Override
    public AudioItem loadItem(final AudioPlayerManager manager, final AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return this.getSearch(reference.identifier.substring(SEARCH_PREFIX.length()));
            }

            if (reference.identifier.startsWith(ISRC_PREFIX)) {
                return this.getTrackByISRC(reference.identifier.substring(ISRC_PREFIX.length()));
            }

            if (reference.identifier.startsWith(TRACK_SEARCH_PREFIX)) {
                return this.getTrackSearch(reference.identifier.substring(TRACK_SEARCH_PREFIX.length()));
            }

            if (reference.identifier.startsWith(SHARE_URL)) {
                final HttpGet request = new HttpGet(reference.identifier);
                request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());

                try (final CloseableHttpResponse response = this.httpInterfaceManager.getInterface().execute(request)) {
                    if (response.getStatusLine().getStatusCode() == 302) {
                        final String location = response.getFirstHeader("Location").getValue();

                        if (location.startsWith("https://www.deezer.com/")) {
                            return this.loadItem(manager, new AudioReference(location, reference.title));
                        }
                    }

                    return null;
                }
            }

            final Matcher matcher = URL_PATTERN.matcher(reference.identifier);

            if (!matcher.find()) {
                return null;
            }

            final String id = matcher.group("identifier");

            switch (matcher.group("type")) {
                case "album":
                    return this.getAlbum(id);
                case "track":
                    return this.getTrack(id);
                case "playlist":
                    return this.getPlaylist(id);
                case "artist":
                    return this.getArtist(id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public JsonBrowser getJson(final String uri) throws IOException {
        final HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", "application/json");
        return HttpClientTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
    }

    private List<AudioTrack> parseTracks(final JsonBrowser json) {
        return json.get("data").values().stream()
            .filter(track -> track.get("type").text().equals("track"))
            .map(this::parseTrack)
            .collect(Collectors.toList());
    }

    private AudioTrack parseTrack(final JsonBrowser json) {
        final String id = json.get("id").text();

        return new DeezerAudioTrack(new AudioTrackInfo(
            json.get("title").text(),
            json.get("artist").get("name").text(),
            json.get("duration").as(Long.class) * 1000,
            id,
            false,
            "https://deezer.com/track/" + id),
            json.get("isrc").text(),
            json.get("album").get("cover_xl").text(),
            this
        );
    }

    private AudioItem getTrackByISRC(final String isrc) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/track/isrc:" + isrc);

        if (json == null || json.get("id").isNull()) {
            return AudioReference.NO_TRACK;
        }

        return this.parseTrack(json);
    }

    private AudioItem getTrackSearch(final String query) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/search/track?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        System.out.println(PUBLIC_API_BASE + "/search/track?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));

        if (json == null || json.get("data").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        final List<AudioTrack> tracks = this.parseTracks(json);
        return new BasicAudioPlaylist("Deezer Search: " + query, tracks, null, true);
    }

    public AudioItem getSearch(final String query) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));

        if (json == null || json.get("data").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        final List<AudioTrack> tracks = this.parseTracks(json);
        return new BasicAudioPlaylist("Deezer Search: " + query, tracks, null, true);
    }

    private AudioItem getAlbum(final String id) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/album/" + id);

        if (json == null || json.get("tracks").get("data").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist(json.get("title").text(), this.parseTracks(json.get("tracks")), null, false);
    }

    private AudioItem getTrack(final String id) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/track/" + id);

        if (json == null) {
            return AudioReference.NO_TRACK;
        }

        return this.parseTrack(json);
    }

    private AudioItem getPlaylist(final String id) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/playlist/" + id);

        if (json == null || json.get("tracks").get("data").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist(json.get("title").text(), this.parseTracks(json.get("tracks")), null, false);
    }

    private AudioItem getArtist(final String id) throws IOException {
        final JsonBrowser json = this.getJson(PUBLIC_API_BASE + "/artist/" + id + "/top?limit=50");

        if (json == null || json.get("data").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist(json.get("data").index(0).get("artist").get("name").text() + "'s Top Tracks", this.parseTracks(json), null, false);
    }

    @Override
    public boolean isTrackEncodable(final AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(final AudioTrack track, final DataOutput output) throws IOException {
        final DeezerAudioTrack deezerAudioTrack = ((DeezerAudioTrack) track);
        DataFormatTools.writeNullableText(output, deezerAudioTrack.getISRC());
        DataFormatTools.writeNullableText(output, deezerAudioTrack.getArtworkURL());
    }

    @Override
    public AudioTrack decodeTrack(final AudioTrackInfo trackInfo, final DataInput input) throws IOException {
        return new DeezerAudioTrack(
            trackInfo,
            DataFormatTools.readNullableText(input),
            DataFormatTools.readNullableText(input),
            this
        );
    }

    @Override
    public void shutdown() {
        try {
            this.httpInterfaceManager.close();
        } catch (IOException e) {
            log.error("Failed to close HTTP interface manager", e);
        }
    }

    @Override
    public void configureRequests(final Function<RequestConfig, RequestConfig> configurator) {
        this.httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(final Consumer<HttpClientBuilder> configurator) {
        this.httpInterfaceManager.configureBuilder(configurator);
    }

    public String getMasterDecryptionKey() {
        return this.masterDecryptionKey;
    }

    public HttpInterface getHttpInterface() {
        return this.httpInterfaceManager.getInterface();
    }
}
