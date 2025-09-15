# Configurando o Kafka Connect com Strimzi
Este guia explica como configurar o Kafka Connect usando o operador Strimzi no Kubernetes. O Kafka Connect é uma ferramenta poderosa para integrar o Kafka com outros sistemas, permitindo a movimentação de dados entre o Kafka e bancos de dados, sistemas de arquivos, entre outros.
## Pré-requisitos
- Um cluster Kubernetes em funcionamento.
- Strimzi Operator instalado no cluster Kubernetes.
- Um cluster Kafka em funcionamento gerenciado pelo Strimzi.
- `kubectl` configurado para interagir com seu cluster Kubernetes.
## Passo 1: Criar um Namespace
Primeiro, crie um namespace dedicado para o Kafka Connect:
```bash
kubectl create namespace kafka
```
## Passo 2: Criar um Secret para Autenticação (se necessário)
Se você estiver usando um registry privado para os conectores, crie um secret do tipo `docker-registry`:
```bash     

kubectl create secret docker-registry <nome-do-seu-secret> \
  --docker-server=docker.io \
  --docker-username=<username> \
  --docker-password=<senha> \
  --namespace kafka

```
Substitua `<nome-do-seu-secret>`, `<seu-registry-url>`, `<seu-username>`, e `<sua-senha>` pelos valores apropriados.

## Passo 3: Criar o Cluster Kafka Connect   

Aplique o arquivo:

```yaml
kubectl apply -f kafka-connect-cluster.yaml -n kafka

```
Implantando Conectores Básicos

## Passo 4: Implantar um Conector Básicos 
###  FileStreamSourceConnector
Este conector lerá dados de um arquivo no sistema de arquivos do worker do Connect e os publicará em um tópico Kafka.

Crie um topic chamado `file-source-topic`:
```bash
kubectl apply -f topic-source-output.yaml -n kafka
```
Acessar um pod do Kafka Connect Worker e criar um arquivo de origem:
```bash
# Encontre o nome de um dos pods do Kafka Connect
kubectl get pods -n kafka -l strimzi.io/cluster=kafka-connect

# Acesse o shell de um dos pods (substitua <connect-pod-name> pelo nome real)
kubectl exec -it <connect-pod-name> -n kafka -- bash

# Dentro do pod, crie o diretório e o arquivo de origem
mkdir -p /tmp/connect-data
echo "Linha 1 do arquivo" > /tmp/connect-data/my-source-file.txt
echo "Linha 2 do arquivo" >> /tmp/connect-data/my-source-file.txt
echo "Linha 3 do arquivo" >> /tmp/connect-data/my-source-file.txt
exit # Saia do shell do pod
```

Configurar e Implantação do KafkaConnector para o Source:

```bash
kubectl apply -f filestreamsource-connector.yaml -n kafka
```

Verificar a Implantação e os Dados no Tópico:
```bash
# Verifique se o conector está em execução

kubectl get KafkaConnector my-file-source-connector -n kafka -o yaml
# Verifique se o "status.connectorStatus.connector.state" é "RUNNING"
# e se as tasks também estão em "RUNNING".

```
Logs do Worker: Verifique os logs dos pods do Kafka Connect para observar a inicialização do conector e o processamento de dados.
```bash
kubectl logs <connect-pod-name> -n kafka
```
Consumir do Tópico Kafka: Use um consumidor simples para verificar se as mensagens estão chegando no file-source-topic.

