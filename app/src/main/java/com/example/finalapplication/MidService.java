package com.example.finalapplication;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dcv.finaltest.configuration.IConfigurationService;
import dcv.finaltest.core.ICoreService;
import dcv.finaltest.hmiapplication.IHMIListener;
import dcv.finaltest.hmiapplication.IServiceInterface;
import dcv.finaltest.property.IPropertyEventListener;
import dcv.finaltest.property.IPropertyService;

public class MidService extends Service {

    private ICoreService mCoreService;
    public static String TAG = "MidService";
    public static int LIST60S_SIZE = 120;
    public static int LIST_HISTORY_SIZE = 15;
    private HandlerThread mHandlerThread;
    private ServiceHandler mHandler;
    private PropertyRemoteObject object = new PropertyRemoteObject();
    private IHMIListener ihmiListener;
    private double[] mList15 = new double[LIST_HISTORY_SIZE];
    private List<Double> mList60 = new ArrayList<>();
    double sumConsumption = 0.0;
    private IPropertyService mPropertyService;
    private IConfigurationService mConfigurationService;

    private ServiceConnection mCoreServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCoreService = ICoreService.Stub.asInterface(service);
            try {
                mPropertyService = mCoreService.getPropertyService();
                mConfigurationService = mCoreService.getConfigurationService();
            } catch (RemoteException e) {
                Log.e(TAG, "Connect to CoreService fail");
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCoreService = null;
        }
    };

    private IServiceInterface.Stub iBinderServiceInterface = new IServiceInterface.Stub() {

        @Override
        public void registerListener(IHMIListener listener) throws RemoteException {
            ihmiListener = listener;
            mPropertyService.registerListener(IPropertyService.PROP_DISTANCE_UNIT, object);
            mPropertyService.registerListener(IPropertyService.PROP_DISTANCE_VALUE, object);
            mPropertyService.registerListener(IPropertyService.PROP_CONSUMPTION_UNIT, object);
            mPropertyService.registerListener(IPropertyService.PROP_CONSUMPTION_VALUE, object);
            mPropertyService.registerListener(IPropertyService.PROP_RESET, object);
        }

        @Override
        public void unregisterListener(IHMIListener listener) throws RemoteException {
            mPropertyService.unregisterListener(IPropertyService.PROP_DISTANCE_UNIT, object);
            mPropertyService.unregisterListener(IPropertyService.PROP_DISTANCE_VALUE, object);
            mPropertyService.unregisterListener(IPropertyService.PROP_CONSUMPTION_UNIT, object);
            mPropertyService.unregisterListener(IPropertyService.PROP_CONSUMPTION_VALUE, object);
            mPropertyService.unregisterListener(IPropertyService.PROP_RESET, object);
        }

        @Override
        public TestCapability getCapability() throws RemoteException {
            return new TestCapability(mConfigurationService.isSupport(IConfigurationService.CONFIG_DISTANCE), mConfigurationService.isSupport(IConfigurationService.CONFIG_CONSUMPTION), mConfigurationService.isSupport(IConfigurationService.CONFIG_RESET));
        }

        @Override
        public void setDistanceUnit(int unit) throws RemoteException {
            PropertyEvent event = new PropertyEvent(IPropertyService.PROP_DISTANCE_UNIT, PropertyEvent.STATUS_AVAILABLE, 0, unit);
            mPropertyService.setProperty(IPropertyService.PROP_DISTANCE_UNIT, event);
        }

        @Override
        public void setConsumptionUnit(int unit) throws RemoteException {
            PropertyEvent event = new PropertyEvent(IPropertyService.PROP_CONSUMPTION_UNIT, PropertyEvent.STATUS_AVAILABLE, 0, unit);
            mPropertyService.setProperty(IPropertyService.PROP_CONSUMPTION_UNIT, event);
        }

        @Override
        public void resetData() throws RemoteException {
            for (int i = 0; i < LIST_HISTORY_SIZE; i++) {
                mList15[i] = 0;
            }
            ihmiListener.onConsumptionChanged(mList15);
        }
    };

    @Override
    public void onCreate() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());
        bindCoreService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinderServiceInterface;
    }

    private void bindCoreService() {
        Intent intent = new Intent();
        intent.setAction("dcv.finaltest.BIND");
        intent.setPackage("dcv.finaltest.hmiapplication");
        if (!bindService(intent, mCoreServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "bindService connect to CoreService fail.");
        }
    }

    private class ServiceHandler extends Handler {

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            PropertyEvent event = (PropertyEvent) msg.obj;
            if (event.getStatus() == PropertyEvent.STATUS_AVAILABLE) {
                switch (msg.what) {
                    case IPropertyService.PROP_DISTANCE_UNIT: {
                        try {
                            ihmiListener.onDistanceUnitChanged((Integer) event.getValue());
                            break;
                        } catch (RemoteException e) {
                            Log.e(TAG, "OnDistanceUnitChanged to failed: ");
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_DISTANCE_VALUE: {
                        try {
                            ihmiListener.onDistanceChanged((Double) event.getValue());
                            break;
                        } catch (RemoteException e) {
                            Log.e(TAG, "OnDistanceChanged to failed: ");
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_CONSUMPTION_UNIT: {
                        try {
                            ihmiListener.OnConsumptionUnitChanged((Integer) event.getValue());
                            break;
                        } catch (RemoteException e) {
                            Log.e(TAG, "OnConsumptionUnitChanged to failed: ");
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_CONSUMPTION_VALUE: {
                        if (mList60.size() < LIST60S_SIZE) {
                            mList60.add((Double) event.getValue());
                            sumConsumption += (Double) event.getValue();
                            Log.d("D/size", mList60.size() + "");
                        } else {
                            StringBuilder builder = new StringBuilder();
                            for (int i = 1; i < 15; i++) {
                                mList15[i - 1] = mList15[i];
                                builder.append(mList15[i - 1] + ", ");
                            }
                            mList15[14] = sumConsumption;
                            builder.append(mList15[14] + "");
                            Log.d("D/mList", builder.toString());
                            try {
                                ihmiListener.onConsumptionChanged(mList15);
                            } catch (RemoteException e) {
                                Log.e(TAG, "OnConsumptionChanged to failed: ");
                                e.printStackTrace();
                            }
                            sumConsumption = 0;
                            mList60.clear();
                        }
                    }
                    case IPropertyService.PROP_RESET: {
                        if (event.getPropertyId() == IPropertyService.PROP_RESET) {
                            try {
                                ihmiListener.onError((Boolean) event.getValue());
                            } catch (RemoteException e) {
                                Log.e(TAG, "OnError to failed: ");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                try {
                    ihmiListener.onError(true);
                } catch (RemoteException e) {
                    Log.e(TAG, "OnError to failed: ");
                    e.printStackTrace();
                }
            }
        }
    }

    private class PropertyRemoteObject extends IPropertyEventListener.Stub {
        @Override
        public void onEvent(PropertyEvent event) throws RemoteException {
            Message msg = new Message();
            switch (event.getPropertyId()) {
                case IPropertyService.PROP_DISTANCE_UNIT: {
                    msg.what = IPropertyService.PROP_DISTANCE_UNIT;
                    msg.obj = event;
                    Log.d("D/e", "DISTANCE_UNIT " + event.getValue() + "");
                    break;
                }
                case IPropertyService.PROP_DISTANCE_VALUE: {
                    msg.what = IPropertyService.PROP_DISTANCE_VALUE;
                    msg.obj = event;
                    Log.d("D/e", "DISTANCE_VALUE " + event.getValue() + "");
                    break;
                }
                case IPropertyService.PROP_CONSUMPTION_UNIT: {
                    msg.what = IPropertyService.PROP_CONSUMPTION_UNIT;
                    msg.obj = event;
                    Log.d("D/e", "CONSUMPTION_UNIT " + event.getValue() + "");
                    break;
                }
                case IPropertyService.PROP_CONSUMPTION_VALUE: {
                    msg.what = IPropertyService.PROP_CONSUMPTION_VALUE;
                    msg.obj = event;
                    Log.d("D/e", "CONSUMPTION_VALUE " + event.getValue() + "");
                    break;
                }
                case IPropertyService.PROP_RESET: {
                    msg.what = IPropertyService.PROP_RESET;
                    msg.obj = event;
                    Log.d("D/e", "RESET " + event.getValue() + "");
                    break;
                }
            }
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mCoreServiceConnection);
    }
}
