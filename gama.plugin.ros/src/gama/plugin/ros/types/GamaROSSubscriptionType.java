package gama.plugin.ros.types;

import gama.annotations.type;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;

/**
 * GAMA type descriptor for {@link GamaROSSubscription}.
 * Registered as "ros_subscription" (id = 2003).
 */
@type(
	name = "ros_subscription",
	id = 2003,
	wraps = { GamaROSSubscription.class }
)
public class GamaROSSubscriptionType extends GamaType<GamaROSSubscription> {

	public static GamaROSSubscriptionType INSTANCE;

	public GamaROSSubscriptionType(final ITypesManager typesManager) {
		super(typesManager);
		INSTANCE = this;
	}

	@Override
	public GamaROSSubscription getDefault() {
		return null;
	}

	@Override
	public boolean canCastToConst() {
		return false;
	}

	@Override
	public GamaROSSubscription cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		if (obj instanceof GamaROSSubscription sub) {
			return sub;
		}
		return null;
	}
}
