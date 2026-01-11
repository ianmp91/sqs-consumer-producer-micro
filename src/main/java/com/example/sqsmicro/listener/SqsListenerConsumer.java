package com.example.sqsmicro.listener;

import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.builders.FlightLegBuilder;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final FlightLegBuilder flightLegBuilder;

    public SqsListenerConsumer(@Value("${cola.aws.sqs.producer}") String colaAwsSqsTwo,
                               DecryptEncryptMessageUtil decryptEncryptMessageUtil,
                               SqsProducerService sqsProducerService,
                               XmlService xmlService,
                               FlightLegBuilder flightLegBuilder) {
        this.colaAwsSqsProducer = colaAwsSqsTwo;
        this.decryptEncryptMessageUtil = decryptEncryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
        this.xmlService = xmlService;
        this.flightLegBuilder = flightLegBuilder;
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
                case IATAAIDXFlightLegRQ rq -> procesarFlightLegRQ(rq, messageDto);
                case IATAAIDXFlightLegNotifRQ rq2 -> procesarFlightLegNotifRQ(rq2, messageDto);
                case null -> throw new IllegalArgumentException("Payload null");
                default -> log.error("Message Type not supported: " + pojo.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }

    private void procesarFlightLegRQ(IATAAIDXFlightLegRQ rq, MessageDto messageDto) throws Exception {
        // 1. Instanciar la respuesta raíz
        var response = flightLegBuilder.buildFlightLegRs(rq, messageDto.metadata());

        // Marshalling y Encriptado (Para B)
        String xmlResponse = xmlService.toXml(response);

        // IMPORTANTE: Encriptar con la Pública de B
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlResponse);

        // Preparar Metadata de respuesta
        Map<String, String> responseMeta = new HashMap<>();
        responseMeta.put("message_type", "IATAAIDXFlightLegRS"); // Tipo de respuesta
        responseMeta.put("correlation_id", messageDto.metadata().get("correlation_id")); // Mantener trazabilidad

        MessageDto responseDto = new MessageDto(
                responseMeta,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey(),
                messageDto.uniqueFlightId()
        );
        log.debug("Before preparing the SQS shipment. Metadata: {}", responseMeta);
        log.debug("Before preparing the SQS shipment. UniqueFlightId: {}", responseDto.uniqueFlightId());
        sqsProducerService.send(colaAwsSqsProducer, responseDto);
    }

    private void procesarFlightLegNotifRQ(IATAAIDXFlightLegNotifRQ rq, MessageDto messageDto) throws Exception {
        // 1. Instanciar la respuesta raíz
        var response = flightLegBuilder.buildFlightLegRsToNotifRq(rq, messageDto.metadata());

        // Marshalling y Encriptado (Para B)
        String xmlResponse = xmlService.toXml(response);

        // IMPORTANTE: Encriptar con la Pública de B
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlResponse);

        // Preparar Metadata de respuesta
        Map<String, String> responseMeta = new HashMap<>();
        responseMeta.put("message_type", "IATAAIDXFlightLegRS"); // Tipo de respuesta
        responseMeta.put("correlation_id", messageDto.metadata().get("correlation_id")); // Mantener trazabilidad

        MessageDto responseDto = new MessageDto(
                responseMeta,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey(),
                messageDto.uniqueFlightId()
        );
        log.debug("Before preparing the SQS shipment. Metadata: {}", responseMeta);
        log.debug("Before preparing the SQS shipment. UniqueFlightId: {}", responseDto.uniqueFlightId());
        sqsProducerService.send(colaAwsSqsProducer, responseDto);
    }
}
