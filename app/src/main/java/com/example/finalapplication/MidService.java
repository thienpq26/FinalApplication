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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dcv.finaltest.configuration.IConfigurationService;
import dcv.finaltest.hmiapplication.IHMIListener;
import dcv.finaltest.hmiapplication.IServiceInterface;
import dcv.finaltest.property.IPropertyEventListener;
import dcv.finaltest.property.IPropertyService;

public class MidService extends Service implements IServiceInterface {

    private IPropertyService mPropertyService;
    private IConfigurationService mConfigurationService;
    public static String TAG = "MidService";
    public static String PACKAGE = "com.example.finalapplication";
    private HandlerThread mHandlerThread;
    private ServiceHandler mHandler;
    private PropertyRemoteObject object = new PropertyRemoteObject();
    private IHMIListener ihmiListener;
    private double[] mList15 = new double[15];
    private List<Double> mList60 = new ArrayList<>();
    private int index = 0;
    double sumConsumption = 0.0;
    private IBinder iBinder = new LocalBinder();

    //Tam thoi cmt su dung listener cua registerListener
//    public MidService(final IHMIListener ihmiListener) {
//        this.ihmiListener = ihmiListener;
//    }

    private ServiceConnection mPropertyServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mPropertyService = IPropertyService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPropertyService = null;
        }
    };

    private ServiceConnection mConfigurationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mConfigurationService = IConfigurationService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConfigurationService = null;
        }
    };

    public class LocalBinder extends Binder {
        MidService getService() {
            return MidService.this;
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
                    break;
                }
                case IPropertyService.PROP_DISTANCE_VALUE: {
                    msg.what = IPropertyService.PROP_DISTANCE_VALUE;
                    msg.obj = event;
                    break;
                }
                case IPropertyService.PROP_CONSUMPTION_UNIT: {
                    msg.what = IPropertyService.PROP_CONSUMPTION_UNIT;
                    msg.obj = event;
                    break;
                }
                case IPropertyService.PROP_CONSUMPTION_VALUE: {
                    msg.what = IPropertyService.PROP_CONSUMPTION_VALUE;
                    msg.obj = event;
                    break;
                }
                case IPropertyService.PROP_RESET: {
                    msg.what = IPropertyService.PROP_RESET;
                    msg.obj = event;
                    break;
                }
            }
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onCreate() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());
        bindPropertyService();
        bindConfigurationService();
    }

    private void bindConfigurationService() {
        Intent intent = new Intent();
        intent.setPackage(PACKAGE);
        intent.putExtra("service", "property");
        bindService(intent, mPropertyServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void bindPropertyService() {
        Intent intent = new Intent();
        intent.setPackage(PACKAGE);
        intent.putExtra("service", "config");
        bindService(intent, mConfigurationServiceConnection, Context.BIND_AUTO_CREATE);
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
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_DISTANCE_VALUE: {
                        try {
                            ihmiListener.onDistanceChanged((Double) event.getValue());
                            break;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_CONSUMPTION_UNIT: {
                        try {
                            ihmiListener.OnConsumptionUnitChanged((Integer) event.getValue());
                            break;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    case IPropertyService.PROP_CONSUMPTION_VALUE: {
                        if (mList60.size() <= 120) {
                            mList60.add((Double) event.getValue());
                            sumConsumption += (Double) event.getValue();
                        } else {
                            if (index >= 14) {
                                for (int i = 1; i < 15; i++) {
                                    mList15[i - 1] = mList15[i];
                                }
                                mList15[14] = sumConsumption;
                                sumConsumption = 0;
                                index = 0;
                                try {
                                    ihmiListener.onConsumptionChanged(mList15);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mList15[index] = sumConsumption;
                                index++;
                            }
                            mList60.clear();
                        }
                    }
                    case IPropertyService.PROP_RESET: {
                        try {
                            ihmiListener.onError((Boolean) event.getValue());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                try {
                    ihmiListener.onError(true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void registerListener(IHMIListener listener) throws RemoteException {
        ihmiListener = listener; // Chua chac
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
        return new TestCapability(mConfigurationService.isSupport(IConfigurationService.CONFIG_DISTANCE), mConfigurationService.isSupport(IConfigurationService.CONFIG_DISTANCE), mConfigurationService.isSupport(IConfigurationService.CONFIG_DISTANCE));
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
        for (int i = 0; i < 15; i++) {
            mList15[i] = 0;
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mPropertyServiceConnection);
        unbindService(mConfigurationServiceConnection);
    }
}
