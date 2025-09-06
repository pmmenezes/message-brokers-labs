package com.lab.subscriber;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class NotificacaoSubscriber {

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
    }
    
    // Flag de controle para manter o subscriber ativo
    private static boolean keepRunning = true;

    public static void main(String[] args) {
        try {
            int option = showMenu();
            
            if (option > 0 && option <= TOPIC_AREAS.size()) {
                String topicName = TOPIC_AREAS.get(option);
                iniciarSubscriber(topicName);
            } else if (option == 5) {
                inscricaoMultipla();
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
        System.out.println("=== ASSINANTE DE NOTIFICAÇÕES ===");
        System.out.println("Selecione a área para receber notificações:");
        System.out.println("1. Financeiro");
        System.out.println("2. Logística");
        System.out.println("3. Comercial");
        System.out.println("4. Todas as Áreas");
        System.out.println("5. Múltiplos Tópicos");
        System.out.print("Escolha uma opção: ");
        
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void iniciarSubscriber(String topicName) throws Exception {
        System.out.println("Iniciando assinatura do tópico: " + topicName);
        System.out.println("Aguardando notificações...");
        System.out.println("Pressione ENTER a qualquer momento para encerrar.");
        
        // Thread para monitorar a entrada do usuário para encerrar
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();  // Aguarda o ENTER
            keepRunning = false;
            System.out.println("Encerrando assinatura...");
        });
        inputThread.setDaemon(true);
        inputThread.start();
        
        // Configuração da conexão com o broker JMS
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
            
            // Criação do consumidor com filtro opcional
            MessageConsumer consumer = session.createConsumer(topic);
            
            // Loop de recebimento de mensagens
            while (keepRunning) {
                // Espera por uma mensagem com timeout
                Message message = consumer.receive(1000);
                
                if (message != null) {
                    processarMensagem(message);
                }
            }
            
            connection.close();
        }
        
        System.out.println("Assinatura encerrada.");
    }
    
    private static void inscricaoMultipla() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== ASSINATURA MÚLTIPLA ===");
        System.out.println("Selecione as áreas (separadas por vírgula, ex: 1,3,4):");
        
        for (Map.Entry<Integer, String> entry : TOPIC_AREAS.entrySet()) {
            System.out.println(entry.getKey() + ". " + entry.getValue());
        }
        
        System.out.print("Escolha: ");
        String input = scanner.nextLine();
        
        String[] selections = input.split(",");
        StringBuilder topicFilter = new StringBuilder();
        
        for (String sel : selections) {
            try {
                int option = Integer.parseInt(sel.trim());
                if (option > 0 && option <= TOPIC_AREAS.size()) {
                    if (topicFilter.length() > 0) {
                        topicFilter.append(" OR ");
                    }
                    topicFilter.append("topicName='").append(TOPIC_AREAS.get(option)).append("'");
                }
            } catch (NumberFormatException e) {
                // Ignora opções inválidas
            }
        }
        
        if (topicFilter.length() == 0) {
            System.out.println("Nenhuma opção válida selecionada. Saindo.");
            return;
        }
        
        System.out.println("Filtro de tópicos: " + topicFilter.toString());
        System.out.println("Aguardando notificações...");
        System.out.println("Pressione ENTER a qualquer momento para encerrar.");
        
        // Thread para monitorar a entrada do usuário para encerrar
        Thread inputThread = new Thread(() -> {
            Scanner s = new Scanner(System.in);
            s.nextLine();  // Aguarda o ENTER
            keepRunning = false;
            System.out.println("Encerrando assinatura...");
        });
        inputThread.setDaemon(true);
        inputThread.start();
        
        // Configuração da conexão com o broker JMS
        try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL)) {
            Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD);
            connection.start();
            
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Criar um consumidor para cada tópico selecionado
            for (String sel : selections) {
                try {
                    int option = Integer.parseInt(sel.trim());
                    if (option > 0 && option <= TOPIC_AREAS.size()) {
                        String topicName = TOPIC_AREAS.get(option);
                        Topic topic = session.createTopic(topicName);
                        MessageConsumer consumer = session.createConsumer(topic);
                        
                        // Definir o MessageListener para este consumidor
                        consumer.setMessageListener(message -> {
                            try {
                                processarMensagem(message);
                            } catch (Exception e) {
                                System.err.println("Erro ao processar mensagem: " + e.getMessage());
                            }
                        });
                    }
                } catch (NumberFormatException e) {
                    // Ignora opções inválidas
                }
            }
            
            // Manter a aplicação em execução até que o usuário encerre
            while (keepRunning) {
                Thread.sleep(1000);
            }
            
            connection.close();
        }
        
        System.out.println("Assinatura encerrada.");
    }
    
    private static void processarMensagem(Message message) throws Exception {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String conteudo = textMessage.getText();
            
            try (JsonReader jsonReader = Json.createReader(new StringReader(conteudo))) {
                JsonObject notificacao = jsonReader.readObject();
                
                // Extrair as informações da notificação
                long timestamp = notificacao.containsKey("timestamp") ? 
                        notificacao.getJsonNumber("timestamp").longValue() : System.currentTimeMillis();
                
                String assunto = notificacao.containsKey("assunto") ? 
                        notificacao.getString("assunto") : "(Sem assunto)";
                
                String mensagem = notificacao.containsKey("mensagem") ? 
                        notificacao.getString("mensagem") : "(Sem mensagem)";
                
                String prioridade = notificacao.containsKey("prioridade") ? 
                        notificacao.getString("prioridade") : "MEDIA";
                
                String topico = notificacao.containsKey("topico") ? 
                        notificacao.getString("topico") : "(Tópico desconhecido)";
                
                // Formatação da data/hora
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String dataHora = sdf.format(new Date(timestamp));
                
                // Exibe a notificação formatada
                System.out.println("\n========== NOVA NOTIFICAÇÃO ==========");
                System.out.println("Data/Hora: " + dataHora);
                System.out.println("Tópico: " + topico);
                System.out.println("Prioridade: " + prioridade);
                System.out.println("Assunto: " + assunto);
                System.out.println("Mensagem: " + mensagem);
                System.out.println("======================================\n");
            }
        }
    }
}