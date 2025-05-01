package com.lab.producer;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class PedidoProducer {

    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:61616";
        String queueName = "lab.pedidos";

        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl)) {
            Connection connection = connectionFactory.createConnection("admin", "admin");
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);

            String mensagemPedido = """
                {
                  "id": "PED123",
                  "cliente": "João Silva",
                  "produto": "Notebook",
                  "valor": 3500.00
                }
                """;

            TextMessage message = session.createTextMessage(mensagemPedido);
            producer.send(message);

            System.out.println("Pedido enviado para a fila: " + mensagemPedido);

            connection.close();
        } catch (JMSException e) {
            System.err.println("Erro ao enviar pedido: " + e.getMessage());
        }
    }
}