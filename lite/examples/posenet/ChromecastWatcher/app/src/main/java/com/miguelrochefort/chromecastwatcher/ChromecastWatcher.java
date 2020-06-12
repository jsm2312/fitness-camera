package com.miguelrochefort.chromecastwatcher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import java.io.IOException;
import java.util.List;

public class ChromecastWatcher {
    public static final String CHROMECAST_SIGNATURE = "cast.media.CastMediaRouteProviderService";

    private MediaRouteSelector mSelector;
    private MediaRouter mMediaRouter;
    private CastDevice mSelectedDevice;
    private Cast.Listener mCastClientListener;
    private RemoteMediaPlayer mRemoteMediaPlayer;

    private GoogleApiClient mApiClient;
    private Context context;

    ChromecastWatcher(Context context) {
        this.context = context;
    }

//    @Override
    public void onCreate() {

        mMediaRouter = MediaRouter.getInstance(context);

        mSelector = new MediaRouteSelector.Builder()
                // These are the framework-supported intents
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();

        mMediaRouter.addCallback(mSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY | MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
    }

//    @Override
    public void onDestroy() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @UiThread
    private boolean isChromecastActive() {
        return getActiveChromecastRoute() != null;
    }

    @UiThread
    private Boolean isChromecastPlaying() {
        if (mRemoteMediaPlayer == null || mRemoteMediaPlayer.getMediaStatus() == null) {
            return null;
        }

        // Here you can get the playback status and the metadata for what's playing
        // But only after the onStatusUpdated() method is called in the mRemoteMediaPlayer callback
        int state = mRemoteMediaPlayer.getMediaStatus().getPlayerState();
        return (state == MediaStatus.PLAYER_STATE_BUFFERING || state == MediaStatus.PLAYER_STATE_PLAYING);
    }

    @UiThread
    private MediaRouter.RouteInfo getActiveChromecastRoute() {
        for (MediaRouter.RouteInfo route : mMediaRouter.getRoutes()) {
            if (isCastDevice(route)) {
                if (route.getConnectionState() == MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED) {
                    return route;
                }
            }
        }

        return null;
    }

    private int getMediaRouteVolume(@NonNull MediaRouter.RouteInfo route) {
        return route.getVolume();
    }

    private void setMediaRouteVolume(@NonNull MediaRouter.RouteInfo route, int volume) {
        route.requestSetVolume(volume);
    }

    private int getMediaRouteMaxVolume(@NonNull MediaRouter.RouteInfo route) {
        return route.getVolumeMax();
    }

    @UiThread
    private MediaRouter.RouteInfo getActiveMediaRoute() {
        if (isChromecastActive()) {
            MediaRouter.RouteInfo route = getActiveChromecastRoute();

            if (route != null) {
                if (!route.isSelected()) {
                    mMediaRouter.selectRoute(route);
                }
            }
            else if (mSelectedDevice != null) {
                mSelectedDevice = null;
            }

            return route;
        }

        return null;
    }

    private boolean isCastDevice(MediaRouter.RouteInfo routeInfo) {
        return routeInfo.getId().contains(CHROMECAST_SIGNATURE);
    }

    private MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            if (isCastDevice(route)) {
                Log.i("MediaRouter", "Chromecast found: " + route);
                if (route.getDescription().equals("Netflix")) {
                    onRouteSelected(router, route);
                }
            }
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (isCastDevice(route)) {
                Log.i("MediaRouter", "Chromecast changed: " + route);
            }
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            if (mSelectedDevice == null && isCastDevice(route)) {
                Log.i("MediaRouter", "Chromecast selected: " + route);

                mSelectedDevice = CastDevice.getFromBundle(route.getExtras());
                mCastClientListener = new Cast.Listener() {
                    @Override
                    public void onApplicationStatusChanged() {
                        Log.i("MediaRouter", "Cast.Listener.onApplicationStatusChanged()");
                    }

                    @Override
                    public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
                        Log.i("MediaRouter", "Cast.Listener.onApplicationMetadataChanged(" + applicationMetadata + ")");

                        if (applicationMetadata != null) {
                            LaunchOptions launchOptions = new LaunchOptions.Builder().setRelaunchIfRunning(false).build();
                            Cast.CastApi.launchApplication(mApiClient, applicationMetadata.getApplicationId(), launchOptions).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(@NonNull Cast.ApplicationConnectionResult applicationConnectionResult) {
                                    Log.i("MediaRouter", "Cast.CastApi.joinApplication.onResult() " + applicationConnectionResult.getSessionId());

                                    mRemoteMediaPlayer = new RemoteMediaPlayer();
                                    mRemoteMediaPlayer.setOnStatusUpdatedListener( new RemoteMediaPlayer.OnStatusUpdatedListener() {
                                        @Override
                                        public void onStatusUpdated() {
                                            MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                                            List<MediaTrack> tracks = mediaInfo.getMediaTracks();
                                            long mediaStreamDuration = mediaInfo.getStreamDuration();
                                            Object textTrackStyle = mediaInfo.getTextTrackStyle();
                                            MediaMetadata metadata = mediaInfo.getMetadata();
                                            String toString = metadata.toString();
                                            String namespace = mRemoteMediaPlayer.getNamespace();
                                            long streamPosition = mRemoteMediaPlayer.getApproximateStreamPosition();
                                            long streamDuration = mRemoteMediaPlayer.getStreamDuration();
                                            MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                                            int playerState = mediaStatus.getPlayerState();
                                            Object a = mediaStatus.getActiveTrackIds();
                                            Object b = mediaStatus.getCustomData();
                                            Object c = mediaStatus.getIdleReason();
                                            Object d = mediaStatus.getPlaybackRate();
                                            Object e = mediaStatus.getStreamPosition();
                                            Object f = mediaStatus.getStreamVolume();
                                            Log.i("MediaRouter", "Remote media player status " + playerState);
                                            Log.i("MediaRouter", "Remote media player mediaInfo " + mediaStatus.getPlayerState());
                                            Log.i("MediaRouter", "Remote media player namespace " + mediaStatus.getPlayerState());
                                            Log.i("MediaRouter", "Remote media player streamPosition " + mediaStatus.getPlayerState());
                                            Log.i("MediaRouter", "Remote media player streamDuration " + mediaStatus.getPlayerState());
                                            try {
                                                String artist = metadata.getString(MediaMetadata.KEY_ALBUM_ARTIST);
                                                String title = metadata.getString(MediaMetadata.KEY_TITLE);
                                            }
                                            catch(Exception ex) {

                                            }
                                            // TODO: you can call isChromecastPlaying() now
                                        }
                                    });

                                    try {
                                        Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
                                    } catch(IOException e) {
                                        Log.e("MediaRouter", "Exception while creating media channel ", e );
                                    } catch(NullPointerException e) {
                                        Log.e("MediaRouter", "Something wasn't reinitialized for reconnectChannels", e);
                                    }

                                    mRemoteMediaPlayer.requestStatus(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                        @Override
                                        public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                            Log.i("MediaRouter", "requestStatus() " + mediaChannelResult);
                                        }
                                    });

                                    try {
                                        Cast.CastApi.requestStatus(mApiClient);
                                    } catch (IOException e) {
                                        Log.e("MediaRouter", "Couldn't request status", e);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onApplicationDisconnected(int i) {
                        Log.i("MediaRouter", "Cast.Listener.onApplicationDisconnected(" + i + ")");
                    }

                    @Override
                    public void onActiveInputStateChanged(int i) {
                        Log.i("MediaRouter", "Cast.Listener.onActiveInputStateChanged(" + i + ")");
                    }

                    @Override
                    public void onStandbyStateChanged(int i) {
                        Log.i("MediaRouter", "Cast.Listener.onStandbyStateChanged(" + i + ")");
                    }

                    @Override
                    public void onVolumeChanged() {
                        Log.i("MediaRouter", "Cast.Listener.onVolumeChanged()");
                    }
                };

                Cast.CastOptions.Builder apiOptionsBuilder = new Cast.CastOptions.Builder(mSelectedDevice, mCastClientListener);

                mApiClient = new GoogleApiClient.Builder(context)
                        .addApi( Cast.API, apiOptionsBuilder.build() )
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(@Nullable Bundle bundle) {
                                Log.i("MediaRouter", "GoogleApiClient.onConnected()");
                                Log.i("MediaRouter", "Bundle " + bundle);
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.i("MediaRouter", "GoogleApiClient.onConnectionSuspended(" + i + ")");
                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                                Log.i("MediaRouter", "GoogleApiClient.onConnectionFailed()");
                            }
                        })
                        .build();

                mApiClient.connect();
            }
            else {
                mSelectedDevice = null;
                mRemoteMediaPlayer = null;
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            if (isCastDevice(route)) {
                if (mSelectedDevice != null && mSelectedDevice.isSameDevice(CastDevice.getFromBundle(route.getExtras()))) {
                    mSelectedDevice = null;
                }
                Log.i("MediaRouter", "Chromecast lost: " + route);
            }
        }
    };
}
