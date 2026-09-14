package com.rocha.spacecraftmanagementsystem.exception;

// Excepcion generica de "no encontrado" para los recursos de Fase 2
// (tickets, eventos) que no ameritan su propia clase dedicada como SpacecraftNotFoundException.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}