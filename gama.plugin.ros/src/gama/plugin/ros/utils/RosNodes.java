/*******************************************************************************************************
 *
 * RosNodes.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gama.api.exceptions.GamaRuntimeException;
import gama.api.kernel.simulation.IExperimentAgent;
import gama.api.runtime.scope.IScope;
import gama.plugin.ros.types.GamaROSNode;

/**
 * Holds the ROS2 nodes of the running experiment and closes them when it is disposed.
 *
 * <h3>Why nodes cannot simply be created on demand</h3>
 *
 * <p>
 * A {@code ROS2Node} is a Fast DDS participant: it opens sockets, starts its own threads and takes part in the
 * discovery traffic of the domain. Creating one per agent, or one per simulation of a batch, saturates the domain and
 * leaves the previous participants advertised long after the run that created them has ended. So a node is cached
 * under its name and domain, and {@code ros_node("gama", 0)} called from a thousand agents returns the same node.
 * </p>
 *
 * <p>
 * The cache belongs to the <b>experiment agent</b>, exactly as the ONNX sessions do: the simulations of a batch share
 * their nodes, and everything is closed when the experiment is disposed. Nothing survives a run, so relaunching a
 * model never inherits a participant from the previous one.
 * </p>
 */
public final class RosNodes {

	/** The key under which the cache is stored on the experiment agent. */
	private static final String NODES = "$ros2_nodes";

	private RosNodes() {}

	/**
	 * Returns the nodes of the current experiment, creating the cache and arming its disposal on first use.
	 *
	 * @param scope
	 *            the scope
	 * @return the mutable cache, keyed by name and domain
	 */
	@SuppressWarnings ("unchecked")
	private static Map<String, GamaROSNode> nodesOf(final IScope scope) {
		final IExperimentAgent experiment = scope == null ? null : scope.getExperiment();
		if (experiment == null) throw GamaRuntimeException.error(
				"ROS2 nodes can only be created from within an experiment, since they belong to it.", scope);
		Map<String, GamaROSNode> nodes = (Map<String, GamaROSNode>) experiment.getAttribute(NODES);
		if (nodes != null) return nodes;
		// two agents of the same experiment may ask for the first node at the same time
		synchronized (experiment) {
			nodes = (Map<String, GamaROSNode>) experiment.getAttribute(NODES);
			if (nodes == null) {
				final Map<String, GamaROSNode> created = new ConcurrentHashMap<>();
				experiment.setAttribute(NODES, created);
				experiment.postDisposeAction(s -> {
					closeAll(created);
					return null;
				});
				nodes = created;
			}
		}
		return nodes;
	}

	/**
	 * Returns the node of the given name on the given domain, creating and registering it if this experiment does not
	 * have it yet.
	 *
	 * @param scope
	 *            the scope
	 * @param name
	 *            the name of the node
	 * @param domainId
	 *            the ROS domain id
	 * @return the node, never null
	 */
	public static GamaROSNode get(final IScope scope, final String name, final int domainId) {
		final Map<String, GamaROSNode> nodes = nodesOf(scope);
		final String key = domainId + "/" + name;
		final GamaROSNode cached = nodes.get(key);
		if (cached != null && !cached.isClosed()) return cached;
		final GamaROSNode node = new GamaROSNode(scope, name, domainId);
		// putIfAbsent so that two threads racing on the same name share the winner and the loser is closed
		final GamaROSNode concurrent = nodes.putIfAbsent(key, node);
		if (concurrent != null && !concurrent.isClosed()) {
			node.close();
			return concurrent;
		}
		nodes.put(key, node);
		return node;
	}

	/**
	 * Closes one node and forgets it, so that naming it again creates a fresh one.
	 *
	 * @param scope
	 *            the scope
	 * @param node
	 *            the node to close, may be null
	 * @return true if the node was open and has been closed
	 */
	public static boolean close(final IScope scope, final GamaROSNode node) {
		if (node == null || node.isClosed()) return false;
		nodesOf(scope).remove(node.getDomainId() + "/" + node.getName(), node);
		node.close();
		return true;
	}

	/**
	 * Closes every node of the current experiment.
	 *
	 * @param scope
	 *            the scope
	 * @return how many nodes were closed
	 */
	public static int closeAll(final IScope scope) {
		return closeAll(nodesOf(scope));
	}

	/**
	 * Closes and forgets the nodes of a cache.
	 *
	 * @param nodes
	 *            the cache
	 * @return how many nodes were closed
	 */
	private static int closeAll(final Map<String, GamaROSNode> nodes) {
		int closed = 0;
		for (final GamaROSNode node : nodes.values()) {
			if (node.isClosed()) { continue; }
			node.close();
			closed++;
		}
		nodes.clear();
		return closed;
	}

}
