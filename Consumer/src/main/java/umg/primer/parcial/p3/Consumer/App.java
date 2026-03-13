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

    // URL del POST para guardar las transacciones [cite: 87-88]
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
            
            // Nos aseguramos de que la cola exista antes de escucharla
            channel.queueDeclare(banco, true, false, false, null);

            // Definimos qué hacer cuando llega un mensaje de RabbitMQ [cite: 189]
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String mensajeJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag(); // ID del mensaje

                try {
                    // 1. Deserializar JSON a Objeto Java
                    Transaccion tx = mapper.readValue(mensajeJson, Transaccion.class);
                    System.out.println("\n [x] Procesando TX-" + tx.getIdTransaccion() + " del banco " + banco);

                    // --- 2. INYECTAR DATOS DEL ESTUDIANTE ---
                    tx.setNombre("Cristian Josué Flores Pleitez"); 
                    tx.setCarnet("0905-24-4847");         
                    tx.setCorreo("cfloresp5@miumg.edu.gt");
                    
                    // Convertimos el objeto modificado de vuelta a texto JSON
                    String jsonModificado = mapper.writeValueAsString(tx);
                    // ----------------------------------------

                    // 3. Enviar por POST a la API usando el JSON modificado
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(POST_API_URL))
                            .header("Content-Type", "application/json") 
                            .POST(HttpRequest.BodyPublishers.ofString(jsonModificado)) // Usamos jsonModificado aquí
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


                    // 3. Validar respuesta HTTP y hacer el ACK manual [cite: 192-195]
                    if (response.statusCode() == 200 || response.statusCode() == 201) {                        System.out.println(" [V] Guardada en BD exitosamente.");
                        channel.basicAck(deliveryTag, false); // Confirma que ya se puede borrar de la cola [cite: 194]
                    } else {
                        System.err.println(" [X] Error de API al guardar. Status: " + response.statusCode());
                        channel.basicNack(deliveryTag, false, true); // Devuelve el mensaje a la cola para reintento [cite: 195]
                    }

                } catch (Exception e) {
                    System.err.println(" [!] Error interno procesando el mensaje. Reencolando...");
                    channel.basicNack(deliveryTag, false, true); // Devuelve el mensaje a la cola si hay error de código
                }
            };

            // Iniciar el consumo de la cola (autoAck en false para controlarlo nosotros) [cite: 286]
            channel.basicConsume(banco, false, deliverCallback, consumerTag -> { });
        }
    }
}