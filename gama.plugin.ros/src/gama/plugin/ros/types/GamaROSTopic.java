package gama.plugin.ros.types;

import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2QoSProfile;
import us.ihmc.jros2.ROS2Topic;

import gama.api.gaml.types.IType;
import gama.api.runtime.scope.IScope;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;

/**
 * Wraps a jros2 ROS2Topic as a GAMA value.
 *
 * <p>Stores the topic name, the fully-qualified ROS message class name
 * (e.g. {@code "geometry_msgs.Twist"}), and optionally a QoS profile.
 * The underlying {@link ROS2Topic} instance is created lazily via
 * {@link #getRos2Topic()}.
 */
public class GamaROSTopic implements IValue {

	private final String topicName;
	private final String messageClassName;
	private final ROS2QoSProfile qos;

	/** Cached ROS2Topic instance (created on first access). */
	private ROS2Topic<?> ros2Topic;

	public GamaROSTopic(final String topicName, final String messageClassName) {
		this(topicName, messageClassName, ROS2QoSProfile.DEFAULT);
	}

	public GamaROSTopic(final String topicName, final String messageClassName, final ROS2QoSProfile qos) {
		this.topicName = topicName;
		this.messageClassName = messageClassName;
		this.qos = qos;
	}

	public String getTopicName() {
		return topicName;
	}

	public String getMessageClassName() {
		return messageClassName;
	}

	public ROS2QoSProfile getQos() {
		return qos;
	}

	/**
	 * Returns the underlying {@link ROS2Topic}, loading the message class via
	 * reflection. Throws {@link IllegalArgumentException} if the class is not
	 * found on the bundle classpath.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ROS2Topic<?> getRos2Topic() {
		if (ros2Topic == null) {
			try {
				Class<? extends ROS2Message> msgClass =
						(Class<? extends ROS2Message>) Class.forName(messageClassName);
				ros2Topic = new ROS2Topic<>(topicName, msgClass, qos);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(
						"ROS2 message class not found: " + messageClassName, e);
			}
		}
		return ros2Topic;
	}

	// ── IValue ────────────────────────────────────────────────────────────────

	@Override
	public IType<?> getGamlType() {
		return GamaROSTopicType.INSTANCE;
	}

	@Override
	public String stringValue(final IScope scope) {
		return "ros_topic(" + topicName + ", " + messageClassName + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", topicName, "type", messageClassName);
	}

	@Override
	public IValue copy(final IScope scope) {
		return new GamaROSTopic(topicName, messageClassName, qos);
	}

	@Override
	public String toString() {
		return stringValue(null);
	}
}
