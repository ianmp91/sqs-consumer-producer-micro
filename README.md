# Consumer/Producer Microservice (C)

This service acts as the central processor. It reads encrypted messages, processes them, and notifies the result.

## 🔄 Messaging Flow

1. **Consume (Decryption):** Reads messages from the input queue.
   * **Source:** `cola-aws-sqs-1`.
   * **Security:** Uses the **Private Key (RSA)** to **DECRYPT** the payload and access the raw data.
   * **Action:** Processes the business logic and **deletes** the message from the queue upon success.

2. **Produce:** Sends the process result or the next task.
   * **Destination:** `cola-aws-sqs-2` (to be read by Service B).

## 🛠 Requirements

* **OS:** macOS (Tested on Tahoe 26.1)
* **Java:** JDK 25
* **Infrastructure:** ElasticMQ (Docker)
* **Internal Library:** `sqs-consumer-producer-lib` (A)

## 🔑 Security Configuration

**IMPORTANT:** This service holds the **Private Key**. It must not be shared or uploaded to the repository if it is a production environment.

For local development, ensure the key is available:

```bash
export DECRYPTION_PRIVATE_KEY_PATH=/path/to/private_key.pem
```

## 📦 Installation for Microservices

Add the dependency to your `build.gradle` file (assuming this library is published in your local repository).

```groovy
repositories {
   mavenLocal() // Add to the microservice
   mavenCentral()
}


dependencies {
   implementation 'com.example.sqslib:sqs-consumer-producer-lib:0.0.1-SNAPSHOT'
}
```

## ⚙️ Configuration (application.yml)

```yaml
server:
   port: 8082
spring:
   application:
      name: sqs-consumer-producer-micro
   main:
      allow-bean-definition-overriding: true
   cloud:
      aws:
         region:
            static: us-east-1 # An arbitrary region
         # Specific configuration for SQS pointing to ElasticMQ
         sqs:
            # Points to the ElasticMQ container port
            endpoint: ${SPRING_CLOUD_AWS_SQS_ENDPOINT:http://localhost:9324}
         credentials:
            # Dummy credentials that satisfy the AWS SDK requirement
            access-key: dummy
            secret-key: dummy
cola:
   aws:
      sqs:
         consumer: "cola-aws-sqs-1"
         producer: "cola-aws-sqs-2"
```

## 🚀 Execution

```bash
make clean
make build
make bootJar
make bootRun
```