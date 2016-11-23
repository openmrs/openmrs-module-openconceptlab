package org.openmrs.module.openconceptlab;

import org.openmrs.OpenmrsCacheConfiguration;
import org.openmrs.api.OpenmrsCacheConfigurer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenconceptlabCacheConfiguration implements OpenmrsCacheConfigurer{

    @Override
    public List<OpenmrsCacheConfiguration> getCacheConfiguration() {
        List<OpenmrsCacheConfiguration> openmrsCacheConfigurationList = new ArrayList<OpenmrsCacheConfiguration>();

        OpenmrsCacheConfiguration openmrsCacheConfiguration = new OpenmrsCacheConfiguration();
        openmrsCacheConfiguration.addProperty("name", "conceptDataType");
        openmrsCacheConfiguration.addProperty("maxElementsInMemory", "500");
        openmrsCacheConfiguration.addProperty("eternal", "false");
        openmrsCacheConfiguration.addProperty("timeToIdleSeconds", "300");
        openmrsCacheConfiguration.addProperty("timeToLiveSeconds", "300");
        openmrsCacheConfiguration.addProperty("memoryStoreEvictionPolicy", "LRU");

        openmrsCacheConfigurationList.add(openmrsCacheConfiguration);

        return openmrsCacheConfigurationList;
    }
}
