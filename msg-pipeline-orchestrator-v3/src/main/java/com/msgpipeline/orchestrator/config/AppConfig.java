package com.msgpipeline.orchestrator.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la aplicacion.
 *
 * Los beans de adaptadores se registran por @Component/@Repository con @Profile,
 * por lo que aqui no es necesario declararlos manualmente. Esta clase queda
 * como punto de extension para beans adicionales (por ejemplo, un ObjectMapper
 * personalizado) si fueran necesarios.
 */
@Configuration
public class AppConfig {
}
