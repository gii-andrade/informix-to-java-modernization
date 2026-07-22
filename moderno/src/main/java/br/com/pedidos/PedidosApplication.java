package br.com.pedidos;

import br.com.pedidos.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Ponto de entrada da aplicação Sistema de Pedidos.
 *
 * Migração do legado Informix-4GL para Spring Boot 3 / Java 21.
 *
 * O pacote raiz {@code br.com.pedidos} garante que o @SpringBootApplication
 * escaneie automaticamente todos os módulos:
 *   br.com.pedidos.cliente.*
 *   br.com.pedidos.produto.*
 *   br.com.pedidos.pedido.*
 *   br.com.pedidos.usuario.*
 *   br.com.pedidos.relatorio.*
 *   br.com.pedidos.security.*
 *
 * Para iniciar:
 *   mvn spring-boot:run
 * ou
 *   mvn package && java -jar target/pedidos-service-1.0.0.jar
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class PedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosApplication.class, args);
    }
}
