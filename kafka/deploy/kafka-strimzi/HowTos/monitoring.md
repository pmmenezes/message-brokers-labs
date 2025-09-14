
# Monitoramento do Kafka com Strimzi, Prometheus e Grafana

Este guia descreve como configurar o monitoramento do Apache Kafka implantado com Strimzi usando Prometheus e Grafana. O Strimzi oferece suporte nativo para a exposição de métricas do Kafka, que podem ser coletadas pelo Prometheus e visualizadas no Grafana.

## Instalação customizada do kube-prometheus-stack 
1. Ajuste o `values.yaml` do kube-prometheus-stack para incluir os ServiceMonitors, PodMonitors e Rules em todos os namespaces. Assim você não precisa “marcar” cada ServiceMonitor com rótulos específicos do release.

Para instalar o Prometheus e o Grafana, você pode usar o Helm Chart oficial da comunidade do Prometheus. Siga os passos abaixo: 

1. Adicione o repositório Helm do Prometheus:
   ```bash
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm repo update
   ```
2. Baixe e customize o arquivo values.yaml conforme necessário. Aqui está um exemplo básico que inclui configurações para monitorar todos os namespaces:

```bash
helm show values prometheus-community/kube-prometheus-stack > values.yaml
``` 
Edite o `values.yaml` para editar as seguintes configurações:

