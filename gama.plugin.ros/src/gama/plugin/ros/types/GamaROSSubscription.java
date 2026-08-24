/*******************************************************************************************************
 *
 * GamaROSSubscription.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation
 * platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.types;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.IType;
import gama.api.gaml.types.Types;
import gama.api.runtime.scope.IScope;
import gama.api.types.list.GamaListFactory;
import gama.api.types.list.IList;
import gama.api.types.map.IMap;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;
import gama.plugin.ros.utils.RosMessageConverter;
import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2MessageReader;
import us.ihmc.jros2.ROS2Node;
import us.ihmc.jros2.ROS2Subscription;
import us.ihmc.jros2.ROS2SubscriptionCallback;
import us.ihmc.jros2.ROS2Topic;

/**
 * A subscription on one topic of one node, buffering what arrives until the simulation reads it.
 *
 * <h3>Crossing the two clocks</h3>
 *
 * <p>
 * ROS delivers messages on a Fast DDS receive thread, whenever the network has something, while a GAMA model reads
 * them at fixed points of its own cycle. The two cannot share objects: the message the callback is handed is only
 * valid for the duration of the callback. So each message is converted to a GAML map <b>in the callback thread</b> and
 * pushed on a bounded queue, which the operators drain from the simulation thread. Nothing of jros2 crosses over.
 * </p>
 *
 * <p>
 * The queue is bounded because a sensor topic can easily publish faster than a simulation steps: when it is full the
 * oldest message is dropped, which is the behaviour of a ROS subscription with {@code KEEP_LAST} and the same depth.
 * {@code getDroppedCount} reports how many were lost, so a model can tell a queue that is too small from a topic that
 * is simply silent.
 * </p>
 */
public class GamaROSSubscription implements IValue {

	/** The jros2 subscription. */
	private final ROS2Subscription<?> subscription;

	/** The node that owns the subscription and that must destroy it. */
	private final GamaROSNode node;

	/** The topic being listened to. */
	private final GamaROSTopic topic;

	/** The messages converted by the callback thread and waiting for the simulation thread. */
	private final BlockingQueue<IMap<String, Object>> queue;

	/** How many messages the queue had to drop because the model did not read fast enough. */
	private final AtomicLong dropped = new AtomicLong();

	/** How many messages have been received, dropped ones included. */
	private final AtomicLong received = new AtomicLong();

	/**
	 * Creates a subscription and starts receiving on it.
	 *
	 * <p>
	 * The callback captures the queue, not this wrapper: the queue exists before the subscription does, which is what
	 * makes it safe for a message to arrive while the constructor is still running.
	 * </p>
	 *
	 * @param scope
	 *            the scope, used to report a creation failure
	 * @param node
	 *            the node, which must be open
	 * @param topic
	 *            the topic
	 * @param queueSize
	 *            how many messages to keep before dropping the oldest
	 */
	@SuppressWarnings ({ "unchecked", "rawtypes" })
	public GamaROSSubscription(final IScope scope, final GamaROSNode node, final GamaROSTopic topic,
			final int queueSize) {
		if (node == null || topic == null)
			throw GamaRuntimeException.error("A ROS2 subscription needs both a node and a topic", scope);
		if (queueSize < 1) throw GamaRuntimeException
				.error("The queue size of a subscription must be at least 1, " + queueSize + " given", scope);
		node.checkOpen(scope);
		this.node = node;
		this.topic = topic;
		this.queue = new ArrayBlockingQueue<>(queueSize);
		try {
			this.subscription = subscribe(node.getNode(), (us.ihmc.jros2.ROS2Topic) topic.getRos2Topic(), queue,
					received, dropped);
		} catch (final Throwable t) {
			throw GamaRuntimeException.error("The subscription to '" + topic.getTopicName() + "' could not be created: "
					+ t.getMessage(), scope);
		}
	}

