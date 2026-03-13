package umg.primer.parcial.p3.TrasaccionesProducer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

// Importamos las clases de tu paquete
import umg.primer.parcia.clases.LoteTransacciones;
import umg.primer.parcia.clases.Transaccion;

public class App {
    public static void main(String[] args) {
        
        // URL del GET [cite: 3-5]
        String url = "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones";
        
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); 

        try (Connection connection = factory.newConnection(); 
             Channel channel = connection.createChannel()) {
            
            System.out.println("Iniciando Producer... Filtrando a exactamente 25 transacciones por banco.");

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                System.out.println("Consultando API...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    LoteTransacciones lote = mapper.readValue(response.body(), LoteTransacciones.class);
                    
                    // Mapa para llevar la cuenta de cuántas van por banco
                    Map<String, Integer> contadorBancos = new HashMap<>();
                    int totalEnviadas = 0;

                    // Recorremos el lote que nos dio la API [cite: 262]
                    for (Transaccion tx : lote.getTransacciones()) {
                        
                        String nombreCola = tx.getBancoDestino(); // [cite: 263]
                        
                        // Si es la primera vez que vemos este banco, iniciamos su contador en 0
                        contadorBancos.putIfAbsent(nombreCola, 0);

                        // Verificamos si este banco aún no ha llegado a su límite de 25
                        if (contadorBancos.get(nombreCola) < 25) {
                            
                            // Crear la cola dinámicamente si no existe [cite: 266]
                            channel.queueDeclare(nombreCola, true, false, false, null);

                            // Convertir y enviar [cite: 267]
                            String jsonMensaje = mapper.writeValueAsString(tx);
                            channel.basicPublish("", nombreCola, null, jsonMensaje.getBytes(StandardCharsets.UTF_8));
                            
                            // Le sumamos 1 al contador de este banco
                            int nuevaCantidad = contadorBancos.get(nombreCola) + 1;
                            contadorBancos.put(nombreCola, nuevaCantidad);
                            totalEnviadas++;
                            
                            System.out.println(" [" + nuevaCantidad + "/25] TX-" + tx.getIdTransaccion() + " enviada a: " + nombreCola);
                        }

                        // Si ya logramos enviar 100 transacciones en total, detenemos el ciclo
                        if (totalEnviadas == 100) {
                            break; 
                        }
                    }
                    
                    System.out.println("\n¡Proceso terminado exitosamente!");
                    System.out.println("Se enviaron " + totalEnviadas + " transacciones en total a RabbitMQ.");
                    
                } else {
                    System.err.println("Error en API: Status " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("Error procesando los datos: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error de conexión con RabbitMQ: " + e.getMessage());
        }
    }
}