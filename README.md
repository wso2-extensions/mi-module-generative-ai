# WSO2 Micro Integrator Low Code AI Framework

This repository contains the source code for the WSO2 Micro Integrator Low Code AI framework. This framework provides a set of tools and building blocks to integrate AI capabilities into your Synapse integration projects.

## Features

- **Prompt Templating**: Define templates for various AI operations.
- **Chat Completion**: Integrate with various LLMs (Large Language Models) for chat-based interactions.
- **Document Parsing**: Parse and process documents.
- **Document Splitting**: Split documents into paragraphs, sentences, and words for further processing.
- **Embedding Generation**: Generate embeddings for text data.
- **Vector Store Integration**: Integrate with vector stores like Pinecone, Chroma, and PostgreSQL for efficient data retrieval.

## Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher
- WSO2 Micro Integrator 4.4.0 or higher

### How to locally build and deploy the latest version

1. Clone the repository:
    ```sh
    git clone https://github.com/your-repo/mi-module-generative-ai.git
    cd mi-module-generative-ai
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```
3. Copy the the generated connector.zip file from the `target` directory to the `<VS_CODE_MI_PROJECT>/src/main/wso2mi/resources/connectors` directory of your integration project.

4. Update the `pom.xml` file of your integration project to include the connector:
    ```xml
    <dependency>
        <groupId>org.wso2.carbon.esb.module.ai</groupId>
        <artifactId>mi-module-generative-ai</artifactId>
        <version>1.0.0</version>
    </dependency>
    ```

5. Create an integration project using the WSO2 Micro Integrator Toolkit and run the project.

### How to use the latest stable version

1. Add AI module to your VS Code MI Project from tool palette.

2. Develop your integration project using the AI module and run the project.

## Synapse XML Configuration for AI Operations

### Prompt Template
Inputs:
- `prompt`: The prompt template using Synapse expressions.
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Parsed prompt as a json object of the [schema](src/main/resources/outputschema/prompt.json).

Following is an example configuration for prompt templating.
```xml
<ai.prompt>
    <prompt>What is the largest country among: ${payload.countries}</prompt>
    <responseVariable>ai_prompt</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.prompt>
```

### Chat Completion

Inputs:
- `system`: The system prompt (optional).
- `prompt`: The prompt user input.
- `outputType`: The output type you want to receive (string, int, float, boolean).
- `modelName`: The model name to use.
- `temperature`: The temperature parameter.
- `maxTokens`: The maximum number of tokens to generate.
- `topP`: The top-p parameter.
- `frequencyPenalty`: The frequency penalty parameter.
- `seed`: The seed parameter.
- `knowledge`: The knowledge base.
- `history`: The chat history.
- `maxHistory`: The maximum history length.
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Chat completion response as a json object of the [schema](src/main/resources/outputschema/chat.json).


Following is an example configuration for chat completion using the OpenAI API.
```xml
<ai.chat configKey="OPEN_AI_CONNECTION">
    <system>You are a helpful AI</system>
    <prompt>{${payload.question}}</prompt>
    <outputType>string</outputType>     
    <modelName>gpt-4o</modelName>
    <temperature>0.7</temperature>
    <maxTokens>4069</maxTokens>
    <topP>1</topP>
    <frequencyPenalty>0</frequencyPenalty>
    <seed></seed>
    <knowledge>{${vars.docs.payload}}</knowledge>
    <history>{${payload.history}}</history>
    <maxHistory>10</maxHistory>
    <responseVariable>ai_chat</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.chat>
```

### Parser

Inputs:
- `input`: The input data to parse (binary or text).
- `type`: The type of parsing to perform. ("markdown-to-text", "html-to-text", "pdf-to-text", "doc-to-text", "docx-to-text", "ppt-to-text", "pptx-to-text", "xls-to-text", "xlsx-to-text")
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Parsed document as a json object of the [schema](src/main/resources/outputschema/parse.json).

Following is an example configuration for document parsing using the OpenAI API.
```xml
<ai.parse>
    <input>{${payload.binary}}</input>
    <type>pdf-to-text</type>
    <responseVariable>ai_parse</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.parse>
```

### Splitter

Inputs:
- `input`: The input data to split as a string.
- `strategy`: The strategy to use for splitting. ("Recursive", "ByParagraph", "BySentence")
- `maxSegmentSize`: The maximum segment size.
- `maxOverlapSize`: The maximum overlap size.
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- `payload`: Splitted document as a json object of the [schema](src/main/resources/outputschema/split.json).

Following is an example configuration for document splitting.
```xml
<ai.split>
    <input>{${vars.ai_parse_695.payload}}</input>
    <strategy>Recursive</strategy>
    <maxSegmentSize>50</maxSegmentSize>
    <maxOverlapSize>10</maxOverlapSize>
    <responseVariable>splitted</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.split>
```

