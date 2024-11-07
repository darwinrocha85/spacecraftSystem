package com.rocha.spacecraftmanagementsystem.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Intercepta el método getSpacecraftById en el controlador cuando el ID es negativo
    @Before("execution(* com.rocha.spacecraftmanagementsystem.controller.SpacecraftController.getSpacecraftById(..)) && args(id)")
    public void logIfNegativeId(JoinPoint joinPoint, Long id) {
        if (id < 0) {
            logger.warn("Se solicitó una nave espacial con un ID negativo: {}", id);
        }
    }
}