```yaml
prometheus:
  prometheusSpec:
    serviceMonitorSelectorNilUsesHelmValues: false   
    podMonitorSelectorNilUsesHelmValues: false
    ruleSelectorNilUsesHelmValues: false
    serviceMonitorSelector: {}
    podMonitorSelector: {}
    ruleSelector: {}
    serviceMonitorNamespaceSelector: {}
    podMonitorNamespaceSelector: {}
    ruleNamespaceSelector: {}  
    additionalScrapeConfigs:
      name: additional-scrape-configs
      key: prometheus-additional.yaml
additionalPrometheusRulesMap:
  strimzi-kube-state-metrics-rules:
    groups:
    - name: strimzi-kube-state-metrics
      rules:
        - alert: KafkaTopicNotReady
          expr: strimzi_kafka_topic_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaTopic {{ $labels.topicName }} is not ready"
        - alert: KafkaTopicDeprecated
          expr: strimzi_kafka_topic_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaTopic {{ $labels.topicName }} contains a deprecated configuration"
        - alert: KafkaUserNotReady
          expr: strimzi_kafka_user_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaUser {{ $labels.username }} is not ready"
        - alert: KafkaUserDeprecated
          expr: strimzi_kafka_user_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaUser {{ $labels.username }} contains a deprecated configuration"
        - alert: KafkaNotReady
          expr: strimzi_kafka_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi Kafka {{ $labels.name }} using {{ $labels.kafka_version }} is not ready"
        - alert: KafkaDeprecated
          expr: strimzi_kafka_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi Kafka {{ $labels.name }} contains a deprecated configuration"
        # KafkaNodePool is not having a ready status as this is implemented via Kafka resource
        - alert: KafkaNodePoolDeprecated
          expr: strimzi_kafka_node_pool_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaNodePool {{ $labels.name }} contains a deprecated configuration"
        # StrimziPodSet is not having any further information as it is an internal resource and doesn't get operated by the user
        - alert: KafkaRebalanceNotReady
          expr: strimzi_kafka_rebalance_resource_info{ready!="True",template!="true"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaRebalance {{ $labels.name }} is not ready"
        - alert: KafkaRebalanceProposalPending
          expr: strimzi_kafka_rebalance_resource_info{ready="True",template!="true",proposal_ready="True"}
          for: 1h
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaRebalance {{ $labels.name }} is in proposal pending state and waits for approval for more than 1h."
        - alert: KafkaRebalanceRebalancing
          expr: strimzi_kafka_rebalance_resource_info{ready="True",template!="true",rebalancing="True"}
          for: 1h
          labels:
            severity: info
          annotations:
            message: "Strimzi KafkaRebalance {{ $labels.name }} is taking longer than 1h."
        - alert: KafkaRebalanceDeprecated
          expr: strimzi_kafka_rebalance_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaRebalance {{ $labels.name }} contains a deprecated configuration"
        - alert: KafkaConnectNotReady
          expr: strimzi_kafka_connect_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaConnect {{ $labels.name }} is not ready"
        - alert: KafkaConnectDeprecated
          expr: strimzi_kafka_connect_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaConnect {{ $labels.name }} contains a deprecated configuration"
        - alert: KafkaConnectorNotReady
          expr: strimzi_kafka_connector_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaConnector {{ $labels.name }} is not ready"
        - alert: KafkaConnectorDeprecated
          expr: strimzi_kafka_connector_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaConnector {{ $labels.name }} contains a deprecated configuration"
        - alert: KafkaMirrorMaker2NotReady
          expr: strimzi_kafka_mm2_resource_info{ready!="True"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaMirrorMaker2 {{ $labels.name }} is not ready"
        - alert: KafkaMirrorMaker2Deprecated
          expr: strimzi_kafka_mm2_resource_info{deprecated="Warning"}
          for: 15m
          labels:
            severity: warning
          annotations:
            message: "Strimzi KafkaMirrorMaker2 {{ $labels.name }} contains a deprecated configuration"
  strimizi-kafka-rules:
    groups:
      - name: kafka
        rules:
        - alert: KafkaRunningOutOfSpace
          expr: kubelet_volume_stats_available_bytes{persistentvolumeclaim=~"data(-[0-9]+)?-(.+)-kafka-[0-9]+"} * 100 / kubelet_volume_stats_capacity_bytes{persistentvolumeclaim=~"data(-[0-9]+)?-(.+)-kafka-[0-9]+"} < 15
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka is running out of free disk space'
            description: 'There are only {{ $value }} percent available at {{ $labels.persistentvolumeclaim }} PVC'
        - alert: UnderReplicatedPartitions
          expr: kafka_server_replicamanager_underreplicatedpartitions > 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka under replicated partitions'
            description: 'There are {{ $value }} under replicated partitions on {{ $labels.kubernetes_pod_name }}'
        - alert: AbnormalControllerState
          expr: sum(kafka_controller_kafkacontroller_activecontrollercount) by (strimzi_io_name) != 1
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka abnormal controller state'
            description: 'There are {{ $value }} active controllers in the cluster'
        - alert: OfflinePartitions
          expr: sum(kafka_controller_kafkacontroller_offlinepartitionscount) > 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka offline partitions'
            description: 'One or more partitions have no leader'
        - alert: UnderMinIsrPartitionCount
          expr: kafka_server_replicamanager_underminisrpartitioncount > 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka under min ISR partitions'
            description: 'There are {{ $value }} partitions under the min ISR on {{ $labels.kubernetes_pod_name }}'
        - alert: OfflineLogDirectoryCount
          expr: kafka_log_logmanager_offlinelogdirectorycount > 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka offline log directories'
            description: 'There are {{ $value }} offline log directories on {{ $labels.kubernetes_pod_name }}'
        - alert: ScrapeProblem
          expr: up{kubernetes_namespace!~"openshift-.+",kubernetes_pod_name=~".+-kafka-[0-9]+"} == 0
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'Prometheus unable to scrape metrics from {{ $labels.kubernetes_pod_name }}/{{ $labels.instance }}'
            description: 'Prometheus was unable to scrape metrics from {{ $labels.kubernetes_pod_name }}/{{ $labels.instance }} for more than 3 minutes'
        - alert: ClusterOperatorContainerDown
          expr: count((container_last_seen{container="strimzi-cluster-operator"} > (time() - 90))) < 1 or absent(container_last_seen{container="strimzi-cluster-operator"})
          for: 1m
          labels:
            severity: major
          annotations:
            summary: 'Cluster Operator down'
            description: 'The Cluster Operator has been down for longer than 90 seconds'
        - alert: KafkaBrokerContainersDown
          expr: absent(container_last_seen{container="kafka",pod=~".+-kafka-[0-9]+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'All `kafka` containers down or in CrashLookBackOff status'
            description: 'All `kafka` containers have been down or in CrashLookBackOff status for 3 minutes'
        - alert: KafkaContainerRestartedInTheLast5Minutes
          expr: count(count_over_time(container_last_seen{container="kafka"}[5m])) > 2 * count(container_last_seen{container="kafka",pod=~".+-kafka-[0-9]+"})
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: 'One or more Kafka containers restarted too often'
            description: 'One or more Kafka containers were restarted too often within the last 5 minutes'
      - name: entityOperator
        rules:
        - alert: TopicOperatorContainerDown
          expr: absent(container_last_seen{container="topic-operator",pod=~".+-entity-operator-.+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'Container topic-operator in Entity Operator pod down or in CrashLookBackOff status'
            description: 'Container topic-operator in Entity Operator pod has been or in CrashLookBackOff status for 3 minutes'
        - alert: UserOperatorContainerDown
          expr: absent(container_last_seen{container="user-operator",pod=~".+-entity-operator-.+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'Container user-operator in Entity Operator pod down or in CrashLookBackOff status'
            description: 'Container user-operator in Entity Operator pod have been down or in CrashLookBackOff status for 3 minutes'
      - name: connect
        rules:
        - alert: ConnectContainersDown
          expr: absent(container_last_seen{container=~".+-connect",pod=~".+-connect-.+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'All Kafka Connect containers down or in CrashLookBackOff status'
            description: 'All Kafka Connect containers have been down or in CrashLookBackOff status for 3 minutes'
        - alert: ConnectFailedConnector
          expr: sum(kafka_connect_connector_status{status="failed"}) > 0
          for: 5m
          labels:
            severity: major
          annotations:
            summary: 'Kafka Connect Connector Failure'
            description: 'One or more connectors have been in failed state for 5 minutes,'
        - alert: ConnectFailedTask
          expr: sum(kafka_connect_worker_connector_failed_task_count) > 0
          for: 5m
          labels:
            severity: major
          annotations:
            summary: 'Kafka Connect Task Failure'
            description: 'One or more tasks have been in failed state for 5 minutes.'
      - name: bridge
        rules:
        - alert: BridgeContainersDown
          expr: absent(container_last_seen{container=~".+-bridge",pod=~".+-bridge-.+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'All Kafka Bridge containers down or in CrashLookBackOff status'
            description: 'All Kafka Bridge containers have been down or in CrashLookBackOff status for 3 minutes'
        - alert: AvgProducerLatency
          expr: strimzi_bridge_kafka_producer_request_latency_avg > 10
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka Bridge producer average request latency'
            description: 'The average producer request latency is {{ $value }} on {{ $labels.clientId }}'
        - alert: AvgConsumerFetchLatency
          expr: strimzi_bridge_kafka_consumer_fetch_latency_avg > 500
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka Bridge consumer average fetch latency'
            description: 'The average consumer fetch latency is {{ $value }} on {{ $labels.clientId }}'
        - alert: AvgConsumerCommitLatency
          expr: strimzi_bridge_kafka_consumer_commit_latency_avg > 200
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Kafka Bridge consumer average commit latency'
            description: 'The average consumer commit latency is {{ $value }} on {{ $labels.clientId }}'
        - alert: Http4xxErrorRate
          expr: strimzi_bridge_http_server_requestCount_total{code=~"^4..$", container=~"^.+-bridge", path !="/favicon.ico"} > 10
          for: 1m
          labels:
            severity: warning
          annotations:
            summary: 'Kafka Bridge returns code 4xx too often'
            description: 'Kafka Bridge returns code 4xx too much ({{ $value }}) for the path {{ $labels.path }}'
        - alert: Http5xxErrorRate
          expr: strimzi_bridge_http_server_requestCount_total{code=~"^5..$", container=~"^.+-bridge"} > 10
          for: 1m
          labels:
            severity: warning
          annotations:
            summary: 'Kafka Bridge returns code 5xx too often'
            description: 'Kafka Bridge returns code 5xx too much ({{ $value }}) for the path {{ $labels.path }}'
      - name: mirrorMaker2
        rules:
        - alert: MirrorMaker2ContainerDown
          expr: absent(container_last_seen{container=~".+-mirrormaker2",pod=~".+-mirrormaker2-.+"})
          for: 3m
          labels:
            severity: major
          annotations:
            summary: 'All Kafka Mirror Maker 2 containers down or in CrashLookBackOff status'
            description: 'All Kafka Mirror Maker 2 containers have been down or in CrashLookBackOff status for 3 minutes'
      - name: kafkaExporter
        rules:
        - alert: UnderReplicatedPartition
          expr: kafka_topic_partition_under_replicated_partition > 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Topic has under-replicated partitions'
            description: 'Topic  {{ $labels.topic }} has {{ $value }} under-replicated partition {{ $labels.partition }}'
        - alert: TooLargeConsumerGroupLag
          expr: kafka_consumergroup_lag > 1000
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'Consumer group lag is too big'
            description: 'Consumer group {{ $labels.consumergroup}} lag is too big ({{ $value }}) on topic {{ $labels.topic }}/partition {{ $labels.partition }}'
        - alert: NoMessageForTooLong
          expr: changes(kafka_topic_partition_current_offset[10m]) == 0
          for: 10s
          labels:
            severity: warning
          annotations:
            summary: 'No message for 10 minutes'
            description: 'There is no messages in topic {{ $labels.topic}}/partition {{ $labels.partition }} for 10 minutes'
      - name: certificates
        interval: 1m0s
        rules:
        - alert: CertificateExpiration
          expr: |
            strimzi_certificate_expiration_timestamp_ms/1000 - time() < 30 * 24 * 60 * 60
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: 'Certificate will expire in less than 30 days'
            description: 'Certificate of type {{ $labels.type }} in cluster {{ $labels.cluster }} in namespace {{ $labels.resource_namespace }} will expire in less than 30 days'    


```       

