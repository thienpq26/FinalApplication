
package dcv.finaltest.hmiapplication;

import dcv.finaltest.hmiapplication.IHMIListener;
import dcv.finaltest.hmiapplication.TestCapability;
// Declare any non-default types here with import statements

interface IServiceInterface {
    void registerListener(in IHMIListener listener);
    void unregisterListener(in IHMIListener listener);
    TestCapability getCapability();
    void setDistanceUnit(int unit);
    void setConsumptionUnit(int unit);
    void resetData();
}
