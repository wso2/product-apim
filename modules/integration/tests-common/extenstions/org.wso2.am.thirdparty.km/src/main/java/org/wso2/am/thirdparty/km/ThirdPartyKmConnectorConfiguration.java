package org.wso2.am.thirdparty.km;

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.model.ConfigurationDto;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;
import org.wso2.carbon.apimgt.impl.keymgt.AbstractKeyManagerConnectorConfiguration;

import java.util.ArrayList;
import java.util.List;


@Component(
        name ="thirdpary.km.component",
        immediate = true,
        service = KeyManagerConnectorConfiguration.class,
        property = {"type=" + ThirdPartyKMConstants.KEY_MANAGER_TYPE})
public class ThirdPartyKmConnectorConfiguration extends AbstractKeyManagerConnectorConfiguration {

    @Override
    public String getImplementation() {

        return ThirdPartyKmConnector.class.getName();
    }

    @Override
    public String getJWTValidator() {

        return null;
    }
}