3. Instale o Prometheus e o Grafana usando o Helm Chart:
   ```bash
   helm install prometheus prometheus-community/kube-prometheus-stack --namespace monitoring --create-namespace --values custom-values.yaml
   ```  
4. Verifique se os pods do Prometheus e Grafana estão em execução:
   ```bash
   kubectl get pods -n monitoring
   ```

## Habilitar métricas JMX no Kafka do Strimzi

Para habilitar as métricas JMX no Kafka do Strimzi, você precisa modificar a configuração do Kafka para expor as métricas. Isso pode ser feito adicionando as seguintes configurações ao seu recurso Kafka:

###  ConfigMap com a config do JMX Exporter

```yaml
## ConfigMap com a configuração do JMX Exporter para Strimzi
## https://github.com/strimzi/strimzi-kafka-operator/blob/main/examples/metrics/kube-state-metrics/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-jmx-config
  namespace: kafka
data:
  config.yaml: |
    spec:
      resources:
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaTopic
          metricNamePrefix: strimzi_kafka_topic
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka topic resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                ready: [ status, conditions, "[type=Ready]", status ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                partitions: [ spec, partitions ]
                replicas: [ spec, replicas ]
                generation: [ status, observedGeneration ]
                topicId: [ status, topicId ]
                topicName: [ status, topicName ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaUser
          metricNamePrefix: strimzi_kafka_user
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka user resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                ready: [ status, conditions, "[type=Ready]", status ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                secret: [ status, secret ]
                generation: [ status, observedGeneration ]
                username: [ status, username ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: Kafka
          metricNamePrefix: strimzi_kafka
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                ready: [ status, conditions, "[type=Ready]", status ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                kafka_version: [ status, kafkaVersion ]
                kafka_metadata_state: [ status, kafkaMetadataState ]
                kafka_metadata_version: [ status, kafkaMetadataVersion ]
                cluster_id: [ status, clusterId ]
                operator_last_successful_version: [ status, operatorLastSuccessfulVersion ]
                generation: [ status, observedGeneration ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaNodePool
          metricNamePrefix: strimzi_kafka_node_pool
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka node pool resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                # KafkaNodePool is not having a ready status as this is implemented via Kafka resource
                # ready: [ status, conditions, "[type=Ready]", status ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                node_ids: [ status, nodeIds ]
                roles: [ status, roles ]
                replicas: [ status, replicas ]
                cluster_id: [ status, clusterId ]
                generation: [ status, observedGeneration ]
        - groupVersionKind:
            group: core.strimzi.io
            version: v1beta2
            kind: StrimziPodSet
          metricNamePrefix: strimzi_pod_set
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi pod set resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                currentPods: [ status, currentPods ]
                pods: [ status, pods ]
                readyPods: [ status, readyPods ]
                generation: [ status, observedGeneration ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaRebalance
          metricNamePrefix: strimzi_kafka_rebalance
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi kafka rebalance resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                ready: [ status, conditions, "[type=Ready]", status ]
                proposal_ready: [ status, conditions, "[type=ProposalReady]", status ]
                rebalancing: [ status, conditions, "[type=Rebalancing]", status ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                template: [ metadata, annotations, "strimzi.io/rebalance-template" ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaConnect
          metricNamePrefix: strimzi_kafka_connect
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka Connect resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                ready: [ status, conditions, "[type=Ready]", status ]
                generation: [ status, observedGeneration ]
                connectorPluginsClass: [ status, connectorPlugins, class ]
                connectorPluginsType: [ status, connectorPlugins, type ]
                connectorPluginsVersion: [ status, connectorPlugins, version ]
                replicas: [ status, replicas ]
                labelSelector: [ status, labelSelector ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaConnector
          metricNamePrefix: strimzi_kafka_connector
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka Connector resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                ready: [ status, conditions, "[type=Ready]", status ]
                generation: [ status, observedGeneration ]
                autoRestartCount: [ status, autoRestart, count ]
                autoRestartConnectorName: [ status, autoRestart, connectorName ]
                tasksMax: [ status, tasksMax ]
                topics: [ status, topics ]
        - groupVersionKind:
            group: kafka.strimzi.io
            version: v1beta2
            kind: KafkaMirrorMaker2
          metricNamePrefix: strimzi_kafka_mm2
          metrics:
            - name: resource_info
              help: "The current state of a Strimzi Kafka MirrorMaker2 resource."
              each:
                type: Info
                info:
                  labelsFromPath:
                    name: [ metadata, name ]
              labelsFromPath:
                exported_namespace: [ metadata, namespace ]
                deprecated: [ status, conditions, "[reason=DeprecatedFields]", type ]
                ready: [ status, conditions, "[type=Ready]", status ]
                generation: [ status, observedGeneration ]
                autoRestartCount: [ status, autoRestartStatuses, count ]
                autoRestartConnectorName: [ status, autoRestartStatuses, connectorName ]
                connectorPluginsClass: [ status, connectorPlugins, class ]
                connectorPluginsType: [ status, connectorPlugins, type ]
                connectorPluginsVersion: [ status, connectorPlugins, version ]
                labelSelector: [ status, labelSelector ]
                replicas: [ status, replicas ]
```
```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-cluster
  namespace: kafka
  labels:
    strimzi.io/cluster: kafka-cluster
    strimzi.io/kind: Kafka  
  annotations:
    strimzi.io/node-pools: enabled
    strimzi.io/kraft: enabled
spec:
  kafka:
    version: 4.0.0
    metadataVersion: 4.0-IV3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
  entityOperator:
    topicOperator: {}
    userOperator: {}
  kafkaExporter:
    topicRegex: ".*"
    groupRegex: ".*"
```

