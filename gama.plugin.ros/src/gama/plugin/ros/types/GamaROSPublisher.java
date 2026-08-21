package gama.plugin.ros.types;

import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2Publisher;

import gama.api.gaml.types.IType;
import gama.api.runtime.scope.IScope;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;

/**
 * Wraps a jros2 {@link ROS2Publisher} as a GAMA value.
 *
 * <p>The publisher is created by the node and held here. Closing this wrapper
 * delegates to {@link GamaROSNode} → {@link us.ihmc.jros2.ROS2Node#destroyPublisher}.
 */
public class GamaROSPublisher implements IValue {

	private final ROS2Publisher<?> publisher;
	private final GamaROSNode node;
	private final GamaROSTopic topic;

	public GamaROSPublisher(final GamaROSNode node, final GamaROSTopic topic) {
		this.node = node;
		this.topic = topic;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ROS2Publisher pub = node.getNode().createPublisher(
				(us.ihmc.jros2.ROS2Topic) topic.getRos2Topic());
		this.publisher = pub;
	}

	@SuppressWarnings("unchecked")
	public <T extends ROS2Message<T>> ROS2Publisher<T> getPublisher() {
		return (ROS2Publisher<T>) publisher;
	}

	public GamaROSNode getNode() {
		return node;
	}

	public GamaROSTopic getTopic() {
		return topic;
	}

	/** Destroy this publisher on the owning node. */
	public void close() {
		node.getNode().destroyPublisher(publisher);
	}

	public boolean isClosed() {
		return publisher.isClosed();
	}

	// ── IValue ────────────────────────────────────────────────────────────────

	@Override
	public IType<?> getGamlType() {
		return GamaROSPublisherType.INSTANCE;
	}

	@Override
	public String stringValue(final IScope scope) {
		return "ros_publisher(" + publisher.getTopicName() + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", publisher.getTopicName());
	}

	@Override
	public IValue copy(final IScope scope) {
		// Publishers are not cloneable
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}
}
