package gama.plugin.ros.types;

import gama.annotations.type;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;

/**
 * GAMA type descriptor for {@link GamaROSPublisher}.
 * Registered as "ros_publisher" (id = 2002).
 */
@type(
	name = "ros_publisher",
	id = 2002,
	wraps = { GamaROSPublisher.class }
)
public class GamaROSPublisherType extends GamaType<GamaROSPublisher> {

	public static GamaROSPublisherType INSTANCE;

	public GamaROSPublisherType(final ITypesManager typesManager) {
		super(typesManager);
		INSTANCE = this;
	}

	@Override
	public GamaROSPublisher getDefault() {
		return null;
	}

	@Override
	public boolean canCastToConst() {
		return false;
	}

	@Override
	public GamaROSPublisher cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		if (obj instanceof GamaROSPublisher pub) {
			return pub;
		}
		return null;
	}
}