## Deploy do Strimzi Kafka com métricas habilitadas
Adicione a seção `metrics` na especificação do Kafka para habilitar as métricas JMX. Aqui está um exemplo de configuração do recurso Kafka com métricas habilitadas:


```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaNodePool
metadata:
  name: controller
  labels:
    strimzi.io/cluster: kafka-cluster
    strimzi.io/kind: KafkaNodePool
    strimzi.io/name: kafka-cluster-controller
  namespace: kafka
spec:
  replicas: 3
  roles:
    - controller
  storage:
    type: jbod
    volumes:
      - id: 0
        type: ephemeral
         kraftMetadata: shared
     - id: 1
        type: ephemeral
         kraftMetadata: shared
--- 
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaNodePool
metadata:
  name: broker
  labels:
    strimzi.io/cluster: kafka-cluster
    strimzi.io/kind: KafkaNodePool
    strimzi.io/name: kafka-cluster-broker
  namespace: kafka
spec:
  replicas: 3
  roles:
    - broker
  storage:
    type: jbod
    volumes:
      - id: 0
        type: ephemeral
         kraftMetadata: shared
     - id: 1
        type: ephemeral
         kraftMetadata: shared

---
## ConfigMap com a configuração do JMX Prometheus Exporter para Kafka
kind: ConfigMap
apiVersion: v1
metadata:
  name: kafka-metrics
  labels:
    app: strimzi
data:
  kafka-metrics-config.yml: |
    # See https://github.com/prometheus/jmx_exporter for more info about JMX Prometheus Exporter metrics
    lowercaseOutputName: true
    rules:
    # Special cases and very specific rules
    - pattern: kafka.server<type=(.+), name=(.+), clientId=(.+), topic=(.+), partition=(.*)><>Value
      name: kafka_server_$1_$2
      type: GAUGE
      labels:
        clientId: "$3"
        topic: "$4"
        partition: "$5"
    - pattern: kafka.server<type=(.+), name=(.+), clientId=(.+), brokerHost=(.+), brokerPort=(.+)><>Value
      name: kafka_server_$1_$2
      type: GAUGE
      labels:
        clientId: "$3"
        broker: "$4:$5"
    - pattern: kafka.server<type=(.+), cipher=(.+), protocol=(.+), listener=(.+), networkProcessor=(.+)><>connections
      name: kafka_server_$1_connections_tls_info
      type: GAUGE
      labels:
        cipher: "$2"
        protocol: "$3"
        listener: "$4"
        networkProcessor: "$5"
    - pattern: kafka.server<type=(.+), clientSoftwareName=(.+), clientSoftwareVersion=(.+), listener=(.+), networkProcessor=(.+)><>connections
      name: kafka_server_$1_connections_software
      type: GAUGE
      labels:
        clientSoftwareName: "$2"
        clientSoftwareVersion: "$3"
        listener: "$4"
        networkProcessor: "$5"
    - pattern: "kafka.server<type=(.+), listener=(.+), networkProcessor=(.+)><>(.+-total):"
      name: kafka_server_$1_$4
      type: COUNTER
      labels:
        listener: "$2"
        networkProcessor: "$3"
    - pattern: "kafka.server<type=(.+), listener=(.+), networkProcessor=(.+)><>(.+):"
      name: kafka_server_$1_$4
      type: GAUGE
      labels:
        listener: "$2"
        networkProcessor: "$3"
    - pattern: kafka.server<type=(.+), listener=(.+), networkProcessor=(.+)><>(.+-total)
      name: kafka_server_$1_$4
      type: COUNTER
      labels:
        listener: "$2"
        networkProcessor: "$3"
    - pattern: kafka.server<type=(.+), listener=(.+), networkProcessor=(.+)><>(.+)
      name: kafka_server_$1_$4
      type: GAUGE
      labels:
        listener: "$2"
        networkProcessor: "$3"
    # Some percent metrics use MeanRate attribute
    # Ex) kafka.server<type=(KafkaRequestHandlerPool), name=(RequestHandlerAvgIdlePercent)><>MeanRate
    - pattern: kafka.(\w+)<type=(.+), name=(.+)Percent\w*><>MeanRate
      name: kafka_$1_$2_$3_percent
      type: GAUGE
    # Generic gauges for percents
    - pattern: kafka.(\w+)<type=(.+), name=(.+)Percent\w*><>Value
      name: kafka_$1_$2_$3_percent
      type: GAUGE
    - pattern: kafka.(\w+)<type=(.+), name=(.+)Percent\w*, (.+)=(.+)><>Value
      name: kafka_$1_$2_$3_percent
      type: GAUGE
      labels:
        "$4": "$5"
    # Generic per-second counters with 0-2 key/value pairs
    - pattern: kafka.(\w+)<type=(.+), name=(.+)PerSec\w*, (.+)=(.+), (.+)=(.+)><>Count
      name: kafka_$1_$2_$3_total
      type: COUNTER
      labels:
        "$4": "$5"
        "$6": "$7"
    - pattern: kafka.(\w+)<type=(.+), name=(.+)PerSec\w*, (.+)=(.+)><>Count
      name: kafka_$1_$2_$3_total
      type: COUNTER
      labels:
        "$4": "$5"
    - pattern: kafka.(\w+)<type=(.+), name=(.+)PerSec\w*><>Count
      name: kafka_$1_$2_$3_total
      type: COUNTER
    # Generic gauges with 0-2 key/value pairs
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+), (.+)=(.+)><>Value
      name: kafka_$1_$2_$3
      type: GAUGE
      labels:
        "$4": "$5"
        "$6": "$7"
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+)><>Value
      name: kafka_$1_$2_$3
      type: GAUGE
      labels:
        "$4": "$5"
    - pattern: kafka.(\w+)<type=(.+), name=(.+)><>Value
      name: kafka_$1_$2_$3
      type: GAUGE
    # Emulate Prometheus 'Summary' metrics for the exported 'Histogram's.
    # Note that these are missing the '_sum' metric!
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+), (.+)=(.+)><>Count
      name: kafka_$1_$2_$3_count
      type: COUNTER
      labels:
        "$4": "$5"
        "$6": "$7"
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.*), (.+)=(.+)><>(\d+)thPercentile
      name: kafka_$1_$2_$3
      type: GAUGE
      labels:
        "$4": "$5"
        "$6": "$7"
        quantile: "0.$8"
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+)><>Count
      name: kafka_$1_$2_$3_count
      type: COUNTER
      labels:
        "$4": "$5"
    - pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.*)><>(\d+)thPercentile
      name: kafka_$1_$2_$3
      type: GAUGE
      labels:
        "$4": "$5"
        quantile: "0.$6"
    - pattern: kafka.(\w+)<type=(.+), name=(.+)><>Count
      name: kafka_$1_$2_$3_count
      type: COUNTER
    - pattern: kafka.(\w+)<type=(.+), name=(.+)><>(\d+)thPercentile
      name: kafka_$1_$2_$3
      type: GAUGE
      labels:
        quantile: "0.$4"
    # KRaft overall related metrics
    # distinguish between always increasing COUNTER (total and max) and variable GAUGE (all others) metrics
    - pattern: "kafka.server<type=raft-metrics><>(.+-total|.+-max):"
      name: kafka_server_raftmetrics_$1
      type: COUNTER
    - pattern: "kafka.server<type=raft-metrics><>(current-state): (.+)"
      name: kafka_server_raftmetrics_$1
      value: 1
      type: UNTYPED
      labels:
        $1: "$2"
    - pattern: "kafka.server<type=raft-metrics><>(.+):"
      name: kafka_server_raftmetrics_$1
      type: GAUGE
    # KRaft "low level" channels related metrics
    # distinguish between always increasing COUNTER (total and max) and variable GAUGE (all others) metrics
    - pattern: "kafka.server<type=raft-channel-metrics><>(.+-total|.+-max):"
      name: kafka_server_raftchannelmetrics_$1
      type: COUNTER
    - pattern: "kafka.server<type=raft-channel-metrics><>(.+):"
      name: kafka_server_raftchannelmetrics_$1
      type: GAUGE
    # Broker metrics related to fetching metadata topic records in KRaft mode
    - pattern: "kafka.server<type=broker-metadata-metrics><>(.+):"
      name: kafka_server_brokermetadatametrics_$1
      type: GAUGE
```

