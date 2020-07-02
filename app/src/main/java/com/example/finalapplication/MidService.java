package com.example.finalapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import dcv.finaltest.hmiapplication.IHMIListener;
import dcv.finaltest.property.IPropertyEventListener;
import dcv.finaltest.property.IPropertyService;

public class MidService extends Service implements IPropertyEventListener {

    public static String TAG = "MidService";
    private IHMIListener ihmiListener;
    private MidServiceHandler midServiceHandler;
    private HandlerThread handlerThread;

    @Override
    public void onCreate() {
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        midServiceHandler = new MidServiceHandler(handlerThread.getLooper());
    }

    public MidService(IHMIListener ihmiListener) {
        this.ihmiListener = ihmiListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class MidServiceHandler extends Handler {
        MidServiceHandler(Looper looper) {
            super(looper);
        }

        void onDistanceUnitChanged(int distanceUnit) {
            Message message = new Message();
            message.what = IPropertyService.PROP_DISTANCE_UNIT;
            message.obj = distanceUnit;
            sendMessage(message);
        }

        void onDistanceChanged(double distance) {
            Message message = new Message();
            message.what = IPropertyService.PROP_DISTANCE_VALUE;
            message.obj = distance;
            sendMessage(message);
        }

        void onConsumptionUnitChanged(int consumptionUnit) {
            Message message = new Message();
            message.what = IPropertyService.PROP_CONSUMPTION_UNIT;
            message.obj = consumptionUnit;
            sendMessage(message);
        }

        void onConsumptionChanged(double[] consumptionList) {
            Message message = new Message();
            message.what = IPropertyService.PROP_CONSUMPTION_VALUE;
            message.obj = consumptionList;
            sendMessage(message);
        }

        void onError(boolean isError) {
            Message message = new Message();
            message.what = IPropertyService.PROP_RESET;
            message.obj = isError;
            sendMessage(message);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IPropertyService.PROP_DISTANCE_UNIT: {
                    try {
                        ihmiListener.onDistanceUnitChanged((Integer) msg.obj);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                case IPropertyService.PROP_DISTANCE_VALUE: {
                    try {
                        ihmiListener.onDistanceChanged((Double) msg.obj);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                case IPropertyService.PROP_CONSUMPTION_UNIT: {
                    try {
                        ihmiListener.OnConsumptionUnitChanged((Integer) msg.obj);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                case IPropertyService.PROP_CONSUMPTION_VALUE: {
                    try {
                        ihmiListener.onConsumptionChanged((double[]) msg.obj);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                case IPropertyService.PROP_RESET: {
                    try {
                        ihmiListener.onError(true);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(PropertyEvent event) throws RemoteException {
        switch (event.getPropertyId()) {
            case IPropertyService.PROP_DISTANCE_UNIT: {
                midServiceHandler.onDistanceUnitChanged((Integer) event.getValue());
                break;
            }
            case IPropertyService.PROP_DISTANCE_VALUE: {
                midServiceHandler.onDistanceChanged((Double) event.getValue());
                break;
            }
            case IPropertyService.PROP_CONSUMPTION_UNIT: {
                midServiceHandler.onConsumptionUnitChanged((Integer) event.getValue());
                break;
            }
            case IPropertyService.PROP_CONSUMPTION_VALUE: {
                midServiceHandler.onConsumptionChanged((double[]) event.getValue());
                break;
            }
            case IPropertyService.PROP_RESET: {
                midServiceHandler.onError(true);
                break;
            }
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