### Embedding

Inputs:
- `input`: The input data to generate embeddings as a string.
- `model`: The model to use for generating embeddings.
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Embeddings as a json object of the [schema](src/main/resources/outputschema/generateEmbeddings.json).

Following is an example configuration for generating embeddings.
```xml
<ai.generateEmbeddings configKey="OPEN_AI_1">
    <input>{${vars.splitted.payload}}</input>
    <model>text-embedding-3-small</model>
    <responseVariable>embeddings</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.generateEmbeddings>
```

### Add to Vector Store

Inputs:
- `input`: Array of Text embedding objects. You can directly use the output payload of the generateEmbeddings operation. Refer [TextEmbedding schema](src/main/resources/inputschema/textEmbedding.json)
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Status of the operation as a json object of the [schema](src/main/resources/outputschema/addToStore.json).
> Note: Output has an empty payload. The status is stored in the attributes. You can access the status using the following expression `${vars.<responseVariable>.attributes.status}`.

Following is an example configuration for adding embeddings to a vector store.
```xml
<ai.addToStore configKey="PINECONE_1">
    <input>{${vars.embeddings.payload}}</input>
    <responseVariable>store</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.addToStore>
```

### Retrieve from Vector Store

Inputs:
- `input`: Array of numbers ( embedding of the query text). Refer [TextEmbedding schema](src/main/resources/inputschema/textEmbedding.json)
- `maxResults`: Maximum number of results to retrieve.
- `minScore`: Minimum score to consider for a result.
- `responseVariable`: The variable to store the response.
- `overwriteBody`: Whether to overwrite the message body.

Outputs:
- Retrieved documents as a json object of the [schema](src/main/resources/outputschema/searchStore.json).

Following is an example configuration for retrieving embeddings from a vector store.
```xml
<ai.searchStore configKey="PINECONE_1">
    <input>{${vars.query.payload[0].embedding}}</input>
    <maxResults>5</maxResults>
    <minScore>0.5</minScore>
    <responseVariable>docs</responseVariable>
    <overwriteBody>false</overwriteBody>
</ai.searchStore>
```

## Synapse XML Configuration for AI Connections

### OpenAI

```xml
<localEntry key="OPEN_AI_1" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>OPEN_AI</connectionType>
    <name>OPEN_AI_1</name>
    <apiKey>YOUR_API_KEY</apiKey>
    <baseUrl>https://api.openai.com/v1</baseUrl>
  </ai.init>
</localEntry>
```

### Anthropic

```xml
<localEntry key="ANTHROPIC" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>ANTHROPIC</connectionType>
    <name>ANTHROPIC</name>
    <apiKey>YOUR_API_KEY</apiKey>
  </ai.init>
</localEntry>
```

### Mistral AI

```xml
<localEntry key="MISTRAL" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>MISTRAL_AI</connectionType>
    <name>MISTRAL</name>
    <apiKey>YOUR_API_KEY</apiKey>
  </ai.init>
</localEntry>
```

## Synapse XML Configuration for Vector Store Connections

### MI Vector Store
This is a simple in-registry vector store for testing/development purposes. It stores the embeddings in the MI registry. Do not use this in production. 

```xml
<localEntry key="VECTOR_STORE_1" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>MI_VECTOR_STORE</connectionType>
    <name>VECTOR_STORE_1</name>
    <persistence>Enable</persistence>
  </ai.init>
</localEntry>
```


### Chroma

```xml
<localEntry key="CHROMA" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>CHROMA_DB</connectionType>
    <name>CHROMA</name>
    <url>http://localhost:8000/</url>
    <collection>WSO2-MI</collection>
  </ai.init>
</localEntry>
```

### Pinecone

```xml
<localEntry key="PINECONE_1" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>PINECONE</connectionType>
    <apiKey>YOUR_API_KEY</apiKey>
    <index>mi</index>
    <namespace>ai</namespace>
    <cloud>AWS</cloud>
    <region>us-east-1</region>
    <dimension>1536</dimension>
    <name>PINECONE_1</name>
  </ai.init>
</localEntry>
```

### PostgreSQL

```xml
<localEntry key="POSTGRES" xmlns="http://ws.apache.org/ns/synapse">
  <ai.init>
    <connectionType>POSTGRE_SQL</connectionType>
    <name>POSTGRES</name>
    <host>localhost</host>
    <port>5432</port>
    <database>mi</database>
    <table>items</table>
    <dimension>1536</dimension>
    <user>isuruWij</user>
    <password>default</password>
  </ai.init>
</localEntry>
```

## Contributing

We welcome contributions! Please read our [contributing guidelines](CONTRIBUTING.md) for more details.

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for more details.
