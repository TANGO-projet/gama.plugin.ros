package gama.plugin.ros.types;

import java.util.concurrent.ConcurrentLinkedQueue;

import gama.api.gaml.types.IType;
import gama.api.runtime.scope.IScope;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;
import gama.plugin.ros.utils.RosMessageConverter;
import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2MessageReader;
import us.ihmc.jros2.ROS2Subscription;
import us.ihmc.jros2.ROS2SubscriptionCallback;

/**
 * Wraps a jros2 {@link ROS2Subscription} as a GAMA value.
 *
 * <p>Incoming messages are converted to a GAMA map immediately in the callback
 * thread and stored in a thread-safe queue.  GAMA operators then dequeue them
 * from the simulation thread, keeping the two concurrency domains decoupled.
 */
public class GamaROSSubscription implements IValue {

	private final ROS2Subscription<?> subscription;
	private final GamaROSNode node;
	private final GamaROSTopic topic;

	/** Thread-safe queue of converted messages (GamaMap representation). */
	private final ConcurrentLinkedQueue<Object> messageQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Private constructor — use {@link #create(GamaROSNode, GamaROSTopic)}.
	 * The {@code sub} parameter is the already-created jros2 subscription.
	 */
	private GamaROSSubscription(final GamaROSNode node, final GamaROSTopic topic,
			final ROS2Subscription<?> sub) {
		this.node = node;
		this.topic = topic;
		this.subscription = sub;
	}

	/**
	 * Factory method: creates the subscription on the given node/topic and
	 * wires the callback that fills the internal message queue.
	 */
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public static GamaROSSubscription create(final GamaROSNode node, final GamaROSTopic topic) {
//		// Temporary holder so the lambda can reference the wrapper before it exists.
//		// We fill it immediately after construction.
//		GamaROSSubscription[] holder = new GamaROSSubscription[1];
//
//		ROS2SubscriptionCallback<ROS2Message> callback = (ROS2MessageReader<ROS2Message> reader) -> {
//			ROS2Message msg = reader.readLatest();
//			if (msg != null && holder[0] != null) {
//				holder[0].messageQueue.add(RosMessageConverter.toGamaMap(msg));
//			}
//		};
//
//		ROS2Subscription<?> sub = (ROS2Subscription<?>) node.getNode().createSubscription(
//				(us.ihmc.jros2.ROS2Topic) topic.getRos2Topic(), callback);
//
//		GamaROSSubscription instance = new GamaROSSubscription(node, topic, sub);
//		holder[0] = instance;
//		return instance;
//	}

	// ── Queue access ──────────────────────────────────────────────────────────

	/** Returns {@code true} if at least one message is waiting in the queue. */
	public boolean hasData() {
		return !messageQueue.isEmpty();
	}

	/**
	 * Removes and returns the oldest message as a GAMA map,
	 * or {@code null} if the queue is empty.
	 */
	public Object poll() {
		return messageQueue.poll();
	}

	/**
	 * Drains the queue and returns the most recent message (discards older ones),
	 * or {@code null} if the queue is empty.
	 */
	public Object pollLatest() {
		Object latest = null;
		Object item;
		while ((item = messageQueue.poll()) != null) {
			latest = item;
		}
		return latest;
	}

	/** Discards all queued messages. */
	public void clearQueue() {
		messageQueue.clear();
	}

	/** Number of messages currently waiting. */
	public int queueSize() {
		return messageQueue.size();
	}

	// ── Lifecycle ─────────────────────────────────────────────────────────────

	/** Destroy this subscription on the owning node. */
	public void close() {
		node.getNode().destroySubscription(subscription);
	}

	public boolean isClosed() {
		return subscription.isClosed();
	}

	public GamaROSNode getNode() { return node; }
	public GamaROSTopic getTopic() { return topic; }

	// ── IValue ────────────────────────────────────────────────────────────────

	@Override
	public IType<?> getGamlType() {
		return GamaROSSubscriptionType.INSTANCE;
	}

	@Override
	public String stringValue(final IScope scope) {
		return "ros_subscription(" + subscription.getTopicName() + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", subscription.getTopicName());
	}

	@Override
	public IValue copy(final IScope scope) {
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}
}
