package com.example.sqsmicro.listener;

import com.example.sqslib.builders.FlightLegBuilder;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.services.ConfigurationLoaderService;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@ExtendWith(MockitoExtension.class)
public class SqsListenerConsumerTests {

    @Mock
    private ConfigurationLoaderService configurationLoaderService;

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
                decryptEncryptMessageUtil,
                sqsProducerService,
                xmlService,
                flightLegBuilder,
                configurationLoaderService
        );
    }

    @Test
    void shouldDecryptProcessAndSendResponse() throws Exception {
        /*String encryptedKey = "encryptedKey";
        String xmlPayload = "<IATAAIDXFlightLegRS>some xml</IATAAIDXFlightLegRS>";
        MessageDto mesagge = new MessageDto(
                Map.of("MESSAGE_TYPE", "IATAAIDXFlightLegRS"),
                xmlPayload,
                "encryptedKey"
        );
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                new DecryptEncryptMessageUtil.EncryptedMessageBundle(xmlPayload, encryptedKey);
        when(decryptEncryptMessageUtil.decryptHybrid(xmlPayload, mesagge.encryptedKey())).thenReturn(xmlPayload);
        when(decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlPayload)).thenReturn(encryptedMessageBundle);
        sqsListenerConsumer.processMessage(mesagge);
        verify(decryptEncryptMessageUtil).decryptHybrid(xmlPayload, mesagge.encryptedKey());
        verify(decryptEncryptMessageUtil).encryptHybridWithPublicKey(xmlPayload);
        verify(sqsProducerService).send(eq("cola-aws-sqs-2"), eq(mesagge));*/
    }
}
