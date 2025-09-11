
[conduktor/kafka-stack-docker-compose](https://github.com/conduktor/kafka-stack-docker-compose/blob/master/zk-single-kafka-single.yml)

https://redhat-developer-demos.github.io/kafka-tutorial/kafka-tutorial/1.0.x/07-kubernetes.html#kubernetes

https://strimzi.io/docs/operators/latest/overview
```
$kubectl exec -it kafka-deployment-xxxxxxxxxxx -- bash
[appuser@kafka-deployment-xxxxxxxxxxxxxx ~]$ 
kafka-topics --bootstrap-server localhost:29092 --create --topic topic-one --replication-factor 1 --partitions 3
Created topic topic-one.

[appuser@kafka-deployment-xxxxxxxxxxxxxx ~]$ kafka-topics --bootstrap-server localhost:29092 --list
topic-one





# Terminal 1: Produtor
  kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic topic-one
>test1
>test2
>test3
>^C

# Terminal 2: Consumidor
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic topic-one \
  --from-beginning

Com grupos 

  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic topic-one \
  --from-beginning --group group1

  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic topic-one \ 
  --from-beginning --group group1

    kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic topic-one \ 
  --from-beginning --group group1

 kafka-topics --bootstrap-server localhost:9092 --describe --topic topic-one
 kafka-topics --bootstrap-server localhost:9092 --delete --topic topic-one


  kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group group1
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
  kafka-consumer-groups --bootstrap-server localhost:9092 --delete --group group1