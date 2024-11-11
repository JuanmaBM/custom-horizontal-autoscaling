# Scaling service with KEDA

This demo illustrates the power of KEDA in dynamically scaling applications based on real-world conditions and metrics, overcoming the limitations of Kubernetes' native HPA. By leveraging event-driven autoscaling, custom metrics, and flexible scaling strategies, KEDA can optimize resource utilization and improve application performance across diverse workloads.


## Introduction to Elastic Systems Like Kubernetes

Kubernetes is a container orchestration platform designed to manage applications at scale. One of its key strengths is its ability to elastically manage resources based on demand. In an elastic system, applications can scale up (adding more instances) when there’s high traffic or scale down when demand decreases. This elasticity allows systems to optimize resource usage, ensuring efficient use of infrastructure while maintaining application performance.

## Horizontal Pod Autoscaler (HPA) in Kubernetes

In Kubernetes, the default mechanism for scaling applications is the Horizontal Pod Autoscaler (HPA). HPA adjusts the number of pod replicas in a deployment based on predefined metrics, such as CPU or memory usage. While HPA is useful for simple scaling needs, it has limitations:

- **Limited Metrics**: HPA primarily uses CPU and memory metrics, which might not be sufficient for more complex workloads, such as event-driven applications.
- **Lack of Event-Driven Scaling**: HPA does not natively support scaling based on external events, like messages in a queue, unless additional custom metrics are configured.
- **Static Thresholds**: Scaling decisions in HPA are made based on static thresholds, which may not always reflect the dynamic nature of certain workloads.

These limitations make HPA less suitable for certain use cases, such as event-driven systems or applications that rely on external metrics to make scaling decisions.

## What is KEDA?

Kubernetes Event-Driven Autoscaler (KEDA) is a Kubernetes-based autoscaling solution designed to address the limitations of HPA. KEDA extends Kubernetes by allowing it to scale applications based on external event sources and custom metrics, such as message queue lengths, database load, or response times.

### Key Features of KEDA:
- **Event-Driven Autoscaling**: KEDA enables scaling based on external events, such as the number of messages in Kafka, Redis, or other event sources.
- **Multiple Metrics**: KEDA supports custom metrics beyond CPU and memory, such as Prometheus-based metrics, allowing more granular control over scaling.
- **Scale-to-Zero**: KEDA can scale workloads down to zero replicas when no events are being processed, saving resources.

By integrating with existing Kubernetes infrastructure, KEDA provides a flexible, event-driven approach to autoscaling that complements Kubernetes' native HPA.

## Demo Explanation

**__DISCLAIMER__**:

> ❗️ **NOTE:** For testing purposes Vegeta attack has been used in this demo. If you are not familirized with it you can check the documentation in the following link https://github.com/tsenart/vegeta

> ⚠️ **IMPORTANT**: You'll need a connection to Kubernetes/ocp cluster by kubectl/oc in order to apply all changes

### Scenario 1: Event-Driven Scaling with Kafka

In this scenario, we will simulate a workload where an API sends one request per second, producing messages to a Kafka topic. KEDA will monitor the Kafka topic and automatically scale the service responsible for consuming the messages whenever new events are detected.

**Steps:**
- The API sends one message per second to Kafka.
- KEDA detects new messages in the Kafka topic.
- KEDA scales the consumer service up to handle the message load and scales it down when the messages are processed.

**Commands**

```
kubectl run scenario-1-test --restart=Never --image="peterevans/vegeta" -- sh -c \
"jq -ncM '{method: \"POST\", url: \"http://http-kafka-mock-custom-autoscaler-test.apps.cluster-k4724.k4724.sandbox3206.opentlc.com/\", body: \"{\\\"message\\\":\\\"Hello world\\\"}\" | @base64, header: {\"Content-Type\": [\"application/json\"]}}' | \
vegeta attack -format=json -rate=1 -duration=10s | tee results.bin | vegeta report"
```

### Scenario 2: Scaling Kafka Consumers Under High Load

