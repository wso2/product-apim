package org.wso2.am.thirdparty.km;

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;
import org.wso2.carbon.apimgt.impl.keymgt.AbstractKeyManagerConnectorConfiguration;

@Component(
        name = "thirdpary.km.component",
        immediate = true,
        service = KeyManagerConnectorConfiguration.class)
public class ThirdPartyKmConnectorConfiguration extends AbstractKeyManagerConnectorConfiguration {

    @Override
    public String getImplementation() {

        return ThirdPartyKmConnector.class.getName();
    }

    @Override
    public String getJWTValidator() {

        return null;
    }

    @Override
    public String getType() {

        return ThirdPartyKMConstants.KEY_MANAGER_TYPE;
    }
}
