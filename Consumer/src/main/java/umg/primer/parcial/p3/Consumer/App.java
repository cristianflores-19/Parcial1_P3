package umg.primer.parcial.p3.Consumer;

import umg.primer.parcial.clases.Transaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class App {

    private static final String POST_API_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";

    public static void main(String[] args) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newHttpClient();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String[] bancosDestino = {"BANRURAL", "GYT", "BAC", "BI"}; 

        System.out.println(" [*] Consumer iniciado. Esperando transacciones...");

        for (String banco : bancosDestino) {
            
            channel.queueDeclare(banco, true, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String mensajeJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag(); 

                try {
                    Transaccion tx = mapper.readValue(mensajeJson, Transaccion.class);
                    
                    // ====================================================================
                    //                 NUEVO SERIE II - INICIO DE MODIFICACIÓN
                    // ====================================================================
                    
                    // 1. Evidenciar en consola la cola atendida y el ID procesado
                    System.out.println("\nAtendiendo cola: " + banco + " | ID procesado: " + tx.getIdTransaccion());

                    // 2. Condición principal: Si el monto > Q4000.00 se rechaza
                    if (tx.getMonto() > 4000.00) {
                        
                        // Enviar a la nueva cola de rechazados
                        channel.queueDeclare("cola_rechazados", true, false, false, null);
                        channel.basicPublish("", "cola_rechazados", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                        
                        // Registro en consola (Formato exigido en el examen para rechazadas)
                        System.out.println("idTransaccion: " + tx.getIdTransaccion());
                        System.out.println("monto: Q." + tx.getMonto());
                        System.out.println("estado: RECHAZADA");

                        // Confirmamos a RabbitMQ que ya la movimos (no se pierde, solo cambia de cola)
                        channel.basicAck(deliveryTag, false); 
                        
                    } else {
                    // ====================================================================
                    //                 NUEVO SERIE II - FIN DE MODIFICACIÓN
                    // ====================================================================

                        // (FLUJO ORIGINAL: Si es <= Q4000.00, se mantienen tus datos inyectados y va al POST)
                        tx.setNombre("Christian Josue Flores Pleitez"); 
                        tx.setCarnet("0905-24-4847"); 
                        tx.setCorreo("cfloresp10@miumg.edu.gt"); 
                        
                        String jsonModificado = mapper.writeValueAsString(tx);

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(POST_API_URL))
                                .header("Content-Type", "application/json") 
                                .POST(HttpRequest.BodyPublishers.ofString(jsonModificado))
                                .build();

                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            
                            // ====================================================================
                            // NUEVO SERIE II: Registro en consola (Formato exigido para aceptadas)
                            // ====================================================================
                            System.out.println("idTransaccion: " + tx.getIdTransaccion());
                            System.out.println("monto: Q." + tx.getMonto());
                            System.out.println("estado: ACEPTADA");
                            // ====================================================================
                            
                            channel.basicAck(deliveryTag, false); 
                        } else {
                            channel.basicNack(deliveryTag, false, true); 
                        }
                    }

                } catch (Exception e) {
                    channel.basicNack(deliveryTag, false, true); 
                }
            };

            channel.basicConsume(banco, false, deliverCallback, consumerTag -> { });
        }
    }
}