Here, we will simulate a higher message load scenario where one Kafka consumer is insufficient to process all the messages in a topic. KEDA will automatically scale the number of consumer instances to keep up with the increasing workload.

**Steps:**
- A large number of messages are pushed to the Kafka topic.
- KEDA monitors the message queue length.
- When the message backlog grows beyond a certain threshold, KEDA scales up the number of Kafka consumer instances.
- Once the backlog is processed, KEDA scales the service down again.

**Commands**

```
kubectl patch deployment http-kafka-mock -n custom-autoscaler-test \
--type='json' \
-p='[{"op": "replace", "path": "/spec/template/spec/containers/0/env/1/value", "value": "0"}, {"op": "replace", "path": "/spec/template/spec/containers/0/env/2/value", "value": "0"}]'

```
```
kubectl run scenario-2-test --restart=Never --image="peterevans/vegeta" -- sh -c \
"jq -ncM '{method: \"POST\", url: \"http://http-kafka-mock-custom-autoscaler-test.apps.cluster-k4724.k4724.sandbox3206.opentlc.com/\", body: \"{\\\"message\\\":\\\"Hello world\\\"}\" | @base64, header: {\"Content-Type\": [\"application/json\"]}}' | \
vegeta attack -format=json -rate=1000 -duration=30s | tee results.bin | vegeta report"
```

### Scenario 3: API Scaling Based on Response Time

In this scenario, we will showcase how KEDA can use Prometheus metrics to monitor the response time of an HTTP API and ensure that the response time stays around 2 seconds by adjusting the number of service instances.

**Steps:**
- Prometheus collects the response time metrics for the HTTP API.
- KEDA monitors the response time and automatically scales the service to ensure the API responds within approximately 2 seconds.
- If the response time increases, additional instances are created to handle the load and reduce the latency.

**Commands**


```
kubectl patch deployment http-kafka-mock -n custom-autoscaler-test \
--type='json' \
-p='[{"op": "replace", "path": "/spec/template/spec/containers/0/env/1/value", "value": "3000"}, {"op": "replace", "path": "/spec/template/spec/containers/0/env/2/value", "value": "1500"}]'

```

```
kubectl run scenario-3-test --restart=Never --image="peterevans/vegeta" -- sh -c \
"jq -ncM '{method: \"POST\", url: \"http://http-kafka-mock-custom-autoscaler-test.apps.cluster-k4724.k4724.sandbox3206.opentlc.com/\", body: \"{\\\"message\\\":\\\"Hello world\\\"}\" | @base64, header: {\"Content-Type\": [\"application/json\"]}}' | \
vegeta attack -format=json -rate=50 -duration=60s | tee results.bin | vegeta report"
```

### Scenario 4: Handling Large Request Spikes with Multiple Metrics

In this final scenario, we will demonstrate how KEDA can handle large spikes in request traffic. We will start with two default replicas of the HTTP API service and use both CPU average and Prometheus-based metrics (with a 2-second response time target) to dynamically scale the API service as needed.

**Steps:**
- Two API replicas are deployed by default to handle normal traffic.
- When a spike in requests occurs, KEDA monitors both CPU usage and response time.
- KEDA uses these metrics to scale up the API service, ensuring it can handle both the increased load and maintain an average response time of 2 seconds.
- After the traffic spike subsides, KEDA scales the service back down to optimize resource usage.

**Commands**

```
kubectl apply -f scenario-4.yaml
```

```
kubectl run scenario-4-test --restart=Never --image="peterevans/vegeta" -- sh -c \
"jq -ncM '{method: \"POST\", url: \"http://http-kafka-mock-custom-autoscaler-test.apps.cluster-k4724.k4724.sandbox3206.opentlc.com/\", body: \"{\\\"message\\\":\\\"Hello world\\\"}\" | @base64, header: {\"Content-Type\": [\"application/json\"]}}' | \
vegeta attack -format=json -rate=200 -duration=60s | tee results.bin | vegeta report"
```