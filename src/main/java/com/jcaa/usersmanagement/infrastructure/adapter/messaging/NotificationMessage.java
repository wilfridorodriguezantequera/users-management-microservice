package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;

/**
 * Mensaje que se publica en el topico de Kafka 'notificaciones'.
 *
 * <p>Los nombres de los campos son el contrato acordado por el grupo: users y accounts lo
 * escriben, notifiers lo lee. Si se cambia un nombre aqui, hay que cambiarlo en los tres
 * microservicios.
 *
 * <pre>
 * { "email": "...", "nombre": "...", "asunto": "...", "mensaje": "&lt;p&gt;...&lt;/p&gt;" }
 * </pre>
 */
public record NotificationMessage(String email, String nombre, String asunto, String mensaje) {

  public static NotificationMessage from(final EmailDestinationModel destination) {
    return new NotificationMessage(
        destination.getDestinationEmail(),
        destination.getDestinationName(),
        destination.getSubject(),
        destination.getBody());
  }
}
