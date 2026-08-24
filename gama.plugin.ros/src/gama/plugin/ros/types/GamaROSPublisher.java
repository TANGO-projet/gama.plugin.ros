/*******************************************************************************************************
 *
 * GamaROSPublisher.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.types;

import java.util.Map;

import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.IType;
import gama.api.gaml.types.Types;
import gama.api.runtime.scope.IScope;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;
import gama.plugin.ros.utils.RosMessageConverter;
import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2Publisher;

/**
 * A publisher on one topic of one node.
 *
 * <p>
 * Each publication allocates a fresh message, so that a field left out of the GAML map takes its ROS default rather
 * than the value of the previous publication. {@code publish} serialises synchronously, so nothing of the message
 * survives the call.
 * </p>
 */
public class GamaROSPublisher implements IValue {

	/** The jros2 publisher. */
	private final ROS2Publisher<?> publisher;

	/** The node that owns the publisher and that must destroy it. */
	private final GamaROSNode node;

	/** The topic being published on. */
	private final GamaROSTopic topic;

	/** How many messages have been published, for reporting. */
	private long published;

	/**
	 * Creates a publisher.
	 *
	 * @param scope
	 *            the scope, used to report a creation failure
	 * @param node
	 *            the node, which must be open
	 * @param topic
	 *            the topic
	 */
	@SuppressWarnings ({ "unchecked", "rawtypes" })
	public GamaROSPublisher(final IScope scope, final GamaROSNode node, final GamaROSTopic topic) {
		if (node == null || topic == null)
			throw GamaRuntimeException.error("A ROS2 publisher needs both a node and a topic", scope);
		node.checkOpen(scope);
		this.node = node;
		this.topic = topic;
		try {
			this.publisher = node.getNode().createPublisher((us.ihmc.jros2.ROS2Topic) topic.getRos2Topic(),
					topic.getQos());
		} catch (final Throwable t) {
			throw GamaRuntimeException.error("The publisher on '" + topic.getTopicName() + "' could not be created: "
					+ t.getMessage(), scope);
		}
	}

	/**
	 * Builds a message from a GAML map and writes it on the network.
	 *
	 * @param scope
	 *            the scope
	 * @param message
	 *            the fields to write, keyed by field name
	 */
	@SuppressWarnings ({ "unchecked", "rawtypes" })
	public void publish(final IScope scope, final Map<String, Object> message) {
		checkOpen(scope);
		if (message == null) throw GamaRuntimeException
				.error("Cannot publish nil on '" + topic.getTopicName() + "'; publish an empty map instead", scope);
		final ROS2Message<?> msg = RosMessageConverter.newInstance(scope, topic.getMessageClass());
		RosMessageConverter.fromGamaMap(scope, msg, message);
		try {
			((ROS2Publisher) publisher).publish(msg);
		} catch (final Throwable t) {
			throw GamaRuntimeException
					.error("Publishing on '" + topic.getTopicName() + "' failed: " + t.getMessage(), scope);
		}
		published++;
	}

	/**
	 * Blocks until at least one subscriber has been discovered on the topic.
	 *
	 * <p>
	 * ROS2 discovery is asynchronous, and messages published before a subscriber is matched are simply dropped under
	 * the default volatile durability. A model that publishes a handful of messages should wait here first.
	 * </p>
	 *
	 * @param milliseconds
	 *            how long to wait at most
	 * @return true if a subscriber was matched within the delay
	 */
	public boolean waitForSubscriber(final long milliseconds) {
		return publisher.waitForSubscription(milliseconds);
	}

	/**
	 * @return the number of subscribers currently matched on the topic
	 */
	public int getSubscriberCount() { return publisher.getPublicationMatchedStatus(); }

	/**
	 * @return how many messages this publisher has written
	 */
	public long getPublishedCount() { return published; }

	/**
	 * @return the node that owns this publisher
	 */
	public GamaROSNode getNode() { return node; }

	/**
	 * @return the topic being published on
	 */
	public GamaROSTopic getTopic() { return topic; }

	/**
	 * Destroys the publisher on its node. Calling this twice, or after the node has been closed, is harmless.
	 */
	public void close() {
		if (!publisher.isClosed() && !node.isClosed()) { node.getNode().destroyPublisher(publisher); }
	}

	/**
	 * @return true once the publisher, or the node that owns it, has been closed
	 */
	public boolean isClosed() { return publisher.isClosed() || node.isClosed(); }

	/**
	 * Guards the operators that need a live publisher.
	 *
	 * @param scope
	 *            the scope
	 */
	public void checkOpen(final IScope scope) {
		if (isClosed()) throw GamaRuntimeException.error(
				"The publisher on '" + topic.getTopicName() + "' is closed and cannot be used any more", scope);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// IValue
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public IType<?> getGamlType() { return Types.get(GamaROSPublisher.class); }

	@Override
	public String stringValue(final IScope scope) {
		return "publisher on " + topic.getTopicName() + " (" + topic.getMessageClass().getName() + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", topic.getTopicName(), "type",
				topic.getMessageClass().getName(), "published", published);
	}

	@Override
	public IValue copy(final IScope scope) {
		// a publisher is a native resource: copying would create a second writer on the topic
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}

}
