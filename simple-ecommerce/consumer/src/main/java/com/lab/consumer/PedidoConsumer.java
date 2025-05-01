package com.lab.consumer;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PedidoConsumer {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "lab.pedidos";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final AtomicInteger processedCount = new AtomicInteger(0);
    private static volatile boolean running = true;
    private static CountDownLatch shutdownLatch;

    public static void main(String[] args) {
        try {
            int option = showMenu();
            
            switch (option) {
                case 1:
                    consumirPedidosAssincrono();
                    break;
                case 2:
                    consumirPedidosSincrono();
                    break;
                case 3:
                    consumirPedidosPorTempo();
                    break;
                default:
                    System.out.println("Opção inválida. Saindo.");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erro na aplicação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static int showMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== CONSUMIDOR DE PEDIDOS ===");
        System.out.println("1. Consumir pedidos continuamente (assíncrono)");
        System.out.println("2. Consumir pedidos um a um (síncrono)");
        System.out.println("3. Consumir pedidos por tempo limitado");
        System.out.print("Escolha uma opção: ");
        
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void consumirPedidosAssincrono() throws Exception {
        shutdownLatch = new CountDownLatch(1);
        System.out.println("\nIniciando consumo assíncrono de pedidos...");
        System.out.println("Pressione ENTER a qualquer momento para finalizar.");
        
        // Thread para monitorar entrada do usuário para encerrar
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine(); // Aguarda ENTER
            running = false;
            System.out.println("Finalizando consumo de pedidos...");
            shutdownLatch.countDown();
        }).start();
        
        // Configuração de conexão e consumer
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            MessageConsumer consumer = session.createConsumer(queue);
            
            // Registra um listener assíncrono
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String json = ((TextMessage) message).getText();
                        processarPedido(json);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar mensagem: " + e.getMessage());
                }
            });
            
            System.out.println("Consumer registrado. Aguardando pedidos...");
            
            // Aguarda sinal para encerrar
            shutdownLatch.await();
            
            // Fechamento organizado
            System.out.println("Total de pedidos processados: " + processedCount.get());
            consumer.close();
            session.close();
            connection.close();
        }
    }
    
    private static void consumirPedidosSincrono() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nConsumidor pronto. Pressione ENTER para consumir próximo pedido ou digite 'sair' para encerrar.");
        
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            MessageConsumer consumer = session.createConsumer(queue);
            
            String input = "";
            while (!input.equalsIgnoreCase("sair")) {
                System.out.print("Pressione ENTER para consumir próximo pedido: ");
                input = scanner.nextLine();
                
                if (input.equalsIgnoreCase("sair")) {
                    break;
                }
                
                // Espera por até 5 segundos pela próxima mensagem
                Message message = consumer.receive(5000);
                
                if (message != null && message instanceof TextMessage) {
                    String json = ((TextMessage) message).getText();
                    processarPedido(json);
                } else {
                    System.out.println("Nenhum pedido disponível na fila.");
                }
            }
            
            System.out.println("Total de pedidos processados: " + processedCount.get());
            consumer.close();
            session.close();
            connection.close();
        }
    }
    
    private static void consumirPedidosPorTempo() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nInforme por quantos segundos deseja consumir pedidos: ");
        int segundos = 30; // Valor padrão
        
        try {
            segundos = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido. Usando 30 segundos como padrão.");
        }
        
        final int tempoFinal = segundos;
        shutdownLatch = new CountDownLatch(1);
        
        System.out.println("Consumindo pedidos por " + segundos + " segundos...");
        
        // Thread para controlar o tempo
        new Thread(() -> {
            try {
                Thread.sleep(tempoFinal * 1000);
                running = false;
                shutdownLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Configuração de conexão e consumer
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            MessageConsumer consumer = session.createConsumer(queue);
            
            // Registra um listener assíncrono
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String json = ((TextMessage) message).getText();
                        processarPedido(json);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar mensagem: " + e.getMessage());
                }
            });
            
            System.out.println("Consumer registrado. Aguardando pedidos...");
            
            // Aguarda tempo estipulado
            boolean completed = shutdownLatch.await(tempoFinal + 1, TimeUnit.SECONDS);
            
            // Fechamento organizado
            System.out.println("\nTempo finalizado!");
            System.out.println("Total de pedidos processados: " + processedCount.get());
            consumer.close();
            session.close();
            connection.close();
        }
    }
    
    private static void processarPedido(String json) {
        try {
            String timestamp = dateFormat.format(new Date());
            JSONParser parser = new JSONParser();
            JSONObject pedido = (JSONObject) parser.parse(json);
            
            String id = (String) pedido.get("id");
            String cliente = (String) pedido.get("cliente");
            String produto = (String) pedido.get("produto");
            double valor = 0;
            
            // Trata valor como Double ou Long
            Object valorObj = pedido.get("valor");
            if (valorObj instanceof Double) {
                valor = (Double) valorObj;
            } else if (valorObj instanceof Long) {
                valor = ((Long) valorObj).doubleValue();
            }
            
            int currentCount = processedCount.incrementAndGet();
            
            System.out.println("\n=== PEDIDO RECEBIDO [" + currentCount + "] ===");
            System.out.println("Timestamp: " + timestamp);
            System.out.println("ID: " + id);
            System.out.println("Cliente: " + cliente);
            System.out.println("Produto: " + produto);
            System.out.println("Valor: R$ " + String.format("%.2f", valor));
            System.out.println("Status: Processado com sucesso");
            System.out.println("===============================");
            
            // Simula algum processamento
            Thread.sleep(500);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar pedido JSON: " + e.getMessage());
            System.err.println("JSON recebido: " + json);
        }
    }
}