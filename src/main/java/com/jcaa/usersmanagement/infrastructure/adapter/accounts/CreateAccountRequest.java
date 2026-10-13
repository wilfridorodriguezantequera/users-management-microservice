package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

/**
 * Cuerpo de POST /api/accounts. Los nombres de los campos son el contrato acordado con el
 * microservicio accounts: { "userId": "...", "email": "...", "nombre": "..." }.
 */
public record CreateAccountRequest(String userId, String email, String nombre) {}
