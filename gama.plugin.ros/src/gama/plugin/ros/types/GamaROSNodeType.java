/*******************************************************************************************************
 *
 * GamaROSNodeType.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.types;

import gama.annotations.doc;
import gama.annotations.type;
import gama.annotations.support.ISymbolKind;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;
import gama.plugin.ros.RosConstants;

/**
 * The GAML type {@code ros_node}, wrapping {@link GamaROSNode}.
 *
 * <p>
 * A node is a native resource, so it is never copied and never cast from anything else: the only way to get one is
 * the {@code ros_node} operator, which caches it for the experiment.
 * </p>
 */
@type (
		name = RosConstants.NODE,
		id = RosConstants.NODE_ID,
		wraps = { GamaROSNode.class },
		kind = ISymbolKind.REGULAR,
		concept = { RosConstants.CONCEPT },
		doc = { @doc ("A participant on the ROS2 network, obtained with the ros_node operator. Nodes belong to the experiment and are closed when it is disposed, so the same name always gives back the same node.") })
public class GamaROSNodeType extends GamaType<GamaROSNode> {

	/**
	 * @param typesManager
	 *            the manager the type registers itself with
	 */
	public GamaROSNodeType(final ITypesManager typesManager) {
		super(typesManager);
	}

	@Override
	public GamaROSNode getDefault() { return null; }

	@Override
	public boolean canCastToConst() { return false; }

	@Override
	public GamaROSNode cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		// nothing else converts to a ros_node: it is produced by its operator and by nothing else
		return obj instanceof final GamaROSNode value ? value : null;
	}

}
