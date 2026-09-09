/*******************************************************************************************************
 *
 * GamaROSTopic.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.types;

import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.IType;
import gama.api.gaml.types.Types;
import gama.api.runtime.scope.IScope;
import gama.api.types.list.IList;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;
import gama.plugin.ros.utils.RosMessageConverter;
import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2QoSProfile;
import us.ihmc.jros2.ROS2Topic;

/**
 * A topic name plus the message type carried on it, the pair a publisher or a subscription is built from.
 *
 * <p>
 * The message class is resolved and validated when the topic is created rather than when it is used, so that a wrong
 * type name is reported at the {@code ros_topic(...)} call, where the model can be fixed, instead of at the first
 * publication. Both the ROS spelling ({@code "geometry_msgs/msg/Twist"}, {@code "std_msgs/String"}) and the Java one
 * ({@code "geometry_msgs.Twist"}) are accepted, and the trailing underscore jros2 adds to the classes whose name would
 * clash with a Java one ({@code std_msgs.String_}, {@code example_interfaces.Byte_}) is tried automatically.
 * </p>
 *
 * <p>
 * A topic is an immutable description, not a network resource: it holds no native handle and needs no closing. The
 * same topic can be given to any number of nodes.
 * </p>
 */
public class GamaROSTopic implements IValue {

	/** The name of the topic, as it appears in {@code ros2 topic list}. */
	private final String topicName;

	/** The name of the message type, as written in the model. */
	private final String messageTypeName;

	/** The quality of service the publishers and subscriptions of this topic use by default. */
	private final ROS2QoSProfile qos;

	/** The resolved message class, never null. */
	private final Class<? extends ROS2Message<?>> messageClass;

	/** The jros2 topic, built once since it is immutable too. */
	private final ROS2Topic<?> ros2Topic;

	/**
	 * Creates a topic with the default quality of service.
	 *
	 * @param scope
	 *            the scope, used to report an unknown message type
	 * @param topicName
	 *            the name of the topic
	 * @param messageTypeName
	 *            the message type, in ROS or in Java spelling
	 */
	public GamaROSTopic(final IScope scope, final String topicName, final String messageTypeName) {
		this(scope, topicName, messageTypeName, ROS2QoSProfile.DEFAULT);
	}

	/**
	 * Creates a topic.
	 *
	 * @param scope
	 *            the scope, used to report an unknown message type
	 * @param topicName
	 *            the name of the topic
	 * @param messageTypeName
	 *            the message type, in ROS or in Java spelling
	 * @param qos
	 *            the quality of service
	 */
	@SuppressWarnings ({ "unchecked", "rawtypes" })
	public GamaROSTopic(final IScope scope, final String topicName, final String messageTypeName,
			final ROS2QoSProfile qos) {
		if (topicName == null || topicName.isBlank())
			throw GamaRuntimeException.error("A ROS2 topic needs a name", scope);
		// ROS2 topics are DDS topics named "rt" + the topic name, so a missing leading slash produces
		// "rtcmd_vel" instead of "rt/cmd_vel" and matches nothing. jros2 only logs a warning about it,
		// which is invisible from a model, so the slash is added here instead.
		final String trimmed = topicName.trim();
		this.topicName = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
		this.messageTypeName = messageTypeName == null ? "" : messageTypeName.trim();
		this.qos = qos == null ? ROS2QoSProfile.DEFAULT : qos;
		this.messageClass = resolve(scope, this.messageTypeName);
		this.ros2Topic = new ROS2Topic(this.topicName, this.messageClass, this.qos);
	}

	/**
	 * Resolves a message type name to the class jros2 generated for it.
	 *
	 * @param scope
	 *            the scope
	 * @param name
	 *            the message type, in ROS or in Java spelling
	 * @return the message class
	 */
	@SuppressWarnings ("unchecked")
	private static Class<? extends ROS2Message<?>> resolve(final IScope scope, final String name) {
		// geometry_msgs/msg/Twist and geometry_msgs/Twist both denote geometry_msgs.Twist
		final String candidate = name.replace("/msg/", ".").replace('/', '.').replace("::", ".");
		final ClassLoader loader = GamaROSTopic.class.getClassLoader();
		for (final String attempt : new String[] { candidate, candidate + "_" }) {
			try {
				final Class<?> found = Class.forName(attempt, false, loader);
				if (!ROS2Message.class.isAssignableFrom(found)) { break; }
				return (Class<? extends ROS2Message<?>>) found;
			} catch (final ClassNotFoundException e) {
				// try the next spelling
			}
		}
		throw GamaRuntimeException.error("'" + name + "' is not a ROS2 message type known to jros2. Message types are "
				+ "written either as in ROS ('geometry_msgs/msg/Twist') or as a Java class name "
				+ "('geometry_msgs.Twist'), and the packages bundled with the plugin are actionlib_msgs, "
				+ "builtin_interfaces, diagnostic_msgs, example_interfaces, geometry_msgs, lifecycle_msgs, nav_msgs, "
				+ "rcl_interfaces, rosgraph_msgs, sensor_msgs, shape_msgs, statistics_msgs, std_msgs, stereo_msgs, "
				+ "tf2_msgs, trajectory_msgs and visualization_msgs. Types defined by a package of your own are not "
				+ "available: jros2 generates its message classes ahead of time.", scope);
	}

	/**
	 * @return the name of the topic
	 */
	public String getTopicName() { return topicName; }

	/**
	 * @return the message type, as it was written in the model
	 */
	public String getMessageTypeName() { return messageTypeName; }

	/**
	 * @return the resolved message class
	 */
	public Class<? extends ROS2Message<?>> getMessageClass() { return messageClass; }

	/**
	 * @return the quality of service
	 */
	public ROS2QoSProfile getQos() { return qos; }

	/**
	 * @return the jros2 topic, ready to be given to a node
	 */
	public ROS2Topic<?> getRos2Topic() { return ros2Topic; }

	/**
	 * @return the names of the fields of the message type, which are also the keys of the maps read from and published
	 *         on this topic
	 */
	public IList<String> getFields() { return RosMessageConverter.fieldsOf(messageClass); }

	// ---------------------------------------------------------------------------------------------------------------
	// IValue
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public IType<?> getGamlType() { return Types.get(GamaROSTopic.class); }

	@Override
	public String stringValue(final IScope scope) {
		return topicName + " (" + messageClass.getName() + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "topic", topicName, "type", messageClass.getName());
	}

	@Override
	public IValue copy(final IScope scope) {
		// immutable: sharing the instance is safe and avoids resolving the class again
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}

}
