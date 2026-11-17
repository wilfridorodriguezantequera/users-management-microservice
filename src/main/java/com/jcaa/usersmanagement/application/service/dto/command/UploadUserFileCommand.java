package com.jcaa.usersmanagement.application.service.dto.command;

/** Datos para subir un archivo de un usuario. */
public record UploadUserFileCommand(
    String userId, String fileName, String contentType, byte[] content) {}
