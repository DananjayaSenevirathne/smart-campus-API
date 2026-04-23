package com.smartcampus.api.config;

import com.smartcampus.api.exception.GlobalExceptionMapper;
import com.smartcampus.api.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.api.filter.LoggingFilter;
import com.smartcampus.api.resource.RoomResource;
import com.smartcampus.api.resource.SensorResource;
import org.glassfish.jersey.server.ResourceConfig;

public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig() {
        packages("com.smartcampus.api");
        register(LoggingFilter.class);
        register(RoomResource.class);
        register(SensorResource.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(GlobalExceptionMapper.class);
    }
}