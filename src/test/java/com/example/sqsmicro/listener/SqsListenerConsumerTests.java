package com.example.sqsmicro.listener;

import com.example.sqslib.builders.FlightLegBuilder;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.service.XmlService;
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
    private FlightLegBuilder flightLegBuilder;

    @Mock
    private DecryptEncryptMessageUtil decryptEncryptMessageUtil;

    @Mock
    private SqsProducerService sqsProducerService;

    @Mock
    private XmlService xmlService;

    @InjectMocks
    private SqsListenerConsumer sqsListenerConsumer;

    @BeforeEach
    void setUp() {
        decryptEncryptMessageUtil = Mockito.mock(DecryptEncryptMessageUtil.class);
        sqsProducerService = Mockito.mock(SqsProducerService.class);
        sqsListenerConsumer = new SqsListenerConsumer(
                "cola-aws-sqs-2",
                decryptEncryptMessageUtil,
                sqsProducerService,
                xmlService,
                flightLegBuilder
        );
    }

    @Test
    void shouldDecryptProcessAndSendResponse() throws Exception {
        /*String keyId = "keyId";
        String xmlPayload = "<IATAAIDXFlightLegRS>some xml</IATAAIDXFlightLegRS>";
        MessageDto mesagge = new MessageDto(
                Map.of("MESSAGE_TYPE", "IATAAIDXFlightLegRS"),
                xmlPayload,
                "keyId"
        );
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                new DecryptEncryptMessageUtil.EncryptedMessageBundle(xmlPayload, keyId);
        when(decryptEncryptMessageUtil.decryptHybrid(xmlPayload, mesagge.keyId())).thenReturn(xmlPayload);
        when(decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlPayload)).thenReturn(encryptedMessageBundle);
        sqsListenerConsumer.processMessage(mesagge);
        verify(decryptEncryptMessageUtil).decryptHybrid(xmlPayload, mesagge.keyId());
        verify(decryptEncryptMessageUtil).encryptHybridWithPublicKey(xmlPayload);
        verify(sqsProducerService).send(eq("cola-aws-sqs-2"), eq(mesagge));*/
    }
}
