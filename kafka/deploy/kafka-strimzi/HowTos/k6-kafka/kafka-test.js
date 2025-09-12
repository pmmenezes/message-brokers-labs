// kafka-test.js
import { check } from 'k6';
import { Writer } from 'k6/x/kafka';

const writer = new Writer({
  brokers: ['kafka-service:9092'],
  topic: 'test-topic',
});

export let options = {
  vus: 5,           // 5 usuários virtuais
  duration: '2m',   // Por 2 minutos
};

export default function() {
  // Enviar mensagem para o Kafka
  writer.produce({
    messages: [
      {
        key: `key-${__VU}-${__ITER}`,
        value: JSON.stringify({
          timestamp: new Date().toISOString(),
          user: __VU,
          iteration: __ITER,
          message: 'Test message'
        }),
      },
    ],
  });

  check(writer, {
    'mensagem enviada': () => true,
  });
}