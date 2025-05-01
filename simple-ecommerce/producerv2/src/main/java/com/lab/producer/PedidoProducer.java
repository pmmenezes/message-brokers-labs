package com.lab.producer;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PedidoProducer {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "lab.pedidos";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {
        try {
            int option = showMenu();
            
            switch (option) {
                case 1:
                    enviarPedidoManual();
                    break;
                case 2:
                    enviarPedidosArquivo();
                    break;
                case 3:
                    enviarPedidosMultiplos();
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
        System.out.println("=== PRODUTOR DE PEDIDOS ===");
        System.out.println("1. Enviar pedido único (entrada manual)");
        System.out.println("2. Enviar pedidos de arquivo JSON");
        System.out.println("3. Enviar múltiplos pedidos (entrada manual)");
        System.out.print("Escolha uma opção: ");
        
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void enviarPedidoManual() throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== NOVO PEDIDO ===");
        System.out.print("ID do Pedido: ");
        String id = scanner.nextLine();
        
        System.out.print("Nome do Cliente: ");
        String cliente = scanner.nextLine();
        
        System.out.print("Produto: ");
        String produto = scanner.nextLine();
        
        System.out.print("Valor: ");
        double valor = Double.parseDouble(scanner.nextLine());
        
        String mensagemPedido = String.format(
            "{\n" +
            "  \"id\": \"%s\",\n" +
            "  \"cliente\": \"%s\",\n" +
            "  \"produto\": \"%s\",\n" +
            "  \"valor\": %.2f\n" +
            "}", id, cliente, produto, valor);
        
        enviarMensagemParaFila(mensagemPedido);
        System.out.println("Pedido enviado com sucesso!");
    }
    
    private static void enviarPedidosArquivo() throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("\nInforme o caminho do arquivo JSON: ");
        String filePath = scanner.nextLine();
        
        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("Arquivo não encontrado: " + filePath);
            return;
        }
        
        String conteudo = new String(Files.readAllBytes(Paths.get(filePath)));
        
        // Verifica se é um array JSON ou um objeto JSON único
        if (conteudo.trim().startsWith("[")) {
            // Array de pedidos
            JSONParser parser = new JSONParser();
            JSONArray pedidosArray = (JSONArray) parser.parse(conteudo);
            
            int contador = 0;
            for (Object obj : pedidosArray) {
                String pedidoJson = obj.toString();
                enviarMensagemParaFila(pedidoJson);
                contador++;
            }
            
            System.out.println(contador + " pedidos enviados com sucesso!");
        } else {
            // Pedido único
            enviarMensagemParaFila(conteudo);
            System.out.println("Pedido enviado com sucesso!");
        }
    }
    
    private static void enviarPedidosMultiplos() throws Exception {
        Scanner scanner = new Scanner(System.in);
        List<String> pedidos = new ArrayList<>();
        
        System.out.println("\n=== MÚLTIPLOS PEDIDOS ===");
        System.out.println("Digite os detalhes de cada pedido. Para finalizar, deixe o ID em branco.");
        
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n--- Novo Pedido ---");
            System.out.print("ID do Pedido (vazio para sair): ");
            String id = scanner.nextLine();
            
            if (id.trim().isEmpty()) {
                continuar = false;
                continue;
            }
            
            System.out.print("Nome do Cliente: ");
            String cliente = scanner.nextLine();
            
            System.out.print("Produto: ");
            String produto = scanner.nextLine();
            
            System.out.print("Valor: ");
            double valor = Double.parseDouble(scanner.nextLine());
            
            String mensagemPedido = String.format(
                "{\n" +
                "  \"id\": \"%s\",\n" +
                "  \"cliente\": \"%s\",\n" +
                "  \"produto\": \"%s\",\n" +
                "  \"valor\": %.2f\n" +
                "}", id, cliente, produto, valor);
            
            pedidos.add(mensagemPedido);
        }
        
        if (pedidos.isEmpty()) {
            System.out.println("Nenhum pedido para enviar.");
            return;
        }
        
        int contador = 0;
        for (String pedido : pedidos) {
            enviarMensagemParaFila(pedido);
            contador++;
        }
        
        System.out.println(contador + " pedidos enviados com sucesso!");
    }
    
    private static void enviarMensagemParaFila(String mensagem) throws JMSException {
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            MessageProducer producer = session.createProducer(queue);
            
            TextMessage message = session.createTextMessage(mensagem);
            producer.send(message);
            
            System.out.println("Mensagem enviada: " + mensagem);
            
            connection.close();
        }
    }
}