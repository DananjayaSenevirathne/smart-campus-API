package com.smartcampus.api.config;

import com.smartcampus.api.exception.*;
import com.smartcampus.api.filter.LoggingFilter;
import com.smartcampus.api.resource.*;
import org.glassfish.jersey.server.ResourceConfig;

public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig() {
        // Register resources
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);
        
        // Register filters
        register(LoggingFilter.class);
        
        // Register exception mappers
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);
        
        // Ensure package scanning for any other components
        packages("com.smartcampus.api");
    }
}