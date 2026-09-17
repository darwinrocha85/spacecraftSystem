package com.rocha.spacecraftmanagementsystem.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

// Fase de integracion: cobro real de entradas contra el backend de BankIn
// (ver BANKIN-INTEGRATION.md en la raiz del repo para el contrato completo).
@Service
public class BankInPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(BankInPaymentService.class);

    // Precio por defecto cuando la nave no tiene ticketPrice configurado todavia (demo).
    public static final double DEFAULT_TICKET_PRICE = 25.00;

    // Mensaje mostrado al comprador cuando naveSpace no logra conectarse a BankIn (host caido, timeout,
    // BANKIN_API_BASE mal apuntado, etc.). Es un demo: a diferencia del 401 (API key mal configurada,
    // que es un error nuestro y no se muestra), esto SI es seguro/util decirlo tal cual.
    private static final String BANKIN_UNREACHABLE_MESSAGE =
            "No se pudo conectar con BankIn. Asegurate de que la app de BankIn este corriendo (y que BANKIN_API_BASE apunte a esa URL) e intenta de nuevo.";

    @Value("${bankin.api.base}")
    private String apiBase;

    @Value("${bankin.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cobra cardId por amount contra BankIn (POST /transactions/purchase). Si BankIn confirma el
    // cobro (201) devuelve el resultado; en cualquier otro caso lanza una excepcion:
    //  - IllegalArgumentException: motivo seguro de mostrar al comprador (tarjeta no encontrada -404-,
    //    tarjeta inactiva/saldo insuficiente/monto invalido -400-, o BankIn inalcanzable).
    //  - IllegalStateException: error de configuracion (401 - API key invalida) o cualquier otra falla
    //    inesperada. El detalle SOLO se registra en el log del servidor - nunca se muestra al comprador.
    public ChargeResult charge(String cardId, double amount, String note) {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId is required to charge with BankIn");
        }

        double roundedAmount = Math.round(amount * 100) / 100.0;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey);
        }

        HttpEntity<ChargeRequest> entity = new HttpEntity<>(new ChargeRequest(cardId, roundedAmount, note), headers);

        try {
            ResponseEntity<ChargeResponse> response = restTemplate.postForEntity(
                    apiBase + "/transactions/purchase", entity, ChargeResponse.class);

            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                logger.error("BankIn respondio {} en vez de 201 al cobrar la tarjeta {}", response.getStatusCode(), cardId);
                throw new IllegalStateException("No se pudo procesar el pago en este momento.");
            }

            ChargeResponse body = response.getBody();
            return new ChargeResult(body.id(), body.amount());

        } catch (HttpClientErrorException.NotFound e) {
            // BankIn ya manda un detail especifico ("tarjeta no encontrada"); se muestra tal cual al comprador.
            throw new IllegalArgumentException(
                    extractDetail(e, "No encontramos esa tarjeta en BankIn. Verifica el numero e intenta de nuevo."));

        } catch (HttpClientErrorException.BadRequest e) {
            // BankIn distingue tarjeta no activa / fondos insuficientes / monto invalido en su propio
            // detail - se reenvia tal cual para que el comprador vea el motivo exacto del rechazo.
            throw new IllegalArgumentException(
                    extractDetail(e, "BankIn rechazo el cobro (tarjeta inactiva, saldo insuficiente o monto invalido)."));

        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("BankIn rechazo la X-Api-Key configurada (401). Revisa BANKIN_API_KEY. Respuesta: {}",
                    e.getResponseBodyAsString());
            throw new IllegalStateException("No se pudo procesar el pago en este momento.");

        } catch (ResourceAccessException e) {
            // Fallo de conectividad (host caido, timeout, DNS, BANKIN_API_BASE apuntando a otro lado) -
            // no es un error de datos del comprador, asi que si es seguro mostrarlo tal cual (es un demo).
            logger.error("No se pudo conectar con BankIn en {}: {}", apiBase, e.getMessage());
            throw new IllegalArgumentException(BANKIN_UNREACHABLE_MESSAGE);

        } catch (RestClientException e) {
            logger.error("Fallo inesperado al llamar a BankIn en {}: {}", apiBase, e.getMessage());
            throw new IllegalStateException("No se pudo procesar el pago en este momento.");
        }
    }

    // Revierte el cobro de una compra cancelada (POST /transactions/{id}/reverse). A diferencia de
    // /transactions/{id}/annul (panel de gerente, requiere manager_id), reverse no lo pide - por eso
    // naveSpace puede dispararlo solo, sin intervencion humana, al cancelar una entrada.
    // Best-effort: si BankIn no esta disponible o rechaza la reversa, se registra en el log pero NO se
    // bloquea la cancelacion local - el cupo/asiento se libera igual (no hay reintento automatico).
    // Fase 7: devuelve si la reversion se confirmo o no, solo para que el email de cancelacion pueda
    // decir con precision si el reembolso quedo confirmado o no (sigue sin bloquear ni reintentar nada).
    public boolean reverse(Long transactionId) {
        if (transactionId == null) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(apiBase + "/transactions/" + transactionId + "/reverse",
                    HttpMethod.POST, entity, String.class);
            logger.info("BankIn: transaccion {} revertida por cancelacion de entrada.", transactionId);
            return true;
        } catch (RestClientException e) {
            logger.error("No se pudo revertir en BankIn la transaccion {} (la entrada se cancela igual, sin reintento automatico): {}",
                    transactionId, e.getMessage());
            return false;
        }
    }

    private String extractDetail(HttpClientErrorException e, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(e.getResponseBodyAsString());
            if (node.has("detail") && !node.get("detail").isNull()) {
                return node.get("detail").asText();
            }
        } catch (Exception parseError) {
            logger.warn("No se pudo leer el detalle del error {} de BankIn: {}", e.getStatusCode(), parseError.getMessage());
        }
        return fallback;
    }

    // ---- request/response de BankIn (POST /transactions/purchase) ----

    private record ChargeRequest(@JsonProperty("card_id") String cardId, double amount, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeResponse(Long id, double amount, String status) {
    }

    // Resultado minimo que le interesa a naveSpace: el id de transaccion de BankIn (para guardarlo
    // junto a la venta) y el monto que BankIn efectivamente registro.
    public record ChargeResult(Long transactionId, double amount) {
    }
}
