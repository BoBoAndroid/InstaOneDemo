package angel.com.instaonedemo;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.arashivision.onecamera.*;
import com.arashivision.onecamera.cameranotification.*;
import com.arashivision.onecamera.cameraresponse.*;
import com.arashivision.onecamera.render.RenderMode;
import org.greenrobot.eventbus.EventBus;

import java.util.List;


public class CameraService extends OneCamera {
    private static final String TAG = "CameraService";
    private static CameraService mInstance;
    private static final Object mSyncObject = new Object();

    public static CameraService instance(Context context) {
        synchronized(mSyncObject) {
            if(mInstance != null)
                return mInstance;
            Context appContext = context.getApplicationContext();
            OneCameraCallback callback = new OneCameraCallback();
            // Pass a null callback handler to OneCamera we let it execute callbacks on its thread.
            // Here we just post event to the receiver in those callback, so it doesn't matter
            Handler callbackHandler = null;
//            OneCamera.setCameraDevMode(true); // use for Air2 develop
            mInstance = new CameraService(appContext, callback, callbackHandler);
            mInstance.switchRenderMode(RenderMode.directDecoding());
            return mInstance;
        }
    }


    private CameraService(Context context, OneCallbacks callbacks, Handler handler) {
        super(context, callbacks, handler);
        setInfoUpdateListener(null, new InfoUpdateListener() {
            @Override
            public void onRecordFpsUpdate(int fps) {
                EventBus.getDefault().post(new RecordFpsUpdateEvent(fps));
            }

            @Override
            public void onLivePushStarted(String url) {
                EventBus.getDefault().post(new LivePushStartedEvent(url));
            }

            @Override
            public void onCameraInfoNotify(int what,int error,Object obj)
            {
               Log.d(TAG,"what " + what + " error " + error);
                //open if need debug 171113
                switch (what) {
                    case OneDriverInfo.Response.InfoType.FIRMWARE_UPGRADE_COMPLETE:
                        break;
                    case OneDriverInfo.Response.InfoType.RECORD_AUTO_SPLIT: {
//                        NotificationCaptureAutoSplit notify = (NotificationCaptureAutoSplit) obj;
//                        Log.d(TAG, "uri " + notify.mVideo.uri);
//                        Log.d(TAG, "file_size " + notify.mVideo.file_size);
//                        Log.d(TAG, "total_time " + notify.mVideo.total_time);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.BATTERY_UPDATE: {
                        BatteryStatus notify = (BatteryStatus) obj;
                        Log.d(TAG, "power_type " + notify.power_type);
                        Log.d(TAG, "battery_scale " + notify.battery_scale);
                       Log.d(TAG, "battery_level " + notify.battery_level);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.BATTERY_LOW: {
                        BatteryStatus notify = (BatteryStatus) obj;
                        Log.d(TAG,"power_type " + notify.power_type);
                        Log.d(TAG,"battery_scale " + notify.battery_scale);
                        Log.d(TAG,"battery_level " + notify.battery_level);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.SHUTDOWN: {
//                        NotificationShutdown notify = (NotificationShutdown) obj;
//                        Log.d(TAG,"err_code " + notify.err_code);
//                        Log.d(TAG,"message " + notify.message);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.STORAGE_UPDATE: {
//                        NotificationCardUpdate notify = (NotificationCardUpdate) obj;
//                        Log.d(TAG,"card_state " + notify.card_state);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.STORAGE_FULL: {
//                        Log.d(TAG,"storage full");
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.BUTTON_PRESSED: {
                        NotificatoinKeyPressed notify = (NotificatoinKeyPressed) obj;
//                        Log.d(TAG,"key_id " + notify.key_id);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.RECORD_STOPPED: {
                        NotificationCaptureStopped notify = (NotificationCaptureStopped) obj;
//                        Log.d(TAG,"err_code " + notify.err_code);
//                        Log.d(TAG, "uri " + notify.mVideo.uri);
//                        Log.d(TAG, "file_size " + notify.mVideo.file_size);
//                        Log.d(TAG, "total_time " + notify.mVideo.total_time);
                        break;
                    }
                    case OneDriverInfo.Response.InfoType.CAPTURE_STILL_IMAGE_STATE_UPDATE:
                    {
                        NotificationTakePictureStateUpdate notify = (NotificationTakePictureStateUpdate) obj;
//                        Log.d(TAG,"state " + notify.state);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.DELETE_FILES:
                    {
                        DeleteFilesResp notify = (DeleteFilesResp)obj;
//                        Log.d(TAG,"delete notify id " + notify.requestID);
                    }
                        break;
                    //deprecated
                    case OneDriverInfo.Response.InfoType.PHONE_INSERT: {
//                        Log.d(TAG, "PhotoInsert");
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.BT_DISCOVER_PERIPHERAL: {
                        NotificatoinDiscoverBTPeripheral notify = (NotificatoinDiscoverBTPeripheral) obj;
                        for(BTPeripheral mBT:notify.peripherals)
                        {
//                            Log.d(TAG," name " + mBT.name + " mac len " + mBT.mac_addr.length);
                            EventBus.getDefault().post(new BleDiscoverEvent(mBT));
                        }
                        break;
                    }
                    case OneDriverInfo.Response.InfoType.BT_CONNECTED_TO_PERIPHERAL: {
                        NotificatoinConnectedToPeripheral notify = (NotificatoinConnectedToPeripheral) obj;
//                        Log.d(TAG,"connect mac " + notify.peripheral.mac_addr.length
//                                + " name " + notify.peripheral.name);
                        EventBus.getDefault().post(new BleConnectEvent(notify.peripheral));
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.BT_DISCONNECTED_PERIPHERAL: {
                        NotificatoinDisconnectedPeripheral notify = (NotificatoinDisconnectedPeripheral) obj;
//                        Log.d(TAG,"disconnect mac " +
//                                notify.peripheral.mac_addr.length +
//                                " name " + notify.peripheral.name);
                        EventBus.getDefault().post(new BleDisconnectEvent(notify.peripheral));
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.CANCEL_CAPTURE:
                        break;
                    //async response
                    case OneDriverInfo.Response.InfoType.GET_FILE_EXTRA:
                    {
                       GetFileExtraResp notify = (GetFileExtraResp)obj;
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.GET_FILE_LIST:
                    {
                        GetFileListResp notify =  (GetFileListResp)obj;
                        EventBus.getDefault().post(new UsbFileList(notify.mUriList));
                        /*for (int i = 0; i < notify.mUriList.size(); i++) {
                            Log.d(TAG,"GetFileListResp " + notify.mUriList.get(i));
                        }*/
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.SET_FILE_EXTRA:
                    {

                    }
                    break;
                    case OneDriverInfo.Response.InfoType.ERASE_SD_CARD:
                    {
//                        EraseSdcardResp notify = (EraseSdcardResp)obj;
//                        Log.d(TAG,"erase notify id " + notify.requestID);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.CALIBRATE_GYRO:
                    {

                    }
                    break;
                    case OneDriverInfo.Response.InfoType.GET_MINI_THUMBNAIL:
                    {
                       GetMiniThumbResp notify = (GetMiniThumbResp)obj;
                       Log.d(TAG,"GET_MINI_THUMBNAIL thumb len " + notify.thumb.length);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.TEST_SD_CARD_SPEED:
                    {
//                        TestSDCardSpeedResp notify = (TestSDCardSpeedResp)obj;
//                        Log.d(TAG,"TEST_SD_CARD_SPEED thumb len " + notify.write_speeds.length);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.START_LIVE_STREAM:
                    {
//                        StartStreamResp notify = (StartStreamResp)obj;
//                        Log.d(TAG,"start id " + notify.requestID + " notify res " + notify.result);
                    }
                    break;
                    case OneDriverInfo.Response.InfoType.STOP_LIVE_STREAM:
                    {
//                        StopStreamResp notify = (StopStreamResp)obj;
//                        Log.d(TAG,"stop id " + notify.requestID + " notify res " + notify.result);
                    }
                    break;
                    default:
                        break;
                }
            }
        });
    }

    private Object mSurface;
    @Override
    public synchronized void setSurface(Object surface) {
        mSurface = surface;
        super.setSurface(surface);
    }

    public synchronized Object getSurface() {
        return mSurface;
    }

    private static class OneCameraCallback implements OneCallbacks {
        @Override
        public void onOpenComplete() {
            EventBus.getDefault().post(new OpenEvent());
        }

        @Override
        public void onError(int err, int data, String description) {
            EventBus.getDefault().post(new ErrorEvent(err, data, description));
        }

        @Override
        public void onDetached() {
            EventBus.getDefault().post(new DetachEvent());
        }

        @Override
        public void onRecordComplete(RecordType recordType, final String recordPath) {
            EventBus.getDefault().post(new RecordCompleteEvent(recordType));
        }

        @Override
        public void onRecordError(final int err, RecordType recordType, String path) {
            EventBus.getDefault().post(new RecordErrorEvent(err));
        }

        @Override
        public void onPhotoCaptured(int err, String path) {
            EventBus.getDefault().post(new PhotoCapturedEvent(err, path));
        }

        @Override
        public void onStillImageCaptured(final int err, final String path)
        {
            EventBus.getDefault().post(new StillImageCapturedEvent(err,path));
        }


        @Override
        public void onRecordVideoStateNotify(int state, VideoResult mResult)
        {
            EventBus.getDefault().post(new RecordCompleteWithStorageEvent(state,mResult));
        }

        @Override
        public void onUsbState(int state, int err)
        {

        }

        @Override
        public void onStillImageWithStorageNotify(TakePictureResponse mResponse)
        {

        }

        @Override
        public void onTimelapseNotify(int state, VideoResult mResult)
        {

        }

        @Override
        public void onUsbSpeedTest(String bitPerS)
        {
            EventBus.getDefault().post(new UsbTestSpeedEvent(bitPerS));
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    public static class OpenEvent {

    }

    public static class DetachEvent {
    }

    public static class ErrorEvent {
        public ErrorEvent(int error, int data, String description) {
            this.error = error;
            this.data = data;
            this.description = description;
        }
        public int error;
        public int data;
        public String description;
    }

    public static class RecordCompleteEvent {
        public RecordCompleteEvent(RecordType recordType) {
            this.recordType = recordType;
            this.error = error;
        }
        public RecordType recordType;
        public int error;
    }

    public static class RecordCompleteWithStorageEvent {
        public RecordCompleteWithStorageEvent(int state,VideoResult mResult) {
            this.mResult = mResult;
            this.state = state;
        }
        public VideoResult mResult;
        public int state;
    }

    public static class RecordErrorEvent {
        public RecordErrorEvent(int err) {
            this.error = err;
        }
        public int error;
    }

    public static class PhotoCapturedEvent {
        public PhotoCapturedEvent(int error, String path) {
            this.error = error;
            this.path = path;
        }
        public int error;
        public String path;
    }

    public static class StillImageCapturedEvent {
        public StillImageCapturedEvent(int error, String path) {
            this.error = error;
            this.path = path;
        }
        public int error;
        public String path;
    }

    public static class RecordFpsUpdateEvent {
        public RecordFpsUpdateEvent(int fps) {
            this.fps = fps;
        }
        public int fps;
    }

    public static class LivePushStartedEvent {
        public LivePushStartedEvent(String url) {
            this.url = url;
        }
        public String url;
    }

    public static class UsbTestSpeedEvent {
        public UsbTestSpeedEvent(String speed) {
            this.speed = speed;
        }
        public String speed;
    }

    public static class BleDisconnectEvent {
        BTPeripheral mBTPeripheral;
        public BleDisconnectEvent(BTPeripheral mBTPeripheral) {
            this.mBTPeripheral = mBTPeripheral;
        }
    }

    public static class BleDiscoverEvent {
        BTPeripheral mBTPeripheral;
        public BleDiscoverEvent(BTPeripheral mBTPeripheral) {
            this.mBTPeripheral = mBTPeripheral;
        }
    }

    public static class BleConnectEvent {
        BTPeripheral mBTPeripheral;
        public BleConnectEvent(BTPeripheral mBTPeripheral) {
            this.mBTPeripheral = mBTPeripheral;
        }
    }

    public static class UsbFileList{
        public List<String> list;
        public UsbFileList(List<String> list){
            this.list=list;
        }
    }
}
