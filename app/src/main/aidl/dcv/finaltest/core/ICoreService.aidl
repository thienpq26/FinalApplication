// ICoreService.aidl
package dcv.finaltest.core;

import dcv.finaltest.configuration.IConfigurationService;
import dcv.finaltest.property.IPropertyService;

// Declare any non-default types here with import statements

interface ICoreService {
    IPropertyService getPropertyService();
    IConfigurationService getConfigurationService();
}
