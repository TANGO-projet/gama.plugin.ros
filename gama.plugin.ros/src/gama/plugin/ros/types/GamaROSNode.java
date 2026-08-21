package gama.plugin.ros.types;

import us.ihmc.jros2.ROS2Node;

import gama.api.gaml.types.IType;
import gama.api.runtime.scope.IScope;
import gama.api.types.misc.IValue;
import gama.api.utils.json.IJson;
import gama.api.utils.json.IJsonValue;

/**
 * Wraps a jros2 ROS2Node as a GAMA value.
 * Holds the underlying node and exposes its name/domainId for display.
 */
public class GamaROSNode implements IValue {

	private final ROS2Node node;
	private final String name;
	private final int domainId;

	public GamaROSNode(final String name, final int domainId) {
		this.name = name;
		this.domainId = domainId;
		this.node = new ROS2Node(name, domainId);
	}

	public ROS2Node getNode() {
		return node;
	}

	public String getName() {
		return name;
	}

	public int getDomainId() {
		return domainId;
	}

	/** Close the underlying ROS2 node and release resources. */
	public void close() {
		if (!node.isClosed()) {
			node.close();
		}
	}

	public boolean isClosed() {
		return node.isClosed();
	}

	// ── IValue ────────────────────────────────────────────────────────────────

	@Override
	public IType<?> getGamlType() {
		return GamaROSNodeType.INSTANCE;
	}

	@Override
	public String stringValue(final IScope scope) {
		return "ros_node(" + name + ", domain=" + domainId + ")";
	}

	@Override
	public IJsonValue serializeToJson(final IJson json) {
		return json.typedObject(getGamlType(), "name", name, "domainId", domainId);
	}

	@Override
	public IValue copy(final IScope scope) {
		// ROS2 nodes are not cloneable — return the same wrapper
		return this;
	}

	@Override
	public String toString() {
		return stringValue(null);
	}
}
