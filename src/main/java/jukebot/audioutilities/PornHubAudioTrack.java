package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.net.URI;

public class PornHubAudioTrack extends DelegatedAudioTrack {

    private final PornHubAudioSourceManager sourceManager;

    public PornHubAudioTrack(AudioTrackInfo trackInfo, PornHubAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public AudioTrack makeClone() {
        return new PornHubAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            processStatic(localExecutor, httpInterface);
        }
    }

    private void processStatic(LocalAudioTrackExecutor localExecutor, HttpInterface httpInterface) throws Exception {
        try (PornHubPersistentHttpStream stream = new PornHubPersistentHttpStream(httpInterface, new URI(trackInfo.identifier), null)) {
            processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
        }
    }

}
