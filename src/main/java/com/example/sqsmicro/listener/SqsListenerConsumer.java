package com.example.sqsmicro.listener;

import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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


    public SqsListenerConsumer(@Value("${cola.aws.sqs.producer}") String colaAwsSqsTwo,
                               DecryptEncryptMessageUtil decryptEncryptMessageUtil,
                               SqsProducerService sqsProducerService) {
        this.colaAwsSqsProducer = colaAwsSqsTwo;
        this.decryptEncryptMessageUtil = decryptEncryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
    }

    @SqsListener("${cola.aws.sqs.consumer}")
    public void processMessage(MessageDto messageDto) {
        try {
            log.info("Reads encrypted messages. Metadata: {} | EncryptedPayload: {}", messageDto.metadata(), messageDto.encryptedPayload());
            // 1. DecryptPayload
            String decryptPayload = decryptEncryptMessageUtil.decryptHybrid(messageDto.encryptedPayload(), messageDto.keyId());
            // Log for tests without lobDebug
            log.debug("Decrypted payload message. Payload: {} ", decryptPayload);
            // 2. Process
            DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                    decryptEncryptMessageUtil.encryptHybridWithPrivateKey(decryptPayload);
            MessageDto message = new MessageDto(
                    Map.of("type", "SOME_CLASS_XML"),
                    encryptedMessageBundle.encryptedPayload(),
                    encryptedMessageBundle.encryptedKey()
            );
            // 3. Responding to cola-aws-sqs-2 using Lib (A)
            sqsProducerService.send(colaAwsSqsProducer, message);
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }
}
