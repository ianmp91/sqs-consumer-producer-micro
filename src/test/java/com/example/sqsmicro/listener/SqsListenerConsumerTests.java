package com.example.sqsmicro.listener;

import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@ExtendWith(MockitoExtension.class)
public class SqsListenerConsumerTests {

    @Mock
    private DecryptEncryptMessageUtil decryptEncryptMessageUtil;

    @Mock
    private SqsProducerService sqsProducerService;

    @InjectMocks
    private SqsListenerConsumer sqsListenerConsumer;

    @BeforeEach
    void setUp() {
        decryptEncryptMessageUtil = Mockito.mock(DecryptEncryptMessageUtil.class);
        sqsProducerService = Mockito.mock(SqsProducerService.class);
        sqsListenerConsumer = new SqsListenerConsumer(
                "cola-aws-sqs-2",
                decryptEncryptMessageUtil,
                sqsProducerService
        );
    }

    @Test
    void shouldDecryptProcessAndSendResponse() throws Exception {
        String encryptedPayload = "BASE64_ENCRYPTED_STRING";
        String decryptedPayload = "LAX-123";
        String keyId = "keyId";
        MessageDto mesagge = new MessageDto(
                Map.of("type", "SOME_CLASS_XML"),
                encryptedPayload,
                "keyId"
        );
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                new DecryptEncryptMessageUtil.EncryptedMessageBundle(encryptedPayload, keyId);
        when(decryptEncryptMessageUtil.decryptHybrid(encryptedPayload, mesagge.keyId())).thenReturn(decryptedPayload);
        when(decryptEncryptMessageUtil.encryptHybridWithPrivateKey(decryptedPayload)).thenReturn(encryptedMessageBundle);
        sqsListenerConsumer.processMessage(mesagge);
        verify(decryptEncryptMessageUtil).decryptHybrid(encryptedPayload, mesagge.keyId());
        verify(decryptEncryptMessageUtil).encryptHybridWithPrivateKey(decryptedPayload);
        verify(sqsProducerService).send(eq("cola-aws-sqs-2"), eq(mesagge));
    }
}