```bash
# Acessar um pod do Kafka broker para usar o console consumer
kubectl exec -it kafka-cluster-kafka-0 -n kafka -- /bin/bash

# Dentro do pod, execute o console consumer
./bin/kafka-console-consumer.sh --bootstrap-server kafka-cluster-kafka-bootstrap:9092 --topic file-source-topic --from-beginning --property print.key=true --property print.value=true

# Você deverá ver as linhas do arquivo "kafka-source-file.txt"
exit # Saia do shell do pod

```
Adicionar mais dados ao arquivo de origem:
```bash
# Acesse novamente o pod do Kafka Connect Worker    
kubectl exec -it <connect-pod-name> -n kafka -- bash
echo "Linha 4 - Nova entrada" >> /tmp/connect-data/my-source-file.txt
exit
# Verifique novamente o consumidor para ver as novas mensagens chegando.
```
### FileStreamSinkConnector
Este conector lerá dados de um tópico Kafka e os gravará em um arquivo no sistema de arquivos do worker.
Acessar um pod do Kafka Connect Worker e criar o diretório de destino:
```bash 
kubectl exec -it <connect-pod-name> -n kafka -- bash
mkdir -p /tmp/connect-data/sink
exit
```
Aplicar o conector Sink:
```bash
kubectl apply -f filestreamsink-connector.yaml -n kafka
```
Verificar a Implantação e os Dados no Arquivo:
```bash
kubectl get KafkaConnector my-file-sink-connector -n kafka -o yaml
# Verifique se o estado é "RUNNING".
```
Logs do Worker:
```bash
kubectl logs -f <connect-pod-name> -n kafka | grep "my-file-sink-connector"
```
Verifique o arquivo de destino no pod do Kafka Connect Worker:
```bash
kubectl exec -it <connect-pod-name> -n kafka -- cat /tmp/connect-data/sink/my-sink-file.txt
```
Você deverá ver as linhas do "file-source-topic" gravadas neste arquivo.

##  Kafka Streams 
O Kafka Streams é uma biblioteca cliente para construir aplicativos e microserviços, onde os dados de entrada e saída são armazenados em tópicos do Kafka. Ele permite o processamento de fluxo de dados em tempo real com alta escalabilidade e tolerância a falhas.

Laboratório:

Utilizando o Kafka Connect e Kafka Streams juntos
Neste laboratório, você irá configurar um pipeline de dados simples usando Kafka Connect para mover dados de um arquivo para um tópico Kafka, processar esses dados usando uma aplicação Kafka Streams, e então mover os dados processados para outro arquivo usando outro conector Kafka Connect.

O fluxo geral será:

1. Arquivo JSON de Origem (ex: my-source-file.json)
2. Kafka Connect Source Connector: Lê o my-source-file.json e o publica em um tópico Kafka como mensagens JSON.
3. Kafka Streams Application: Lê do tópico de origem, processa/transforma as mensagens JSON, e publica em um novo tópico.
4. Kafka Connect Sink Connector: Lê do novo tópico (com os dados transformados) e escreve em um novo arquivo JSON de destino (ex: my-sink-file-transformed.json).
5. Verificação: Verifique o arquivo de destino para garantir que os dados foram processados corretamente.
   
### Passo 1: Criar um Tópico para JSON Raw (Original) e um para JSON Transformado
Aplique os arquivos de tópico:
```bash
kubectl apply -f json-topic.yaml -n kafka
```

Crie um arquivo chamado /tmp/connect-data/my-source-file.json (dentro do volume montado pelos seus pods do Kafka Connect, ou onde você espera que ele esteja acessível) com o seguinte conteúdo:

Neste labortório conecte ao pod do Kafka Connect e crie o arquivo:
```bash
kubectl exec -it <connect-pod-name> -n kafka -- bash
mkdir -p /tmp/connect-data
cat <<EOF > /tmp/connect-data/my-source-file.json
{"id":1,"name":"Alice","age":30}
{"id":2,"name":"Bob","age":25}
{"id":3,"name":"Charlie","age":35}
EOF
```

### Passo 2: Configurar e Implantar o Kafka Connect Source Connector para JSON
Aplique o conector source:
```bash
kubectl apply -f filestreamsource-json-connector.yaml -n kafka
``` 
Kafka Connect Sink Connector (Escrevendo JSON Transformado para Arquivo)
Crie um KafkaConnector para o sink que lerá do tópico com os dados já transformados. Aplicando o conector sink:
```bash
kubectl apply -f filestreamsink-json-connector.yaml -n kafka
```
### Passo 3: Implementar a Transformação com Kafka Streams

Esta é a parte que "muda" o arquivo. Você precisará de um aplicativo separado, escrito em Java (ou Scala, Kotlin), que utilize a biblioteca Kafka Streams. Esse aplicativo será implantado no Kubernetes como um Deployment regular.

