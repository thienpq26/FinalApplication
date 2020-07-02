// IPropertyService.aidl
package dcv.finaltest.property;

import dcv.finaltest.property.IPropertyEventListener;
import dcv.finaltest.property.PropertyEvent;

interface IPropertyService {

    const int PROP_DISTANCE_UNIT = 1;
    const int PROP_DISTANCE_VALUE = 2;
    const int PROP_CONSUMPTION_UNIT = 3;
    const int PROP_CONSUMPTION_VALUE = 4;
    const int PROP_RESET = 5;

    void registerListener(int propID, in IPropertyEventListener callback);
    void unregisterListener(int propID, in IPropertyEventListener callback);
    void setProperty(int prodID, in PropertyEvent value);
}
