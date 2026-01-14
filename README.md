# Consumer/Producer Microservice (C)

## Airport-C Microservice (Consumer/Producer)

**Tech Stack:** Java 25, Gradle Groovy, Spring Boot 4, Spring AWS Cloud SQS.

### Overview
This microservice acts as both a consumer and a producer/listener within the distributed system. Its primary workflow involves:

1.  **Consuming:** It reads, processes, and deletes messages from `cola-aws-sqs-1`.
2.  **Decryption:** Upon reading a message from `cola-aws-sqs-1`, it decrypts the content using its private key via the `decryptHybrid` method.
3.  **Processing & Producing:**
   * It utilizes the shared library `sqs-consumer-producer-lib` (A) to act as a producer.
   * Once processing is complete, it generates a result or defines the next task.
   * It wraps this data in a `MessageDto` and sends it to `cola-aws-sqs-2` for processing by the **Airlines-B** microservice.
4.  **Security & Configuration:**
   * It retrieves the **Airlines-B Public Key** from **Config-Server-D** via REST (using `ExternalConfigServer` + `RestClient`) to encrypt the outbound payload.
   * It maintains its own Private Key locally for decrypting inbound messages.
   * It fetches SQS queue configurations (Inbound: `cola-aws-sqs-1`, Outbound: `cola-aws-sqs-2`) from **Config-Server-D**.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant D as Config-Server-D
    participant Q1 as cola-aws-sqs-1 (Inbound)
    participant C as Airport-C
    participant Q2 as cola-aws-sqs-2 (Outbound)

    Note over C: Startup / Configuration Phase
    C->>D: REST GET (ExternalConfigServer + RestClient)<br/>Fetch Queues & Airlines-B Public Key
    D-->>C: Return Config & Public Key

    Note over C: Runtime Phase
    C->>Q1: Poll/Read Message
    activate C
    Q1-->>C: Deliver Encrypted Message
    
    C->>C: decryptHybrid(payload) using Private Key
    C->>C: Process Business Logic
    C->>C: Create MessageDto & Encrypt using Airlines-B Public Key
    
    C->>Q2: Send Result/Next Task
    C->>Q1: Acknowledge & Delete Message
    deactivate C
```

## 🚀 Execution

```bash
make clean
make build
make bootJar
make bootRun
```