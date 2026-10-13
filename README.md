# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Secretos para desarrollo local

Copiar `application-secrets.properties.example` como
`.environments/application-secrets.properties` y reemplazar los valores de ejemplo.
Este archivo contiene la conexion local a MySQL, la semilla JWT y las credenciales de
Brevo. La carpeta `.environments` esta excluida de Git y del contexto de Docker.

En produccion no se utiliza este archivo: los secretos deben configurarse mediante las
variables de entorno `DB_*`, `JWT_SECRET`, `KAFKA_*` y `ACCOUNTS_URL` del proveedor de despliegue.

## Docker Compose local

Copiar `compose.env.example` como `.env`, reemplazar todos los valores y ejecutar:

```bash
docker compose up --build
```

Compose se detiene con un error si falta `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`,
`JWT_SECRET`, `KAFKA_BOOTSTRAP_SERVERS` o `ACCOUNTS_URL`. El archivo `.env` esta excluido
de Git y del contexto de construccion Docker. Los puertos de MySQL y la API se publican
solo en `127.0.0.1`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`, `DB_PORT`,
   `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_USERNAME`,
   `KAFKA_PASSWORD`, `KAFKA_CA_CERT` y `ACCOUNTS_URL`.
3. Usar una instancia MySQL accesible desde Internet o desde la red privada de
   Render y ejecutar `src/main/resources/schema.sql` una vez para crear el esquema.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.

## Notificaciones por correo (Kafka) y creacion de cuentas

Este servicio ya no envia correos directamente. Forma parte de un sistema de tres
microservicios (users, accounts y notifiers):

- **Paso 2 (async):** al crear o actualizar un usuario, `KafkaEmailPublisherAdapter`
  publica en el topico `notificaciones` un mensaje JSON con el formato acordado:
  `{ "email", "nombre", "asunto", "mensaje" }`. El microservicio notifiers lo lee y lo
  envia con Brevo. La publicacion se hace fuera del hilo HTTP (`notificationExecutor`).
- **Paso 5 (sync):** al crear un usuario, `AccountsRestClientAdapter` llama a
  `POST {ACCOUNTS_URL}/api/accounts` con `{ "userId", "email", "nombre" }`. Si accounts
  falla, el usuario igual queda creado y el error se registra en el log.

| Variable | Ejemplo / valor |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka-xxxx.aivencloud.com:12345` (Service URI de Aiven) |
| `KAFKA_TOPIC` | `notificaciones` |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` |
| `KAFKA_SASL_MECHANISM` | `SCRAM-SHA-256` |
| `KAFKA_USERNAME` / `KAFKA_PASSWORD` | usuario y contrasena SASL de Aiven |
| `KAFKA_CA_CERT` | contenido completo de `ca.pem` (incluye las lineas BEGIN/END) |
| `ACCOUNTS_URL` | `https://accounts-xxxx.onrender.com` |
