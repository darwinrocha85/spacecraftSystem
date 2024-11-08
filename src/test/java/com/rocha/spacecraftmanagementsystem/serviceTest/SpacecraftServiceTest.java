package com.rocha.spacecraftmanagementsystem.serviceTest;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpacecraftServiceTest {

    @Mock
    private SpacecraftRepository spacecraftRepository;

    @InjectMocks
    private SpacecraftService spacecraftService;

    @Test
    void testGetSpacecraftById_Success() {
        // Arrange: configurar los datos de prueba y el comportamiento del mock
        Long spacecraftId = 1L;
        Spacecraft mockSpacecraft = new Spacecraft();
        mockSpacecraft.setId(spacecraftId);
        mockSpacecraft.setName("USS Enterprise");
        when(spacecraftRepository.findById(spacecraftId)).thenReturn(Optional.of(mockSpacecraft));

        // Act: llamar al método que estamos probando
        Spacecraft result = spacecraftService.getSpacecraftById(spacecraftId);

        // Assert: verificar que el resultado sea el esperado
        assertNotNull(result);
        assertEquals(spacecraftId, result.getId());
        assertEquals("USS Enterprise", result.getName());
        verify(spacecraftRepository, times(1)).findById(spacecraftId);
    }

    @Test
    void testGetSpacecraftById_NotFound() {
        // Arrange: configurar el mock para que retorne vacío cuando no encuentra la nave
        Long spacecraftId = 99L;
        when(spacecraftRepository.findById(spacecraftId)).thenReturn(Optional.empty());

        // Act & Assert: verificar que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () -> {
            spacecraftService.getSpacecraftById(spacecraftId);
        });

        assertEquals("Spacecraft not found with ID: " + spacecraftId, exception.getMessage());
        verify(spacecraftRepository, times(1)).findById(spacecraftId);
    }
}
