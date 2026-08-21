package gama.plugin.ros.types;

import gama.annotations.type;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;

/**
 * GAMA type descriptor for {@link GamaROSTopic}.
 * Registered as "ros_topic" (id = 2000).
 */
@type(
	name = "ros_topic",
	id = 2000,
	wraps = { GamaROSTopic.class }
)
public class GamaROSTopicType extends GamaType<GamaROSTopic> {

	/** Singleton reference injected by the GAMA type system after registration. */
	public static GamaROSTopicType INSTANCE;

	public GamaROSTopicType(final ITypesManager typesManager) {
		super(typesManager);
		INSTANCE = this;
	}

	@Override
	public GamaROSTopic getDefault() {
		return null;
	}

	@Override
	public boolean canCastToConst() {
		return false;
	}

	@Override
	public GamaROSTopic cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		if (obj instanceof GamaROSTopic topic) {
			return topic;
		}
		return null;
	}
}
