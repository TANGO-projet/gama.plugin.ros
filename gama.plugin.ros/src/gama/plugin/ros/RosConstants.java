/*******************************************************************************************************
 *
 * RosConstants.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros;

import gama.api.gaml.types.IType;

/**
 * Names, identifiers and documentation categories shared by the ROS2 plugin.
 */
public interface RosConstants {

	/** The GAML name of the node type. */
	String NODE = "ros_node";

	/** The GAML name of the topic type. */
	String TOPIC = "ros_topic";

	/** The GAML name of the publisher type. */
	String PUBLISHER = "ros_publisher";

	/** The GAML name of the subscription type. */
	String SUBSCRIPTION = "ros_subscription";

	/**
	 * The type ids. Custom type ids must not clash with those of the other plugins loaded in the same platform: the
	 * values around {@code +29}, {@code +30}, {@code +35} and {@code +546654..+546661} are taken by the image, kml,
	 * dataframe and bdi extensions of gama.core, and {@code +100} by gama.plugin.onnx.
	 */
	int NODE_ID = IType.BEGINNING_OF_CUSTOM_TYPES + 200;

	/** The type id of {@code ros_topic}. */
	int TOPIC_ID = IType.BEGINNING_OF_CUSTOM_TYPES + 201;

	/** The type id of {@code ros_publisher}. */
	int PUBLISHER_ID = IType.BEGINNING_OF_CUSTOM_TYPES + 202;

	/** The type id of {@code ros_subscription}. */
	int SUBSCRIPTION_ID = IType.BEGINNING_OF_CUSTOM_TYPES + 203;

	/** The documentation category of the operators. */
	String CATEGORY = "ROS2";

	/** The concept used to index the operators and types of this plugin. */
	String CONCEPT = "ros";

	/** The number of messages a subscription keeps before dropping the oldest ones. */
	int DEFAULT_QUEUE_SIZE = 100;

}
