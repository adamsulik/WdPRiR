import threading
import pika

HOST = "localhost"
EXCHANGE_NAME = "messenger"
EXCHANGE_TYPE = "fanout"


class BaseClient:
    def __init__(self):
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=HOST))
        self.channel = self.connection.channel()
        self.channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type=EXCHANGE_TYPE)


class Sender(BaseClient):
    def __init__(self, username):
        super().__init__()
        self.username = username

    def __call__(self, payload):
        message = f'{self.username}: {payload}'
        self.channel.basic_publish(exchange=EXCHANGE_NAME, routing_key='', body=message)

    def send_func(self):
        while (True):
            msg = input()
            self(msg)

    def exit(self):
        self.connection.close()


class Receiver(BaseClient):
    def __init__(self):
        super().__init__()
        self.result = self.channel.queue_declare(queue='', exclusive=True)
        self.queue_name = self.result.method.queue
        self.channel.queue_bind(exchange=EXCHANGE_NAME, queue=self.queue_name)

    @staticmethod
    def callback(ch, method, properties, body):
        msg = body.decode('ascii')
        print("%s" % msg)

    def __call__(self, *args, **kwargs):
        self.channel.basic_consume(
            queue=self.queue_name,
            on_message_callback=self.callback,
            auto_ack=True)

        self.channel.start_consuming()


if __name__ == "__main__":
    username = input('Enter username\n')
    sender = Sender(username)
    receiver = Receiver()

    sending_thread = threading.Thread(target=sender.send_func)
    receiving_thread = threading.Thread(target=receiver)

    receiving_thread.start()
    sending_thread.start()
