package com.msgpipeline.orchestrator.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase de arranque de Spring Boot para el Orchestrator v3.
 *
 * @SpringBootApplication escanea el paquete base com.msgpipeline.orchestrator
 * y registra todos los @Component, @Service, @Repository y @Configuration.
 *
 * En Lambda NO arranca un servidor web; el contexto se crea desde el handler.
 */
@SpringBootApplication(scanBasePackages = "com.msgpipeline.orchestrator")
public class OrchestratorApplication {
    // Sin metodo main: en Lambda el contexto se inicia desde OrchestratorHandler.
    // El metodo main solo es util para ejecucion local con bootRun (opcional).
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(OrchestratorApplication.class, args);
    }
}
