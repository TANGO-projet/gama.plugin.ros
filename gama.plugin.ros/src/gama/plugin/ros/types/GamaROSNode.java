/*******************************************************************************************************
 *
 * GamaROSNode.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
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
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;
import us.ihmc.jros2.ROS2Node;

/**
 * A participant on the ROS2 network, and the owner of the publishers and subscriptions created from it.
 *
 * <p>
 * A node is a native resource: it holds a Fast DDS participant, its own receive threads and a share of the discovery
 * traffic of the domain. Nodes are therefore cached and closed by {@link gama.plugin.ros.utils.RosNodes}, which ties
 * their lifetime to the experiment, and models should not expect to create one per agent.
 * </p>
 */
public class GamaROSNode implements IValue {

	/** The jros2 node. */
	private final ROS2Node node;

	/** The name given to the node, kept for display since a closed node still needs to print. */
	private final String name;

	/** The domain the node participates in. */
	private final int domainId;

	/**
	 * Creates and starts a node.
	 *
	 * @param scope
	 *            the scope, used to report a failure to join the domain
	 * @param name
	 *            the name of the node, as it will appear in {@code ros2 node list}
	 * @param domainId
	 *            the ROS domain id, between 0 and 232
	 */
	public GamaROSNode(final IScope scope, final String name, final int domainId) {
		if (name == null || name.isBlank()) throw GamaRuntimeException.error("A ROS2 node needs a name", scope);
		if (domainId < 0 || domainId > 232) throw GamaRuntimeException
				.error("The ROS domain id must be between 0 and 232, " + domainId + " given", scope);
		this.name = name.trim();
		this.domainId = domainId;
		try {
			this.node = new ROS2Node(this.name, domainId);
		} catch (final Throwable t) {
			throw GamaRuntimeException.error("The ROS2 node '" + this.name + "' could not be created on domain "
					+ domainId + ": " + t.getMessage() + ". The native Fast DDS libraries bundled with jros2 support "
					+ "win-x64, linux-x64, linux-aarch64 and macos-aarch64.", scope);
		}
	}

	/**
	 * @return the jros2 node
	 */
	public ROS2Node getNode() { return node; }

	/**
	 * @return the name of the node
	 */
	public String getName() { return name; }

	/**
	 * @return the domain the node participates in
	 */
	public int getDomainId() { return domainId; }

	/**
	 * Closes the node and everything created from it. Calling this twice is harmless.
	 */
	public void close() {
		if (!node.isClosed()) { node.close(); }
	}

	/**
	 * @return true once the node has been closed, by {@code ros_close} or by the disposal of the experiment
	 */
	public boolean isClosed() { return node.isClosed(); }

	/**
	 * Guards the operators that need a live node.
	 *
	 * @param scope
	 *            the scope
	 */
	public void checkOpen(final IScope scope) {
		if (node.isClosed()) throw GamaRuntimeException
				.error("The ROS2 node '" + name + "' is closed and cannot be used any more", scope);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// IValue
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public IType<?> getGamlType() { return Types.get(GamaROSNode.class); }

	@Override
	public String stringValue(final IScope scope) {
		return name + " (domain " + domainId + (node.isClosed() ? ", closed)" : ")");
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "name", name, "domain", domainId, "closed", node.isClosed());
	}

	@Override
	public IValue copy(final IScope scope) {
		// a node is a native resource shared by every agent that names it: copying would mean joining the domain twice
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}

}