	/**
	 * Creates the jros2 subscription and its callback.
	 *
	 * <p>
	 * Static and generic on purpose: the callback closes over the queue and the two counters rather than over the
	 * wrapper, so it is already safe to run while the constructor above is still executing, and the message type is
	 * known inside the lambda instead of being erased.
	 * </p>
	 *
	 * @param <T>
	 *            the message type of the topic
	 * @param node
	 *            the jros2 node
	 * @param topic
	 *            the jros2 topic
	 * @param queue
	 *            the queue the callback fills
	 * @param received
	 *            the counter of arrived messages
	 * @param dropped
	 *            the counter of messages the queue could not hold
	 * @return the subscription
	 */
	private static <T extends ROS2Message<T>> ROS2Subscription<T> subscribe(final ROS2Node node,
			final ROS2Topic<T> topic, final BlockingQueue<IMap<String, Object>> queue, final AtomicLong received,
			final AtomicLong dropped) {
		final ROS2SubscriptionCallback<T> callback = (final ROS2MessageReader<T> reader) -> {
			// jros2 notifies once per sample, but draining costs nothing and keeps up if the thread falls behind
			T msg;
			while ((msg = reader.read()) != null) {
				received.incrementAndGet();
				final IMap<String, Object> converted = RosMessageConverter.toGamaMap(msg);
				if (!queue.offer(converted)) {
					// KEEP_LAST semantics: make room by giving up the oldest message rather than the newest
					queue.poll();
					dropped.incrementAndGet();
					if (!queue.offer(converted)) { dropped.incrementAndGet(); }
				}
			}
		};
		return node.createSubscription(topic, callback, topic.getQoS());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Reading, from the simulation thread
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * @return true if at least one message is waiting
	 */
	public boolean hasData() { return !queue.isEmpty(); }

	/**
	 * Removes and returns the oldest waiting message.
	 *
	 * @return the message, or null if none is waiting
	 */
	public IMap<String, Object> poll() {
		return queue.poll();
	}

	/**
	 * Removes every waiting message and returns the most recent one, which is what a model interested in the current
	 * state of a sensor rather than in its history wants.
	 *
	 * @return the newest message, or null if none is waiting
	 */
	public IMap<String, Object> pollLatest() {
		IMap<String, Object> latest = null;
		IMap<String, Object> next;
		while ((next = queue.poll()) != null) { latest = next; }
		return latest;
	}

	/**
	 * Removes every waiting message and returns them, oldest first.
	 *
	 * @return the messages, possibly empty but never null
	 */
	public IList<IMap<String, Object>> pollAll() {
		final IList<IMap<String, Object>> result = GamaListFactory.create(Types.MAP);
		IMap<String, Object> next;
		while ((next = queue.poll()) != null) { result.add(next); }
		return result;
	}

	/**
	 * Discards every waiting message.
	 *
	 * @return how many were discarded
	 */
	public int clearQueue() {
		final int size = queue.size();
		queue.clear();
		return size;
	}

	/**
	 * @return how many messages are waiting
	 */
	public int queueSize() { return queue.size(); }

	/**
	 * @return how many messages the queue had to drop because the model did not read fast enough
	 */
	public long getDroppedCount() { return dropped.get(); }

	/**
	 * @return how many messages have arrived on the topic since the subscription was created
	 */
	public long getReceivedCount() { return received.get(); }

	/**
	 * Blocks until at least one publisher has been discovered on the topic.
	 *
	 * @param milliseconds
	 *            how long to wait at most
	 * @return true if a publisher was matched within the delay
	 */
	public boolean waitForPublisher(final long milliseconds) {
		return subscription.waitForPublisher(milliseconds);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Lifecycle
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * @return the node that owns this subscription
	 */
	public GamaROSNode getNode() { return node; }

	/**
	 * @return the topic being listened to
	 */
	public GamaROSTopic getTopic() { return topic; }

	/**
	 * Destroys the subscription on its node and drops what it had buffered. Calling this twice, or after the node has
	 * been closed, is harmless.
	 */
	public void close() {
		if (!subscription.isClosed() && !node.isClosed()) { node.getNode().destroySubscription(subscription); }
		queue.clear();
	}

	/**
	 * @return true once the subscription, or the node that owns it, has been closed
	 */
	public boolean isClosed() { return subscription.isClosed() || node.isClosed(); }

	/**
	 * Guards the operators that need a live subscription.
	 *
	 * @param scope
	 *            the scope
	 */
	public void checkOpen(final IScope scope) {
		if (isClosed()) throw GamaRuntimeException.error(
				"The subscription to '" + topic.getTopicName() + "' is closed and cannot be used any more", scope);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// IValue
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public IType<?> getGamlType() { return Types.get(GamaROSSubscription.class); }

	@Override
	public String stringValue(final IScope scope) {
		return "subscription to " + topic.getTopicName() + " (" + topic.getMessageClass().getName() + ", "
				+ queue.size() + " waiting)";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", topic.getTopicName(), "type",
				topic.getMessageClass().getName(), "waiting", queue.size(), "received", received.get());
	}

	@Override
	public IValue copy(final IScope scope) {
		// a subscription is a native resource with one queue: copying would fork the stream of messages
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}

}