## Configuração do Prometheus para coletar métricas do Kubernetes (cAdvisor e Kubelet)
 
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: additional-scrape-configs
type: Opaque
stringData:
  prometheus-additional.yaml: |
    - job_name: kubernetes-cadvisor
      honor_labels: true
      scrape_interval: 10s
      scrape_timeout: 10s
      metrics_path: /metrics/cadvisor
      scheme: https
      kubernetes_sd_configs:
      - role: node
        namespaces:
          names: []
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        insecure_skip_verify: true
      relabel_configs:
      - separator: ;
        regex: __meta_kubernetes_node_label_(.+)
        replacement: $1
        action: labelmap
      - separator: ;
        regex: (.*)
        target_label: __address__
        replacement: kubernetes.default.svc:443
        action: replace
      - source_labels: [__meta_kubernetes_node_name]
        separator: ;
        regex: (.+)
        target_label: __metrics_path__
        replacement: /api/v1/nodes/${1}/proxy/metrics/cadvisor
        action: replace
      - source_labels: [__meta_kubernetes_node_name]
        separator: ;
        regex: (.*)
        target_label: node_name
        replacement: $1
        action: replace
      - source_labels: [__meta_kubernetes_node_address_InternalIP]
        separator: ;
        regex: (.*)
        target_label: node_ip
        replacement: $1
        action: replace
      metric_relabel_configs:
      - source_labels: [container, __name__]
        separator: ;
        regex: POD;container_(network).*
        target_label: container
        replacement: $1
        action: replace
      - source_labels: [container]
        separator: ;
        regex: POD
        replacement: $1
        action: drop
      - source_labels: [container]
        separator: ;
        regex: ^$
        replacement: $1
        action: drop
      - source_labels: [__name__]
        separator: ;
        regex: container_(network_tcp_usage_total|tasks_state|memory_failures_total|network_udp_usage_total)
        replacement: $1
        action: drop

    - job_name: kubernetes-nodes-kubelet
      scrape_interval: 10s
      scrape_timeout: 10s
      scheme: https
      kubernetes_sd_configs:
      - role: node
        namespaces:
          names: []
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        insecure_skip_verify: true
      relabel_configs:
      - action: labelmap
        regex: __meta_kubernetes_node_label_(.+)
      - target_label: __address__
        replacement: kubernetes.default.svc:443
      - source_labels: [__meta_kubernetes_node_name]
        regex: (.+)
        target_label: __metrics_path__
        replacement: /api/v1/nodes/${1}/proxy/metrics
