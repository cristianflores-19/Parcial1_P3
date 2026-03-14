package umg.primer.parcial.p3.Consumer;

import umg.primer.parcial.clases.Transaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final String POST_API_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";
    private static List<String> transaccionesProcesadas = new ArrayList<>();

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
                    System.out.println("\nAtendiendo cola: " + banco + " | ID procesado: " + tx.getIdTransaccion());

                    if (transaccionesProcesadas.contains(tx.getIdTransaccion())) {
                        
                        channel.queueDeclare("cola_duplicados", true, false, false, null);
                        channel.basicPublish("", "cola_duplicados", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                        
                        System.out.println("idTransaccion: " + tx.getIdTransaccion());
                        System.out.println("estado: Duplicada");
                        System.out.println("cola destino: cola_duplicados");

                        channel.basicAck(deliveryTag, false); 
                        
                    } else {
                        tx.setNombre("Cristian Josué Flores Pleitez"); 
                        tx.setCarnet("0905-24-4847"); 
                        tx.setCorreo("cfloresp5@miumg.edu.gt"); 
                        
                        String jsonModificado = mapper.writeValueAsString(tx);
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(POST_API_URL))
                                .header("Content-Type", "application/json") 
                                .POST(HttpRequest.BodyPublishers.ofString(jsonModificado))
                                .build();

                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            
                            transaccionesProcesadas.add(tx.getIdTransaccion());

                            System.out.println("idTransaccion: " + tx.getIdTransaccion());
                            System.out.println("estado: Procesado");
                            System.out.println("cola destino: POST");
                            
                            channel.basicAck(deliveryTag, false); 
                        } else {
                            channel.queueDeclare("cola_errores", true, false, false, null);
                            channel.basicPublish("", "cola_errores", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                            channel.basicAck(deliveryTag, false);
                        }
                    }

                } catch (Exception e) {
                    try {
                        channel.queueDeclare("cola_errores", true, false, false, null);
                        channel.basicPublish("", "cola_errores", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                        channel.basicAck(deliveryTag, false);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };

            channel.basicConsume(banco, false, deliverCallback, consumerTag -> { });
        }
    }
}