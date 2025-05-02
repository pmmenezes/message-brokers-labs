package com.lab.publisher;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class NotificacaoPublisher {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    
    // Tópicos para diferentes áreas da empresa
    private static final Map<Integer, String> TOPIC_AREAS = new HashMap<>();
    static {
        TOPIC_AREAS.put(1, "lab.notificacoes.financeiro");
        TOPIC_AREAS.put(2, "lab.notificacoes.logistica");
        TOPIC_AREAS.put(3, "lab.notificacoes.comercial");
        TOPIC_AREAS.put(4, "lab.notificacoes.todos");  // Tópico para todas as áreas
        TOPIC_AREAS.put(5, "lab.notificacoes.arquivo");  // Nova opção para ler do arquivo
    }

    public static void main(String[] args) {
        try {
            int option = showMenu();
            
            if (option == 5) {
                // Nova opção para ler notificações de um arquivo JSON
                enviarNotificacoesArquivo();
            } else if (option > 0 && option <= 4) {
                String topicName = TOPIC_AREAS.get(option);
                enviarNotificacao(topicName);
            } else {
                System.out.println("Opção inválida. Saindo.");
            }
        } catch (Exception e) {
            System.err.println("Erro na aplicação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static int showMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== PUBLICADOR DE NOTIFICAÇÕES ===");
        System.out.println("Selecione a área de destino:");
        System.out.println("1. Financeiro");
        System.out.println("2. Logística");
        System.out.println("3. Comercial");
        System.out.println("4. Todas as Áreas");
        System.out.println("5. Ler notificações de arquivo JSON");
        System.out.print("Escolha uma opção: ");
        
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void enviarNotificacao(String topicName) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== NOVA NOTIFICAÇÃO ===");
        System.out.print("Assunto: ");
        String assunto = scanner.nextLine();
        
        System.out.print("Mensagem: ");
        String corpo = scanner.nextLine();
        
        System.out.print("Prioridade (ALTA, MEDIA, BAIXA): ");
        String prioridade = scanner.nextLine().toUpperCase();
        
        String notificacaoJson = String.format(
            "{\n" +
            "  \"timestamp\": %d,\n" +
            "  \"assunto\": \"%s\",\n" +
            "  \"mensagem\": \"%s\",\n" +
            "  \"prioridade\": \"%s\",\n" +
            "  \"topico\": \"%s\"\n" +
            "}", System.currentTimeMillis(), assunto, corpo, prioridade, topicName);
        
        publicarNotificacao(topicName, notificacaoJson, prioridade);
        System.out.println("Notificação publicada com sucesso!");
    }
    
    private static void enviarNotificacoesArquivo() throws Exception {
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
            // Array de notificações
            try (JsonReader jsonReader = Json.createReader(new StringReader(conteudo))) {
                JsonArray notificacoesArray = jsonReader.readArray();
                
                int contador = 0;
                for (JsonValue valor : notificacoesArray) {
                    JsonObject notificacao = valor.asJsonObject();
                    
                    // Extrai os dados da notificação
                    String topico = notificacao.containsKey("topico") ? 
                            notificacao.getString("topico") : "lab.notificacoes.todos";
                    
                    if (topico.isEmpty()) {
                        System.out.println("Aviso: Notificação sem tópico definido, usando 'lab.notificacoes.todos'");
                        topico = "lab.notificacoes.todos";
                    }
                    
                    String prioridade = notificacao.containsKey("prioridade") ? 
                            notificacao.getString("prioridade") : "MEDIA";
                    
                    if (prioridade.isEmpty()) {
                        prioridade = "MEDIA";
                    }
                    
                    // Como o toString() de JsonObject pode não ser formatado igual ao original,
                    // reconstruímos a string JSON para manter a consistência
                    String notificacaoJson = notificacaoToJsonString(notificacao);
                    
                    publicarNotificacao(topico, notificacaoJson, prioridade);
                    contador++;
                }
                
                System.out.println(contador + " notificações publicadas com sucesso!");
            }
        } else {
            // Notificação única
            try (JsonReader jsonReader = Json.createReader(new StringReader(conteudo))) {
                JsonObject notificacao = jsonReader.readObject();
                
                String topico = notificacao.containsKey("topico") ? 
                        notificacao.getString("topico") : "lab.notificacoes.todos";
                
                if (topico.isEmpty()) {
                    System.out.println("Aviso: Notificação sem tópico definido, usando 'lab.notificacoes.todos'");
                    topico = "lab.notificacoes.todos";
                }
                
                String prioridade = notificacao.containsKey("prioridade") ? 
                        notificacao.getString("prioridade") : "MEDIA";
                
                if (prioridade.isEmpty()) {
                    prioridade = "MEDIA";
                }
                
                // Como o toString() de JsonObject pode não ser formatado igual ao original,
                // reconstruímos a string JSON para manter a consistência
                String notificacaoJson = notificacaoToJsonString(notificacao);
                
                publicarNotificacao(topico, notificacaoJson, prioridade);
                System.out.println("Notificação publicada com sucesso!");
            }
        }
    }
    
    private static String notificacaoToJsonString(JsonObject notificacao) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        
        // Timestamp
        if (notificacao.containsKey("timestamp")) {
            sb.append("  \"timestamp\": ")
              .append(notificacao.getJsonNumber("timestamp").longValue())
              .append(",\n");
        } else {
            sb.append("  \"timestamp\": ")
              .append(System.currentTimeMillis())
              .append(",\n");
        }
        
        // Assunto
        sb.append("  \"assunto\": \"")
          .append(notificacao.containsKey("assunto") ? 
                 escapeJsonString(notificacao.getString("assunto")) : "")
          .append("\",\n");
        
        // Mensagem
        sb.append("  \"mensagem\": \"")
          .append(notificacao.containsKey("mensagem") ? 
                 escapeJsonString(notificacao.getString("mensagem")) : "")
          .append("\",\n");
        
        // Prioridade
        sb.append("  \"prioridade\": \"")
          .append(notificacao.containsKey("prioridade") ? 
                 notificacao.getString("prioridade") : "MEDIA")
          .append("\",\n");
        
        // Tópico
        sb.append("  \"topico\": \"")
          .append(notificacao.containsKey("topico") ? 
                 notificacao.getString("topico") : "lab.notificacoes.todos")
          .append("\"\n");
        
        sb.append("}");
        return sb.toString();
    }
    
    private static String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private static void publicarNotificacao(String topicName, String mensagem, String prioridade) throws JMSException {
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
            MessageProducer producer = session.createProducer(topic);
            
            // Definir o tempo de vida da mensagem com base na prioridade
            if (prioridade.equals("ALTA")) {
                producer.setTimeToLive(1000 * 60 * 60 * 24); // 1 dia
                producer.setPriority(9); // Prioridade máxima JMS (0-9)
            } else if (prioridade.equals("MEDIA")) {
                producer.setTimeToLive(1000 * 60 * 60 * 12); // 12 horas
                producer.setPriority(5); // Prioridade média JMS
            } else {
                producer.setTimeToLive(1000 * 60 * 60 * 6); // 6 horas
                producer.setPriority(1); // Prioridade baixa JMS
            }
            
            TextMessage message = session.createTextMessage(mensagem);
            message.setStringProperty("PRIORIDADE", prioridade);
            producer.send(message);
            
            System.out.println("Mensagem publicada no tópico " + topicName + ":");
            System.out.println(mensagem);
            
            connection.close();
        }
    }
}