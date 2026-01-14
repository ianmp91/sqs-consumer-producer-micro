package com.example.sqsmicro.listener;

import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.builders.FlightLegBuilder;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.service.ConfigurationLoaderService;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
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

    private final DecryptEncryptMessageUtil decryptEncryptMessageUtil;
    private final SqsProducerService sqsProducerService;
    private final XmlService xmlService;
    private final FlightLegBuilder flightLegBuilder;
    private final ConfigurationLoaderService configurationLoaderService;

    public SqsListenerConsumer(DecryptEncryptMessageUtil decryptEncryptMessageUtil,
                               SqsProducerService sqsProducerService,
                               XmlService xmlService,
                               FlightLegBuilder flightLegBuilder,
                               ConfigurationLoaderService configurationLoaderService) {
        this.decryptEncryptMessageUtil = decryptEncryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
        this.xmlService = xmlService;
        this.flightLegBuilder = flightLegBuilder;
        this.configurationLoaderService = configurationLoaderService;
    }

    @SqsListener(queueNames = "#{@configurationLoaderService.getListeningQueue()}")
    public void processMessage(MessageDto messageDto) {
        try {
            log.info("Reads encrypted messages. Metadata: {} | EncryptedPayload: {}", messageDto.metadata(), messageDto.encryptedPayload());
            // 1. DecryptPayload
            String targetQueue = configurationLoaderService.getQueueUrl();
            String receiverPubKey = configurationLoaderService.getAirportPublicKey();
            decryptEncryptMessageUtil.loadPublicKey(receiverPubKey);
            String decryptPayload = decryptEncryptMessageUtil.decryptHybrid(messageDto.encryptedPayload(), messageDto.encryptedKey());
            // Log for tests without lobDebug
            log.debug("Decrypted payload message. Payload: {} ", decryptPayload);

            // 3. Convertir XML a Objeto Java (Generic)
            Object pojo = xmlService.fromXml(decryptPayload);

            // 4. Procesar según el tipo (Switch Expression + Pattern Matching)
            switch (pojo) {
                case IATAAIDXFlightLegRQ rq -> procesarFlightLegRQ(rq, messageDto, targetQueue);
                case IATAAIDXFlightLegNotifRQ rq2 -> procesarFlightLegNotifRQ(rq2, messageDto, targetQueue);
                case null -> throw new IllegalArgumentException("Payload null");
                default -> log.error("Message Type not supported: " + pojo.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }

    private void procesarFlightLegRQ(IATAAIDXFlightLegRQ rq, MessageDto messageDto, String targetQueue) throws Exception {
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
        sqsProducerService.send(targetQueue, responseDto);
    }

    private void procesarFlightLegNotifRQ(IATAAIDXFlightLegNotifRQ rq, MessageDto messageDto, String targetQueue) throws Exception {
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
        sqsProducerService.send(targetQueue, responseDto);
    }
}
