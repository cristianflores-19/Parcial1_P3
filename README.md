# Proyecto: Procesamiento de Transacciones Bancarias con RabbitMQ

**Estudiante:** Cristian Josué Flores Pleitez  
**Carnet:** 0905-24-4847  
**Curso:** Programación 3 (Quinto Semestre)  
**Universidad:** Universidad Mariano Gálvez de Guatemala (UMG)  

##  Descripción del Proyecto
Este proyecto implementa un sistema distribuido en Java utilizando el patrón de diseño **Producer-Consumer** apoyado en el broker de mensajería **RabbitMQ**. El objetivo principal es procesar lotes de transacciones bancarias, enrutándolas y distribuyéndolas de forma asíncrona hacia colas específicas dependiendo del banco destino de cada transacción. 

El sistema garantiza la no pérdida de transacciones y un manejo adecuado de errores mediante el uso de confirmaciones manuales (ACK/NACK).

---

##  Arquitectura de la Solución

El sistema está desacoplado en dos componentes principales que operan de manera independiente:

### 1. Componente A: Producer (Productor)
Este componente actúa como el recolector y clasificador de datos.
* **Consumo de API:** Utiliza `HttpClient` nativo de Java para realizar una petición HTTP GET al endpoint proporcionado, obteniendo un lote de transacciones.
* **Deserialización (JSON a POJO):** Utiliza la librería Jackson para mapear la respuesta JSON a un modelo de objetos fuertemente tipado en Java (`LoteTransacciones`, `Transaccion`, `Detalle`, `Referencias`).
* **Enrutamiento Dinámico:** Itera sobre la lista de transacciones, extrae el campo `bancoDestino` y declara de forma dinámica una cola en RabbitMQ con ese nombre exacto (ej. `BANRURAL`, `BAC`, `BI`).
* **Publicación:** Convierte la transacción individual nuevamente a JSON y la publica en su cola correspondiente.

### 2. Componente B: Consumer (Consumidor)
Este componente es el encargado de procesar los mensajes encolados y enviarlos a su destino final.
* **Escucha Multicola:** Se conecta a RabbitMQ y se suscribe a las colas generadas por el Producer.
* **Inyección de Datos (Examen Parcial):** Intercepta el mensaje JSON, lo mapea al objeto `Transaccion` e inyecta dinámicamente los datos del estudiante (`nombre`, `carnet`, `correo`).
* **Envío a Base de Datos:** Realiza una petición HTTP POST al segundo endpoint suministrado, enviando la transacción procesada en el Body de la solicitud.
* **Control de Calidad (ACK Manual):** * Si la API responde con éxito (HTTP 200 o 201), el sistema ejecuta un `channel.basicAck()` para confirmar que el mensaje fue procesado y puede eliminarse de RabbitMQ.
  * Si la API responde con un error o hay un fallo de conexión, el sistema ejecuta un `channel.basicNack(..., requeue=true)` para reencolar el mensaje, garantizando tolerancia a fallos y cero pérdida de datos.

---

##  Tecnologías y Herramientas Utilizadas
* **Lenguaje:** Java 11 (Uso de `java.net.http.HttpClient` para peticiones REST)
* **Gestor de Dependencias:** Maven
* **Broker de Mensajería:** RabbitMQ (Erlang)
* **Librerías Externas:**
  * `amqp-client` (Conexión oficial de RabbitMQ)
  * `jackson-databind` (Procesamiento y mapeo de JSON)

---

##  Cómo ejecutar el proyecto
1. Levantar el servicio local de **RabbitMQ** (accesible en `localhost:15672`).
2. Ejecutar la clase principal `App.java` del proyecto **TrasaccionesProducer** para descargar el lote de la API y llenar las colas.
3. Ejecutar la clase principal `App.java` del proyecto **Consumer** para procesar los mensajes encolados y enviarlos al endpoint POST.

##  Link del video
https://drive.google.com/file/d/1x42Zplic7QGqbjTlmTy_ciRNrAqZMqCM/view?usp=sharing
