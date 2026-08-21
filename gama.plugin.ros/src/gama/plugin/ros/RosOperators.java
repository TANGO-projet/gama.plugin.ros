package gama.plugin.ros;

import java.util.Map;

import gama.annotations.doc;
import gama.annotations.example;
import gama.annotations.no_test;
import gama.annotations.operator;
import gama.annotations.support.Reason;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.runtime.scope.IScope;
import gama.plugin.ros.types.GamaROSNode;
import gama.plugin.ros.types.GamaROSPublisher;
import gama.plugin.ros.types.GamaROSSubscription;
import gama.plugin.ros.types.GamaROSTopic;
import gama.plugin.ros.utils.RosMessageConverter;
import us.ihmc.jros2.ROS2Message;
import us.ihmc.jros2.ROS2QoSProfile;

/**
 * GAMA operators for interacting with a ROS 2 network via jros2.
 *
 * <h3>Typical GAML usage</h3>
 * <pre>
 * global {
 *     ros_node     my_node;
 *     ros_topic    cmd_vel_topic;
 *     ros_publisher cmd_vel_pub;
 *     ros_subscription odom_sub;
 *
 *     init {
 *         my_node       <- ros_node("my_gama_node");
 *         cmd_vel_topic <- ros_topic("/cmd_vel", "geometry_msgs.Twist");
 *         cmd_vel_pub   <- ros_publisher(my_node, cmd_vel_topic);
 *         odom_sub      <- ros_subscription(my_node,
 *                              ros_topic("/odom", "nav_msgs.Odometry"));
 *     }
 *
 *     reflex step {
 *         do ros_publish(cmd_vel_pub,
 *             ["linear"::["x"::1.0,"y"::0.0,"z"::0.0],
 *              "angular"::["x"::0.0,"y"::0.0,"z"::0.5]]);
 *
 *         if ros_has_data(odom_sub) {
 *             map msg <- ros_read(odom_sub);
 *             write "Received: " + msg;
 *         }
 *     }
 * }
 * </pre>
 */
public class RosOperators {

	// ── Node creation ─────────────────────────────────────────────────────────

