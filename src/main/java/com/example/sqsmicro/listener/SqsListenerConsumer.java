package com.example.sqsmicro.listener;

import com.example.sqslib.iata.DepartureArrivalType;
import com.example.sqslib.iata.FlightLegIdentifierType;
import com.example.sqslib.iata.FlightLegType;
import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRS;
import com.example.sqslib.iata.OperationTimeType;
import com.example.sqslib.iata.OperationalStatusType;
import com.example.sqslib.iata.SuccessType;
import com.example.sqslib.iata.UsageType;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Slf4j
@Component
public class SqsListenerConsumer {

    private String colaAwsSqsProducer;
    private final DecryptEncryptMessageUtil decryptEncryptMessageUtil;
    private final SqsProducerService sqsProducerService;
    private final XmlService xmlService;

    public SqsListenerConsumer(@Value("${cola.aws.sqs.producer}") String colaAwsSqsTwo,
                               DecryptEncryptMessageUtil decryptEncryptMessageUtil,
                               SqsProducerService sqsProducerService,
                               XmlService xmlService) {
        this.colaAwsSqsProducer = colaAwsSqsTwo;
        this.decryptEncryptMessageUtil = decryptEncryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
        this.xmlService = xmlService;
    }

    @SqsListener("${cola.aws.sqs.consumer}")
    public void processMessage(MessageDto messageDto) {
        try {

            log.info("Reads encrypted messages. Metadata: {} | EncryptedPayload: {}", messageDto.metadata(), messageDto.encryptedPayload());
            // 1. DecryptPayload
            String decryptPayload = decryptEncryptMessageUtil.decryptHybrid(messageDto.encryptedPayload(), messageDto.keyId());
            // Log for tests without lobDebug
            log.debug("Decrypted payload message. Payload: {} ", decryptPayload);

            // 3. Convertir XML a Objeto Java (Generic)
            Object pojo = xmlService.fromXml(decryptPayload);

            // 4. Procesar según el tipo (Switch Expression + Pattern Matching)
            switch (pojo) {
                case IATAAIDXFlightLegRQ rq -> procesarFlightLegRQ(rq, messageDto.metadata());
                case IATAAIDXFlightLegNotifRQ rq2 -> procesarFlightLegNotifRQ(rq2, messageDto.metadata());
                case null -> throw new IllegalArgumentException("Payload null");
                default -> log.error("Message Type not supported: " + pojo.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }

    private void procesarFlightLegRQ(IATAAIDXFlightLegRQ rq, Map<String, String> metadata) throws Exception {
        // 1. Instanciar la respuesta raíz
        var response = new IATAAIDXFlightLegRS();

        // ---------------------------------------------------------
        // A. SETEO DE ATRIBUTOS (HEADERS XML)
        // ---------------------------------------------------------

        // Obligatorio: Versión del esquema (Coincide con tus XSD 21.3)
        response.setVersion(rq.getVersion());

        // Timestamp actual (El Adapter1 se encargará del formato ISO 8601)
        response.setTimeStamp(LocalDateTime.now());

        // Trazabilidad: Es VITAL devolver el mismo ID que recibiste para que (B) sepa qué responder
        response.setCorrelationID(rq.getCorrelationID());
        response.setTransactionIdentifier(rq.getTransactionIdentifier());

        // Identificador único de este mensaje de respuesta
        response.setTransactionStatusCode("Success"); // O "End" según el flujo
        response.setSequenceNmbr(rq.getSequenceNmbr());
        response.setTarget(rq.getTarget());

        // ---------------------------------------------------------
        // B. SETEO DE ESTADO (SUCCESS vs ERRORS)
        // ---------------------------------------------------------

        // En IATA, la presencia del elemento "Success" vacío indica éxito.
        // Si hubiera error, dejarías Success en null y llenarías setErrors(...)
        SuccessType success = new SuccessType();
        response.setSuccess(success);

        // ---------------------------------------------------------
        // C. SETEO DEL PAYLOAD (DATOS DE NEGOCIO)
        // ---------------------------------------------------------

        // Crear la info del vuelo (FlightLeg)
        // Nota: Como no pasaste la clase FlightLegType, asumo una estructura estándar IATA
        FlightLegType flightLegType = new FlightLegType();
        FlightLegIdentifierType legId = new FlightLegIdentifierType();

        FlightLegIdentifierType.Airline airline = new FlightLegIdentifierType.Airline();
        airline.setValue("QR");
        airline.setCodeContext("IATA");
        legId.setAirline(airline);
        legId.setFlightNumber("1234");

        FlightLegIdentifierType.ArrivalAirport arrivalAirport = new FlightLegIdentifierType.ArrivalAirport();
        arrivalAirport.setValue("GRU");
        arrivalAirport.setCodeContext("1234");
        legId.setArrivalAirport(arrivalAirport);
        FlightLegIdentifierType.DepartureAirport departureAirport = new FlightLegIdentifierType.DepartureAirport();
        departureAirport.setValue("LAX");
        departureAirport.setCodeContext("1234");
        legId.setDepartureAirport(departureAirport);

        flightLegType.setLegIdentifier(legId);

        // --- B. Datos del Vuelo (LegData) ---
        var legData = new FlightLegType.LegData();

        // B.1 Estado Operativo (Ej. "Schuduled", "OffBlock", "Airborne")
        var opStatus = new OperationalStatusType();
        opStatus.setValue("Schuduled"); // Helper method abajo
        opStatus.setCodeContext("Operational");
        // JAXB suele inicializar listas con un get().add() en vez de set()
        legData.getOperationalStatuses().add(opStatus);

        // B.2 Tiempos (Ej. Scheduled Time of Departure)
        var std = new OperationTimeType();
        std.setTimeType("S"); // S = Scheduled
        std.setOperationQualifier("TD"); // TD = Time of Departure
        std.setValue("+2"); // La hora
        legData.getOperationTimes().add(std);

        // B.3 Recursos de Aeropuerto (Ej. Gate/Terminal)
        var airportRes = new FlightLegType.LegData.AirportResources();
        airportRes.setUsage(UsageType.PLANNED);
        legData.getAirportResources().add(airportRes);

        // Asignar LegData al FlightLeg
        flightLegType.setLegData(legData);

        // Agregarlo a la lista
        response.getFlightLegs().add(flightLegType);

        // Marshalling y Encriptado (Para B)
        String xmlResponse = xmlService.toXml(response);

        // IMPORTANTE: Encriptar con la Pública de B
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlResponse);

        // Preparar Metadata de respuesta
        Map<String, String> responseMeta = new HashMap<>();
        responseMeta.put("message_type", "IATAAIDXFlightLegRS"); // Tipo de respuesta
        responseMeta.put("correlation_id", metadata.get("correlation_id")); // Mantener trazabilidad

        MessageDto responseDto = new MessageDto(
                responseMeta,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.debug("Before preparing the SQS shipment. Metadata: {}", responseMeta);
        sqsProducerService.send(colaAwsSqsProducer, responseDto);
    }

    private void procesarFlightLegNotifRQ(IATAAIDXFlightLegNotifRQ rq, Map<String, String> metadata) throws Exception {
        // 1. Instanciar la respuesta raíz
        var response = new IATAAIDXFlightLegRS();

        // ---------------------------------------------------------
        // A. SETEO DE ATRIBUTOS (HEADERS XML)
        // ---------------------------------------------------------

        // Obligatorio: Versión del esquema (Coincide con tus XSD 21.3)
        response.setVersion(rq.getVersion());

        // Timestamp actual (El Adapter1 se encargará del formato ISO 8601)
        response.setTimeStamp(LocalDateTime.now());

        // Trazabilidad: Es VITAL devolver el mismo ID que recibiste para que (B) sepa qué responder
        response.setCorrelationID(rq.getCorrelationID());
        response.setTransactionIdentifier(rq.getTransactionIdentifier());

        // Identificador único de este mensaje de respuesta
        response.setTransactionStatusCode("Success"); // O "End" según el flujo
        response.setSequenceNmbr(rq.getSequenceNmbr());
        response.setTarget(rq.getTarget());

        // ---------------------------------------------------------
        // B. SETEO DE ESTADO (SUCCESS vs ERRORS)
        // ---------------------------------------------------------

        // En IATA, la presencia del elemento "Success" vacío indica éxito.
        // Si hubiera error, dejarías Success en null y llenarías setErrors(...)
        SuccessType success = new SuccessType();
        response.setSuccess(success);

        // ---------------------------------------------------------
        // C. SETEO DEL PAYLOAD (DATOS DE NEGOCIO)
        // ---------------------------------------------------------

        // Crear la info del vuelo (FlightLeg)
        // Nota: Como no pasaste la clase FlightLegType, asumo una estructura estándar IATA
        FlightLegType flightLegType = new FlightLegType();
        FlightLegIdentifierType legId = new FlightLegIdentifierType();

        FlightLegIdentifierType.Airline airline = new FlightLegIdentifierType.Airline();
        airline.setValue("QR");
        airline.setCodeContext("IATA");
        legId.setAirline(airline);
        legId.setFlightNumber("1234");

        FlightLegIdentifierType.ArrivalAirport arrivalAirport = new FlightLegIdentifierType.ArrivalAirport();
        arrivalAirport.setValue("GRU");
        arrivalAirport.setCodeContext("1234");
        legId.setArrivalAirport(arrivalAirport);
        FlightLegIdentifierType.DepartureAirport departureAirport = new FlightLegIdentifierType.DepartureAirport();
        departureAirport.setValue("LAX");
        departureAirport.setCodeContext("1234");
        legId.setDepartureAirport(departureAirport);

        flightLegType.setLegIdentifier(legId);

        // --- B. Datos del Vuelo (LegData) ---
        var legData = new FlightLegType.LegData();

        // B.1 Estado Operativo (Ej. "Schuduled", "OffBlock", "Airborne")
        var opStatus = new OperationalStatusType();
        opStatus.setValue("Schuduled"); // Helper method abajo
        opStatus.setCodeContext("Operational");
        // JAXB suele inicializar listas con un get().add() en vez de set()
        legData.getOperationalStatuses().add(opStatus);

        // B.2 Tiempos (Ej. Scheduled Time of Departure)
        var std = new OperationTimeType();
        std.setTimeType("S"); // S = Scheduled
        std.setOperationQualifier("TD"); // TD = Time of Departure
        std.setValue("+2"); // La hora
        legData.getOperationTimes().add(std);

        // B.3 Recursos de Aeropuerto (Ej. Gate/Terminal)
        var airportRes = new FlightLegType.LegData.AirportResources();
        airportRes.setUsage(UsageType.PLANNED);
        legData.getAirportResources().add(airportRes);

        // Asignar LegData al FlightLeg
        flightLegType.setLegData(legData);

        // Agregarlo a la lista
        response.getFlightLegs().add(flightLegType);

        // Marshalling y Encriptado (Para B)
        String xmlResponse = xmlService.toXml(response);

        // IMPORTANTE: Encriptar con la Pública de B
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlResponse);

        // Preparar Metadata de respuesta
        Map<String, String> responseMeta = new HashMap<>();
        responseMeta.put("message_type", "IATAAIDXFlightLegRS"); // Tipo de respuesta
        responseMeta.put("correlation_id", metadata.get("correlation_id")); // Mantener trazabilidad

        MessageDto responseDto = new MessageDto(
                responseMeta,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.debug("Before preparing the SQS shipment. Metadata: {}", responseMeta);
        sqsProducerService.send(colaAwsSqsProducer, responseDto);
    }
}
