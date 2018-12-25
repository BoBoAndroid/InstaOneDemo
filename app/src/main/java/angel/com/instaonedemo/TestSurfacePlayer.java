package angel.com.instaonedemo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import com.arashivision.arplayer.GlTarget;
import org.rajawali3d.materials.textures.ISurfacePlayer;

import java.io.FileDescriptor;

/**
 * Created by xieyuming on 2017/9/13.
 */

public class TestSurfacePlayer implements ISurfacePlayer {
    @Override
    public boolean useGLTarget() {
        return false;
    }

    @Override
    public void onCreateGLTarget(GlTarget glTarget) {

    }

    @Override
    public void onReleaseGLTarget(GlTarget glTarget) {

    }

    @Override
    public void onCreateSurface(Surface surface) {

    }

    @Override
    public void onReleaseSurface(Surface surface) {

    }

    @Override
    public void initPlayer() {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setDataSource(String s) {

    }

    @Override
    public void setDataSource(Context context, Uri uri) {

    }

    @Override
    public void setDataSource(FileDescriptor fileDescriptor) {

    }

    @Override
    public void setDataSource(FileDescriptor fileDescriptor, long l, long l1) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setLooping(boolean b) {

    }

    @Override
    public void seekTo(int i) {

    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void setVolume(float v) {

    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public String getGyro() {
        return null;
    }

    @Override
    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {

    }

    @Override
    public void setOnErrorListener(OnErrorListener onErrorListener) {

    }

    @Override
    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {

    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener onSeekCompleteListener) {

    }

    @Override
    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {

    }

    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener onBufferingUpdateListener) {

    }

    @Override
    public void setOnRenderingFpsUpdateListener(OnRenderingFpsUpdateListener onRenderingFpsUpdateListener) {

    }

    @Override
    public void setOnRendererFpsReportListener(OnRendererFpsReportListener onRendererFpsReportListener) {

    }

    @Override
    public void setOption(String s, boolean b) {

    }

    @Override
    public void setOption(String s, int i) {

    }

    @Override
    public void setOption(String s, long l) {

    }

    @Override
    public void setOption(String s, double v) {

    }

    @Override
    public boolean readExtras() {
        return false;
    }

    @Override
    public Bundle getExtra() {
        return null;
    }

    @Override
    public void setOnInfoListener(OnInfoListener onInfoListener) {

    }
}
