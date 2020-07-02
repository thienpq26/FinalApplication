// IPropertyEventListener.aidl
package dcv.finaltest.property;
import dcv.finaltest.property.PropertyEvent;
// Declare any non-default types here with import statements

oneway interface IPropertyEventListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onEvent(in PropertyEvent event) = 0;
}