	@operator(value = "ros_node", can_be_const = false, category = { "ROS2" })
	@doc(value = "Creates a ROS2 node with the given name on domain 0.",
		examples = { @example(value = "ros_node my_node <- ros_node(\"gama_node\");", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSNode rosNode(final IScope scope, final String name) {
		return rosNodeWithDomain(scope, name, 0);
	}

	@operator(value = "ros_node", can_be_const = false, category = { "ROS2" })
	@doc(value = "Creates a ROS2 node with the given name on the specified domain ID.",
		examples = { @example(value = "ros_node my_node <- ros_node(\"gama_node\", 0);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSNode rosNodeWithDomain(final IScope scope, final String name, final int domainId) {
		try {
			return new GamaROSNode(name, domainId);
		} catch (Exception e) {
			throw GamaRuntimeException.create(e, scope);
		}
	}

	// ── Topic creation ────────────────────────────────────────────────────────

	@operator(value = "ros_topic", can_be_const = false, category = { "ROS2" })
	@doc(value = "Creates a ROS2 topic descriptor with the given topic name and fully-qualified "
			+ "ROS2 message class name (e.g. 'geometry_msgs.Twist').",
		examples = { @example(value = "ros_topic t <- ros_topic(\"/cmd_vel\", \"geometry_msgs.Twist\");",
			isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSTopic rosTopic(final IScope scope, final String topicName,
			final String messageClassName) {
		return new GamaROSTopic(topicName, messageClassName);
	}

	@operator(value = "ros_topic", can_be_const = false, category = { "ROS2" })
	@doc(value = "Creates a ROS2 topic descriptor with a QoS profile. "
			+ "Accepted QoS values: 'default', 'reliable', 'best_effort'.",
		examples = { @example(value = "ros_topic t <- ros_topic(\"/scan\", \"sensor_msgs.LaserScan\", \"best_effort\");",
			isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSTopic rosTopicWithQos(final IScope scope, final String topicName,
			final String messageClassName, final String qosName) {
		ROS2QoSProfile qos = switch (qosName.toLowerCase().trim()) {
			case "reliable"    -> ROS2QoSProfile.RELIABLE;
			case "best_effort" -> ROS2QoSProfile.BEST_EFFORT;
			default            -> ROS2QoSProfile.DEFAULT;
		};
		return new GamaROSTopic(topicName, messageClassName, qos);
	}

	// ── Publisher ─────────────────────────────────────────────────────────────

	@operator(value = "ros_publisher", can_be_const = false, category = { "ROS2" })
	@doc(value = "Creates a ROS2 publisher on the given node for the given topic.",
		examples = { @example(value = "ros_publisher pub <- ros_publisher(my_node, my_topic);",
			isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSPublisher rosPublisher(final IScope scope,
			final GamaROSNode node, final GamaROSTopic topic) {
		try {
			return new GamaROSPublisher(node, topic);
		} catch (Exception e) {
			throw GamaRuntimeException.create(e, scope);
		}
	}

	// ── Subscription ──────────────────────────────────────────────────────────
//
//	@operator(value = "ros_subscription", can_be_const = false, category = { "ROS2" })
//	@doc(value = "Creates a ROS2 subscription on the given node for the given topic. "
//			+ "Incoming messages are buffered and can be read with ros_read or ros_read_latest.",
//		examples = { @example(value = "ros_subscription sub <- ros_subscription(my_node, my_topic);",
//			isExecutable = false) })
//	@no_test(Reason.IMPOSSIBLE_TO_TEST)
//	public static GamaROSSubscription rosSubscription(final IScope scope,
//			final GamaROSNode node, final GamaROSTopic topic) {
//		try {
//			return GamaROSSubscription.create(node, topic);
//		} catch (Exception e) {
//			throw GamaRuntimeException.create(e, scope);
//		}
//	}

	// ── Publish ───────────────────────────────────────────────────────────────

//	@operator(value = "ros_publish", can_be_const = false, category = { "ROS2" })
//	@doc(value = "Publishes a message on the given publisher. "
//			+ "The message is a map whose structure mirrors the ROS2 message type. "
//			+ "Returns the publisher (for chaining).",
//		examples = { @example(
//			value = "ros_publish(cmd_vel_pub, [\"linear\"::[\"x\"::1.0,\"y\"::0.0,\"z\"::0.0],"
//				+ "\"angular\"::[\"x\"::0.0,\"y\"::0.0,\"z\"::0.5]]);",
//			isExecutable = false) })
//	@no_test(Reason.IMPOSSIBLE_TO_TEST)
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public static GamaROSPublisher rosPublish(final IScope scope,
//			final GamaROSPublisher publisher, final Map message) {
//		if (publisher == null || publisher.isClosed()) {
//			throw GamaRuntimeException.error("ros_publish: publisher is null or closed", scope);
//		}
//		try {
//			String className = publisher.getTopic().getMessageClassName();
//			ROS2Message<?> msg = RosMessageConverter.newInstance(className);
//			RosMessageConverter.fromGamaMap(msg, (Map<String, Object>) message);
//			publisher.getPublisher().publish(msg);
//		} catch (GamaRuntimeException e) {
//			throw e;
//		} catch (Exception e) {
//			throw GamaRuntimeException.create(e, scope);
//		}
//		return publisher;
//	}

	// ── Subscription read ─────────────────────────────────────────────────────

	@operator(value = "ros_has_data", can_be_const = false, category = { "ROS2" })
	@doc(value = "Returns true if at least one message is waiting in the subscription's queue.",
		examples = { @example(value = "if ros_has_data(my_sub) { map msg <- ros_read(my_sub); }",
			isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static Boolean rosHasData(final IScope scope, final GamaROSSubscription sub) {
		if (sub == null) return false;
		return sub.hasData();
	}

	@operator(value = "ros_read", can_be_const = false, category = { "ROS2" })
	@doc(value = "Removes and returns the oldest available message from the subscription as a map. "
			+ "Returns nil if no message is available.",
		examples = { @example(value = "map msg <- ros_read(my_sub);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static Object rosRead(final IScope scope, final GamaROSSubscription sub) {
		if (sub == null) return null;
		return sub.poll();
	}

	@operator(value = "ros_read_latest", can_be_const = false, category = { "ROS2" })
	@doc(value = "Drains the subscription queue and returns the most recent message as a map, "
			+ "discarding older ones. Returns nil if no message is available.",
		examples = { @example(value = "map msg <- ros_read_latest(my_sub);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static Object rosReadLatest(final IScope scope, final GamaROSSubscription sub) {
		if (sub == null) return null;
		return sub.pollLatest();
	}

	@operator(value = "ros_queue_size", can_be_const = false, category = { "ROS2" })
	@doc(value = "Returns the number of messages currently waiting in the subscription queue.",
		examples = { @example(value = "int n <- ros_queue_size(my_sub);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static Integer rosQueueSize(final IScope scope, final GamaROSSubscription sub) {
		if (sub == null) return 0;
		return sub.queueSize();
	}

	// ── Lifecycle ─────────────────────────────────────────────────────────────

	@operator(value = "ros_close_node", can_be_const = false, category = { "ROS2" })
	@doc(value = "Closes a ROS2 node and releases all its resources.",
		examples = { @example(value = "ros_close_node(my_node);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSNode rosCloseNode(final IScope scope, final GamaROSNode node) {
		if (node != null) node.close();
		return node;
	}

	@operator(value = "ros_close_publisher", can_be_const = false, category = { "ROS2" })
	@doc(value = "Destroys a ROS2 publisher on its owning node.",
		examples = { @example(value = "ros_close_publisher(my_pub);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSPublisher rosClosePublisher(final IScope scope,
			final GamaROSPublisher publisher) {
		if (publisher != null) publisher.close();
		return publisher;
	}

	@operator(value = "ros_close_subscription", can_be_const = false, category = { "ROS2" })
	@doc(value = "Destroys a ROS2 subscription on its owning node.",
		examples = { @example(value = "ros_close_subscription(my_sub);", isExecutable = false) })
	@no_test(Reason.IMPOSSIBLE_TO_TEST)
	public static GamaROSSubscription rosCloseSubscription(final IScope scope,
			final GamaROSSubscription sub) {
		if (sub != null) sub.close();
		return sub;
	}
}