```
## Deploy Kafka-Bridge com métricas habilitadas

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaBridge
metadata:
  name: kafka-bridge
  namespace: kafka
    labels:
      strimzi.io/cluster: kafka-cluster   
      strimzi.io/kind: KafkaBridge
      strimzi.io/name: kafka-cluster-bridge 
  annotations:
    strimzi.io/uses-connector-resources: "true"
    strimzi.io/metrics: "true"
spec:
  replicas: 1
  bootstrapServers: kafka-cluster-kafka-bootstrap:9092
  http:
    port: 8080
  enableMetrics: true
```

### Adicionar dashboards do Grafana
Edite o values.yaml do Helm Chart do Prometheus e adicione os dashboards do Grafana:

```yaml
  grafana:
    additionalDataSources:
      - name: Prometheus
        type: prometheus
        access: proxy
        url: http://prometheus-operated.monitoring.svc.cluster.local
        isDefault: true
    operator:
           dashboardsConfigMapRefEnabled: true
           dashboardsConfigMaps:
             - strimzi-kafka-bridge
             - strimzi-kafka
             - strimzi-kraft
             - strimzi-operators
```
### Atualizar ConfigMap com dashboards do Grafana

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: strimzi-kafka-bridge
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  strimzi-kafka-bridge.json: |
  {
  "__requires": [
    {
      "type": "grafana",
      "id": "grafana",
      "name": "Grafana",
      "version": "7.4.5"
    },
    {
      "type": "panel",
      "id": "stat",
      "name": "Stat"
    },
    {
      "type": "datasource",
      "id": "prometheus",
      "name": "Prometheus"
    },
    {
      "type": "panel",
      "id": "timeseries",
      "name": "Timeseries"
    }
  ],
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": {
          "type": "datasource",
          "uid": "grafana"
        },
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 0,
  "id": 16,
  "links": [],
  "panels": [
    {
      "datasource": {
        "type": "datasource",
        "uid": "grafana"
      },
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 0
      },
      "id": 40,
      "targets": [
        {
          "datasource": {
            "type": "datasource",
            "uid": "grafana"
          },
          "refId": "A"
        }
      ],
      "title": "Overview",
      "type": "row"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "N/A"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 0,
        "y": 1
      },
      "id": 19,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(strimzi_bridge_http_server_active_connections{container=~\"^.+-bridge\"})",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "HTTP connections",
      "type": "stat"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "N/A"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 4,
        "y": 1
      },
      "id": 20,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(strimzi_bridge_http_server_active_requests{container=~\"^.+-bridge\"})",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "HTTP requests being processed",
      "type": "stat"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "0"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 8,
        "y": 1
      },
      "id": 43,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "count(strimzi_bridge_kafka_producer_count)",
          "format": "time_series",
          "instant": true,
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "KafkaProducer instances",
      "type": "stat"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "0"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 12,
        "y": 1
      },
      "id": 44,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(strimzi_bridge_kafka_producer_connection_count)",
          "format": "time_series",
          "instant": true,
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "KafkaProducers connections",
      "type": "stat"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "0"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 16,
        "y": 1
      },
      "id": 26,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(strimzi_bridge_kafka_consumer_last_heartbeat_seconds_ago != bool -1)",
          "format": "time_series",
          "instant": true,
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "KafkaConsumer instances",
      "type": "stat"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [
            {
              "options": {
                "match": "null",
                "result": {
                  "text": "0"
                }
              },
              "type": "special"
            }
          ],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "none"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 4,
        "x": 20,
        "y": 1
      },
      "id": 42,
      "maxDataPoints": 100,
      "options": {
        "colorMode": "none",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "horizontal",
        "reduceOptions": {
          "calcs": [
            "lastNotNull"
          ],
          "fields": "",
          "values": false
        },
        "showPercentChange": false,
        "textMode": "auto",
        "wideLayout": true
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(strimzi_bridge_kafka_consumer_connection_count)",
          "format": "time_series",
          "instant": true,
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "KafkaConsumers connections",
      "type": "stat"
    },
    {
      "collapsed": true,
      "datasource": {
        "type": "datasource",
        "uid": "grafana"
      },
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 5
      },
      "id": 38,
      "panels": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "records/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 6
          },
          "id": 22,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(rate(strimzi_bridge_kafka_producer_record_send_total{topic != \"\"}[5m])) by (clientId, topic)",
              "format": "time_series",
              "intervalFactor": 1,
              "refId": "B"
            }
          ],
          "title": "Producer rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "bytes/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 12,
            "y": 6
          },
          "id": 24,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(rate(strimzi_bridge_kafka_producer_byte_total{topic != \"\"}[5m])) by (clientId, topic)",
              "format": "time_series",
              "intervalFactor": 1,
              "refId": "B"
            }
          ],
          "title": "Producer rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "ms",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 14
          },
          "id": 45,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_producer_request_latency_avg{type = \"producer-metrics\"}) by (clientId)",
              "format": "time_series",
              "intervalFactor": 1,
              "refId": "A"
            }
          ],
          "title": "Average producer request latency",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "bytes/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 12,
            "y": 14
          },
          "id": 25,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(rate(strimzi_bridge_kafka_consumer_bytes_consumed_total{topic != \"\"}[5m])) by (clientId, topic)",
              "format": "time_series",
              "intervalFactor": 1,
              "refId": "B"
            }
          ],
          "title": "Consumer rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "records/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 22
          },
          "id": 23,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(rate(strimzi_bridge_kafka_consumer_records_consumed_total{topic != \"\"}[5m])) by (clientId, topic)",
              "format": "time_series",
              "hide": false,
              "intervalFactor": 1,
              "refId": "B"
            }
          ],
          "title": "Consumer rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "partitions",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 12,
            "y": 22
          },
          "id": 28,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_consumer_assigned_partitions) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Consumer Assigned Partitions",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 30
          },
          "id": 46,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_consumer_poll_idle_ratio_avg) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Average consumer idle poll",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "commits/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 12,
            "y": 30
          },
          "id": 41,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(rate(strimzi_bridge_kafka_consumer_commit_total[5m])) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Consumer commit rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "ms",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 38
          },
          "id": 47,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_consumer_commit_latency_avg) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Consumer commits latency",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "requests/sec",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 12,
            "y": 38
          },
          "id": 48,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_consumer_fetch_rate) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Consumer fetch rate",
          "type": "timeseries"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "description": "",
          "fieldConfig": {
            "defaults": {
              "color": {
                "mode": "palette-classic"
              },
              "custom": {
                "axisBorderShow": false,
                "axisCenteredZero": false,
                "axisColorMode": "text",
                "axisLabel": "ms",
                "axisPlacement": "auto",
                "barAlignment": 0,
                "drawStyle": "line",
                "fillOpacity": 10,
                "gradientMode": "none",
                "hideFrom": {
                  "legend": false,
                  "tooltip": false,
                  "viz": false
                },
                "insertNulls": false,
                "lineInterpolation": "linear",
                "lineWidth": 1,
                "pointSize": 5,
                "scaleDistribution": {
                  "type": "linear"
                },
                "showPoints": "never",
                "spanNulls": false,
                "stacking": {
                  "group": "A",
                  "mode": "none"
                },
                "thresholdsStyle": {
                  "mode": "off"
                }
              },
              "links": [],
              "mappings": [],
              "thresholds": {
                "mode": "absolute",
                "steps": [
                  {
                    "color": "green",
                    "value": null
                  },
                  {
                    "color": "red",
                    "value": 80
                  }
                ]
              },
              "unit": "short"
            },
            "overrides": []
          },
          "gridPos": {
            "h": 8,
            "w": 12,
            "x": 0,
            "y": 46
          },
          "id": 49,
          "options": {
            "legend": {
              "calcs": [],
              "displayMode": "list",
              "placement": "bottom",
              "showLegend": true
            },
            "tooltip": {
              "mode": "multi",
              "sort": "none"
            }
          },
          "pluginVersion": "7.4.5",
          "targets": [
            {
              "datasource": "${DS_PROMETHEUS}",
              "expr": "sum(strimzi_bridge_kafka_consumer_fetch_latency_avg) by (clientId)",
              "format": "time_series",
              "interval": "",
              "intervalFactor": 1,
              "legendFormat": "",
              "refId": "A"
            }
          ],
          "title": "Average consumer fetch latency",
          "type": "timeseries"
        }
      ],
      "targets": [
        {
          "datasource": {
            "type": "datasource",
            "uid": "grafana"
          },
          "refId": "A"
        }
      ],
      "title": "Kafka",
      "type": "row"
    },
    {
      "datasource": {
        "type": "datasource",
        "uid": "grafana"
      },
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 6
      },
      "id": 36,
      "targets": [
        {
          "datasource": {
            "type": "datasource",
            "uid": "grafana"
          },
          "refId": "A"
        }
      ],
      "title": "HTTP",
      "type": "row"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "bars",
            "fillOpacity": 100,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 7
      },
      "id": 15,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\"}[5m])) by (method)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Requests rate by HTTP method",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 7
      },
      "id": 17,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "Requests",
          "refId": "A"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{code=~\"^2..$\", container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "2XX",
          "refId": "B"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{code=~\"^4..$\", container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "4XX",
          "refId": "C"
        },
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{code=~\"^5..$\", container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "5XX",
          "refId": "D"
        }
      ],
      "title": "HTTP Requests and Response rate by codes",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "bytes/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 15
      },
      "id": 13,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_bytes_written_total{container=~\"^.+-bridge\"}[5m])) by (container)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "HTTP send rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "bytes/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 15
      },
      "id": 12,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_bytes_read_total{container=~\"^.+-bridge\"}[5m])) by (container)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "HTTP receive rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 23
      },
      "id": 2,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"POST\",path=~\"/topics/[^/]+\"}[5m])) by (path)",
          "format": "time_series",
          "intervalFactor": 1,
          "refId": "B"
        }
      ],
      "title": "Send messages rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 23
      },
      "id": 3,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"POST\",path=~\"/topics/.+/partitions/[0-9]+\"}[5m])) by (path)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Send messages To Partition rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 0,
        "y": 31
      },
      "id": 4,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"POST\",path=~\"/consumers/[^/]+\"}[5m])) by (path)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Consumer instance creation rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 8,
        "y": 31
      },
      "id": 7,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"POST\",path=~\"/consumers/.+/instances/.+/subscription\"}[5m])) by (path)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Subscribe to topics rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 16,
        "y": 31
      },
      "id": 9,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"DELETE\",path=~\"/consumers/.+/instances/.+\"}[5m])) by (path)",
          "format": "time_series",
          "instant": false,
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Consumer instance deletion rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 39
      },
      "id": 8,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"GET\",path=~\"/consumers/.+/instances/.+/records\"}[5m])) by (path)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Poll for messages rate",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "requests/sec",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "links": [],
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 39
      },
      "id": 10,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(strimzi_bridge_http_server_requests_total{container=~\"^.+-bridge\",method=\"POST\",path=~\"/consumers/.+/instances/.+/offsets\"}[5m])) by (path)",
          "format": "time_series",
          "interval": "",
          "intervalFactor": 1,
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "Commit offsets rate",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "datasource",
        "uid": "grafana"
      },
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 47
      },
      "id": 34,
      "targets": [
        {
          "datasource": {
            "type": "datasource",
            "uid": "grafana"
          },
          "refId": "A"
        }
      ],
      "title": "JVM",
      "type": "row"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 0,
        "y": 48
      },
      "id": 30,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(jvm_memory_used_bytes{container=~\"^.+-bridge\"})",
          "format": "time_series",
          "intervalFactor": 1,
          "refId": "A"
        }
      ],
      "title": "JVM Memory Used",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 8,
        "y": 48
      },
      "id": 31,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(jvm_gc_pause_seconds_sum{container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "intervalFactor": 1,
          "refId": "A"
        }
      ],
      "title": "JVM GC Time",
      "type": "timeseries"
    },
    {
      "datasource": "${DS_PROMETHEUS}",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 80
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 16,
        "y": 48
      },
      "id": 32,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      },
      "pluginVersion": "7.4.5",
      "targets": [
        {
          "datasource": "${DS_PROMETHEUS}",
          "expr": "sum(rate(jvm_gc_pause_seconds_count{container=~\"^.+-bridge\"}[5m]))",
          "format": "time_series",
          "intervalFactor": 1,
          "refId": "A"
        }
      ],
      "title": "JVM GC Count",
      "type": "timeseries"
    }
  ],
  "refresh": "5s",
  "schemaVersion": 39,
  "tags": [
    "Strimzi",
    "Kafka",
    "Kafka Bridge"
  ],
  "templating": {
    "list": [
      {
        "current": {},
        "error": null,
        "hide": 0,
        "includeAll": false,
        "label": "datasource",
        "multi": false,
        "name": "DS_PROMETHEUS",
        "options": [],
        "query": "prometheus",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "type": "datasource"
      },
      {
        "current": {},
        "datasource": "${DS_PROMETHEUS}",
        "definition": "",
        "hide": 0,
        "includeAll": false,
        "label": "Namespace",
        "multi": false,
        "name": "kubernetes_namespace",
        "options": [],
        "query": "query_result(strimzi_bridge_http_server_active_connections)",
        "refresh": 1,
        "regex": "/.*namespace=\"([^\"]*).*/",
        "skipUrlSync": false,
        "sort": 0,
        "tagValuesQuery": "",
        "tagsQuery": "",
        "type": "query",
        "useTags": false
      },
      {
        "current": {},
        "datasource": "${DS_PROMETHEUS}",
        "definition": "",
        "hide": 0,
        "includeAll": false,
        "label": "Container Name",
        "multi": false,
        "name": "bridge_container_name",
        "options": [],
        "query": "query_result(strimzi_bridge_http_server_active_connections{namespace=\"$kubernetes_namespace\"})",
        "refresh": 1,
        "regex": "/.*container=\"([^\"]*).*/",
        "skipUrlSync": false,
        "sort": 0,
        "tagValuesQuery": "",
        "tagsQuery": "",
        "type": "query",
        "useTags": false
      }
    ]
  },
  "time": {
    "from": "now-5m",
    "to": "now"
  },
  "timepicker": {
    "refresh_intervals": [
      "10s",
      "30s",
      "1m",
      "5m",
      "15m",
      "30m",
      "1h",
      "2h",
      "1d"
    ],
    "time_options": [
      "5m",
      "15m",
      "1h",
      "6h",
      "12h",
      "24h",
      "2d",
      "7d",
      "30d"
    ]
  },
  "timezone": "",
  "title": "Strimzi Kafka Bridge",
  "version": 6,
  "weekStart": ""
  }
```
```yaml
# Dashboard do Grafana para Strimzi Kafka Bridge
apiVersion: grafana.integreatly.org/v1beta1
kind: GrafanaDashboard
metadata:
  name: strimzi-kafka-bridge-dashboard
  namespace: monitoring
  labels:
    app: grafana
    grafana_dashboard: "1"
spec:
  instanceSelector:
    matchLabels:
      app: grafana
  configMap:
    name: strimzi-kafka-bridge-dashboard
    key: strimzi-kafka-bridge-dashboard.json
```

### Deploy o dashboard do Grafana
```bash
kubectl apply -f strimzi-kafka-bridge-dashboard-configmap.yaml
kubectl apply -f strimzi-kafka-bridge-dashboard.yaml
```
### Atualize helm do Grafana
```bash
helm upgrade grafana grafana/grafana -n monitoring -f values-grafana.yaml
```


### Referências
- [Strimzi Monitoring](https://strimzi.io/docs/operators/latest/deploying.html#deploying-monitoring-deployment-str)
- [Strimzi Metrics](https://strimzi.io/docs/operators/latest/using.html#converting-metrics-str)
- [Prometheus - Community Helm Chart](https://github.com/prometheus-community/helm-charts/tree/main)
  
