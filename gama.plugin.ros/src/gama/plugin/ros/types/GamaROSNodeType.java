package gama.plugin.ros.types;

import gama.annotations.type;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;

/**
 * GAMA type descriptor for {@link GamaROSNode}.
 * Registered as "ros_node" (id = 2001).
 */
@type(
	name = "ros_node",
	id = 2001,
	wraps = { GamaROSNode.class }
)
public class GamaROSNodeType extends GamaType<GamaROSNode> {

	/** Singleton reference injected by the GAMA type system after registration. */
	public static GamaROSNodeType INSTANCE;

	public GamaROSNodeType(final ITypesManager typesManager) {
		super(typesManager);
		INSTANCE = this;
	}

	@Override
	public GamaROSNode getDefault() {
		return null;
	}

	@Override
	public boolean canCastToConst() {
		return false;
	}

	@Override
	public GamaROSNode cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		if (obj instanceof GamaROSNode gamaNode) {
			return gamaNode;
		}
		return null;
	}
}
