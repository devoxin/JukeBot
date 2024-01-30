package me.devoxin.jukebot.audio.sources.deezer;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DeezerAudioTrack extends DelegatedAudioTrack {
    private final String isrc;
    private final String artworkURL;
    private final DeezerAudioSourceManager sourceManager;

    public DeezerAudioTrack(final AudioTrackInfo trackInfo, final String isrc, final String artworkURL,
                            final DeezerAudioSourceManager sourceManager) {
        super(trackInfo);
        this.isrc = isrc;
        this.artworkURL = artworkURL;
        this.sourceManager = sourceManager;
    }

    public String getISRC() {
        return this.isrc;
    }

    public String getArtworkURL() {
        return this.artworkURL;
    }

    private URI getTrackMediaURI() throws IOException, URISyntaxException {
        final HttpPost getSessionID = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=deezer.ping&input=3&api_version=1.0&api_token=");
        final JsonBrowser sessionIdJson = HttpClientTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getSessionID);

        if (sessionIdJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get session ID");
        }

        final String sessionId = sessionIdJson.get("results").get("SESSION").text();

        final HttpPost getUserToken = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=deezer.getUserData&input=3&api_version=1.0&api_token=");
        getUserToken.setHeader("Cookie", "sid=" + sessionId);
        final JsonBrowser userTokenJson = HttpClientTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getUserToken);

        if (userTokenJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get user token");
        }

        final String userLicenseToken = userTokenJson.get("results").get("USER").get("OPTIONS").get("license_token").text();
        final String apiToken = userTokenJson.get("results").get("checkForm").text();

        final HttpPost getTrackToken = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=song.getData&input=3&api_version=1.0&api_token=" + apiToken);
        getTrackToken.setEntity(new StringEntity("{\"sng_id\":\"" + this.trackInfo.identifier + "\"}", ContentType.APPLICATION_JSON));
        final JsonBrowser trackTokenJson = HttpClientTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getTrackToken);

        if (trackTokenJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get track token");
        }

        final String trackToken = trackTokenJson.get("results").get("TRACK_TOKEN").text();

        final HttpPost getMediaURL = new HttpPost(DeezerAudioSourceManager.MEDIA_BASE + "/get_url");
        getMediaURL.setEntity(new StringEntity("{\"license_token\":\"" + userLicenseToken + "\",\"media\": [{\"type\": \"FULL\",\"formats\": [{\"cipher\": \"BF_CBC_STRIPE\", \"format\": \"MP3_128\"}]}],\"track_tokens\": [\"" + trackToken + "\"]}", ContentType.APPLICATION_JSON));
        final JsonBrowser mediaUrlJson = HttpClientTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getMediaURL);

        if (mediaUrlJson.get("data").index(0).get("errors").index(0).get("code").asLong(0) != 0) {
            throw new IllegalStateException("Failed to get media URL");
        }

        return new URI(mediaUrlJson.get("data").index(0).get("media").index(0).get("sources").index(0).get("url").text());
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

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (final HttpInterface httpInterface = this.sourceManager.getHttpInterface()) {
            try (final DeezerPersistentHttpStream stream = new DeezerPersistentHttpStream(httpInterface, this.getTrackMediaURI(), this.trackInfo.length, this.getTrackDecryptionKey())) {
                processDelegate(new Mp3AudioTrack(this.trackInfo, stream), executor);
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
}
